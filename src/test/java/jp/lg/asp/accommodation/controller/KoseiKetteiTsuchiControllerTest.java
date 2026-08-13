package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import jakarta.servlet.http.HttpSession;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.KoseiKetteiTsuchiReportsDto;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.service.KoseiKetteiTsuchiReportsService;
import jp.lg.asp.accommodation.util.SessionHelper;

/**
 * 宿泊税更正・決定通知書 Controller（ACCOMMODATION_TAX-358）の単体テスト。
 *
 * 対象月リストの出し分けと、PDF・プレビュー・印刷のレスポンスを検証する。
 * このControllerは他の帳票画面と異なり、DTOではなくリクエストパラメータを受け取る。
 */
@ExtendWith(MockitoExtension.class)
class KoseiKetteiTsuchiControllerTest {

    @Mock KoseiKetteiTsuchiReportsService reportsService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks KoseiKetteiTsuchiController controller;

    private static final String SHITEI_NO = "00100001";
    private static final byte[] PDF = "%PDF-1.4 dummy".getBytes();

    private HttpSession sessionWith(String shiteiNo) {
        MockHttpSession session = new MockHttpSession();
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setShiteiNo(shiteiNo);
        SessionHelper.saveShiteiGassan(session, dto);
        return session;
    }

    // ===================================================================
    // index — 画面表示
    // ===================================================================

    @Test
    void index_指定番号があれば対象月リストを取得して表示する() {
        KoseiKetteiTsuchiReportsDto dto = new KoseiKetteiTsuchiReportsDto();
        when(reportsService.buildDtoForDisplay(SHITEI_NO)).thenReturn(dto);
        when(reportsService.findTaishoYmList(SHITEI_NO)).thenReturn(List.of("202604", "202603"));
        Model model = new ExtendedModelMap();

        String view = controller.index(sessionWith(SHITEI_NO), model);

        assertThat(view).isEqualTo("reports/koseiKetteiTsuchi");
        assertThat(model.asMap()).containsEntry("dto", dto);
        assertThat(model.asMap().get("taishoYmList")).isEqualTo(List.of("202604", "202603"));
    }

    /** 指定番号が無い場合、対象月リストは空。サービスも呼ばない。 */
    @Test
    void index_指定番号が無ければ対象月リストは空になる() {
        when(reportsService.buildDtoForDisplay("")).thenReturn(new KoseiKetteiTsuchiReportsDto());
        Model model = new ExtendedModelMap();

        String view = controller.index(new MockHttpSession(), model);

        assertThat(view).isEqualTo("reports/koseiKetteiTsuchi");
        assertThat((List<?>) model.asMap().get("taishoYmList")).isEmpty();
        verify(reportsService, never()).findTaishoYmList(any());
    }

    @Test
    void index_指定番号が無くても空文字で表示用DTOを組み立てる() {
        when(reportsService.buildDtoForDisplay("")).thenReturn(new KoseiKetteiTsuchiReportsDto());
        Model model = new ExtendedModelMap();

        controller.index(new MockHttpSession(), model);

        verify(reportsService).buildDtoForDisplay("");
    }

    // ===================================================================
    // PDF / プレビュー / 印刷
    // ===================================================================

    @Test
    void generatePdf_PDFのレスポンスを返す() {
        when(reportsService.generatePdf(SHITEI_NO, "202604", null, null, "2")).thenReturn(PDF);

        ResponseEntity<byte[]> response = controller.generatePdf(SHITEI_NO, "202604", null, null, "2");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getBody()).isEqualTo(PDF);
    }

    @Test
    void generatePdf_対象月を3つ指定してもそのままサービスに渡す() {
        when(reportsService.generatePdf(SHITEI_NO, "202604", "202605", "202606", "3")).thenReturn(PDF);

        controller.generatePdf(SHITEI_NO, "202604", "202605", "202606", "3");

        verify(reportsService).generatePdf(SHITEI_NO, "202604", "202605", "202606", "3");
    }

    /** サービスが例外を投げても画面には500を返す */
    @Test
    void generatePdf_サービスが例外を投げたら500を返す() {
        when(reportsService.generatePdf(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("PDF生成に失敗しました"));

        ResponseEntity<byte[]> response = controller.generatePdf(SHITEI_NO, "202604", null, null, "2");

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    @Test
    void preview_キャッシュを無効にするヘッダが付く() {
        when(reportsService.generatePdf(SHITEI_NO, "202604", null, null, "2")).thenReturn(PDF);

        ResponseEntity<byte[]> response = controller.preview(SHITEI_NO, "202604", null, null, "2");

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst("Cache-Control")).contains("no-cache");
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("inline");
        assertThat(response.getBody()).isEqualTo(PDF);
    }

    @Test
    void print_印刷用のヘッダが付く() {
        when(reportsService.generatePdf(SHITEI_NO, "202604", null, null, "2")).thenReturn(PDF);

        ResponseEntity<byte[]> response = controller.print(SHITEI_NO, "202604", null, null, "2");

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst("X-Print-Action")).isEqualTo("true");
        assertThat(response.getBody()).isEqualTo(PDF);
    }

    @Test
    void print_サービスが例外を投げたら500を返す() {
        when(reportsService.generatePdf(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("PDF生成に失敗しました"));

        ResponseEntity<byte[]> response = controller.print(SHITEI_NO, "202604", null, null, "2");

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }
}

package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;

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
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.dto.TokureiShiteiCancelDto;
import jp.lg.asp.accommodation.dto.TokureiShiteiDto;
import jp.lg.asp.accommodation.service.TokureiShiteiService;
import jp.lg.asp.accommodation.service.TokureiShiteiCancelReportsService;
import jp.lg.asp.accommodation.util.SessionHelper;

/**
 * 納入申告書の提出期限等の特例適用者指定取消通知書 Controller（ACCOMMODATION_TAX-353）の単体テスト。
 *
 * 画面表示時のセッション指定番号による分岐と、
 * PDF・プレビュー・印刷それぞれのレスポンスヘッダを検証する。
 */
@ExtendWith(MockitoExtension.class)
class TokureiShiteiCancelControllerTest {

    @Mock TokureiShiteiService tokureiShiteiService;
    @Mock TokureiShiteiCancelReportsService reportsService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks TokureiShiteiCancelController controller;

    private static final String SHITEI_NO = "00100001";
    private static final byte[] PDF = "%PDF-1.4 dummy".getBytes();

    /** 指定番号を保持したセッション */
    private HttpSession sessionWith(String shiteiNo) {
        MockHttpSession session = new MockHttpSession();
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setShiteiNo(shiteiNo);
        SessionHelper.saveShiteiGassan(session, dto);
        return session;
    }

    private TokureiShiteiDto info() {
        TokureiShiteiDto dto = new TokureiShiteiDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setHakkoYmd(LocalDate.of(2026, 4, 1));
        return dto;
    }

    // ===================================================================
    // index — 画面表示
    // ===================================================================

    @Test
    void index_指定番号が無ければ特別徴収義務者指定モーダルを表示する() {
        Model model = new ExtendedModelMap();

        String view = controller.index(new MockHttpSession(), model);

        assertThat(view).isEqualTo("reports/tokureiShiteiCancel");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
        verify(tokureiShiteiService, never()).getTokugimuInfo(any());
    }

    @Test
    void index_セッションに指定番号があればサービスから取得した情報を表示する() {
        when(tokureiShiteiService.getTokugimuInfo(SHITEI_NO)).thenReturn(info());
        Model model = new ExtendedModelMap();

        String view = controller.index(sessionWith(SHITEI_NO), model);

        assertThat(view).isEqualTo("reports/tokureiShiteiCancel");
        assertThat(model.asMap()).doesNotContainKey("showShiteiGassanModal");
        assertThat(model.asMap().get("dto")).isNotNull();
    }

    @Test
    void index_サービスがnullを返しても発行日が本日で画面を返す() {
        when(tokureiShiteiService.getTokugimuInfo(SHITEI_NO)).thenReturn(null);
        Model model = new ExtendedModelMap();

        String view = controller.index(sessionWith(SHITEI_NO), model);

        assertThat(view).isEqualTo("reports/tokureiShiteiCancel");
        TokureiShiteiCancelDto dto = (TokureiShiteiCancelDto) model.asMap().get("dto");
        assertThat(dto.getHakkoYmd()).isEqualTo(LocalDate.now());
    }

    // ===================================================================
    // PDF / プレビュー / 印刷
    // ===================================================================

    @Test
    void generatePdf_PDFのレスポンスを返す() {
        TokureiShiteiCancelDto dto = new TokureiShiteiCancelDto();
        when(reportsService.generateTsuchiPdf(dto)).thenReturn(PDF);

        ResponseEntity<byte[]> response = controller.generatePdf(dto);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getBody()).isEqualTo(PDF);
    }

    @Test
    void preview_キャッシュを無効にするヘッダが付く() {
        TokureiShiteiCancelDto dto = new TokureiShiteiCancelDto();
        when(reportsService.generateTsuchiPdf(dto)).thenReturn(PDF);

        ResponseEntity<byte[]> response = controller.preview(dto);

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst("Cache-Control")).contains("no-cache");
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("inline");
        assertThat(response.getBody()).isEqualTo(PDF);
    }

    @Test
    void print_印刷用のヘッダが付く() {
        TokureiShiteiCancelDto dto = new TokureiShiteiCancelDto();
        when(reportsService.generateTsuchiPdf(dto)).thenReturn(PDF);

        ResponseEntity<byte[]> response = controller.print(dto);

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst("X-Print-Action")).isEqualTo("true");
        assertThat(response.getBody()).isEqualTo(PDF);
    }
}

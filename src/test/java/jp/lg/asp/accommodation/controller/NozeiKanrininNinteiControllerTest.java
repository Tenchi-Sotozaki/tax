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
import jp.lg.asp.accommodation.dto.NozeiKanrininNinteiDto;
import jp.lg.asp.accommodation.service.NozeiKanrininNinteiService;
import jp.lg.asp.accommodation.service.NozeiKanrininNinteiReportsService;
import jp.lg.asp.accommodation.util.SessionHelper;

/**
 * 納税管理人選任免除認定(不認定)通知書 Controller（ACCOMMODATION_TAX-357）の単体テスト。
 *
 * 画面表示時のセッション指定番号による分岐と、
 * PDF・プレビュー・印刷それぞれのレスポンスヘッダを検証する。
 */
@ExtendWith(MockitoExtension.class)
class NozeiKanrininNinteiControllerTest {

    @Mock NozeiKanrininNinteiService nozeiKanrininNinteiService;
    @Mock NozeiKanrininNinteiReportsService reportsService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks NozeiKanrininNinteiController controller;

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

    private NozeiKanrininNinteiDto info() {
        NozeiKanrininNinteiDto dto = new NozeiKanrininNinteiDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setHakkoYmd(LocalDate.of(2026, 4, 1));
        return dto;
    }

    // ===================================================================
    // index — 画面表示
    // ===================================================================

    @Test
    void index_指定番号が無ければモーダルを表示し発行日が本日になる() {
        Model model = new ExtendedModelMap();

        String view = controller.index(new MockHttpSession(), model);

        assertThat(view).isEqualTo("reports/nozeiKanrininNintei");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
        NozeiKanrininNinteiDto dto = (NozeiKanrininNinteiDto) model.asMap().get("dto");
        assertThat(dto.getHakkoYmd()).isEqualTo(LocalDate.now());
        verify(nozeiKanrininNinteiService, never()).getNinteiInfo(any());
    }

    @Test
    void index_セッションに指定番号があればサービスから取得した情報を表示する() {
        when(nozeiKanrininNinteiService.getNinteiInfo(SHITEI_NO)).thenReturn(info());
        Model model = new ExtendedModelMap();

        String view = controller.index(sessionWith(SHITEI_NO), model);

        assertThat(view).isEqualTo("reports/nozeiKanrininNintei");
        NozeiKanrininNinteiDto dto = (NozeiKanrininNinteiDto) model.asMap().get("dto");
        assertThat(dto.getShiteiNo()).isEqualTo(SHITEI_NO);
        assertThat(model.asMap()).doesNotContainKey("errorMessage");
    }

    /** サービスが例外を投げても画面は返し、エラーメッセージを表示する */
    @Test
    void index_サービスが例外を投げてもエラーメッセージ付きで画面を返す() {
        when(nozeiKanrininNinteiService.getNinteiInfo(SHITEI_NO)).thenThrow(new RuntimeException("該当なし"));
        Model model = new ExtendedModelMap();

        String view = controller.index(sessionWith(SHITEI_NO), model);

        assertThat(view).isEqualTo("reports/nozeiKanrininNintei");
        assertThat(model.asMap().get("errorMessage").toString()).contains(SHITEI_NO);
        NozeiKanrininNinteiDto dto = (NozeiKanrininNinteiDto) model.asMap().get("dto");
        assertThat(dto.getHakkoYmd()).isEqualTo(LocalDate.now());
    }

    // ===================================================================
    // PDF / プレビュー / 印刷
    // ===================================================================

    @Test
    void generatePdf_PDFのレスポンスを返す() {
        NozeiKanrininNinteiDto dto = info();
        when(reportsService.generatePdf(dto)).thenReturn(PDF);

        ResponseEntity<byte[]> response = controller.generatePdf(dto);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getBody()).isEqualTo(PDF);
    }

    /** 発行日は必須。未入力なら帳票を生成せず 400 を返す */
    @Test
    void generatePdf_発行日が未入力なら400を返す() {
        ResponseEntity<byte[]> response = controller.generatePdf(new NozeiKanrininNinteiDto());

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verify(reportsService, never()).generatePdf(any());
    }

    @Test
    void preview_キャッシュを無効にするヘッダが付く() {
        NozeiKanrininNinteiDto dto = info();
        when(reportsService.generatePdf(dto)).thenReturn(PDF);

        ResponseEntity<byte[]> response = controller.preview(dto);

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst("Cache-Control")).contains("no-cache");
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("inline");
        assertThat(response.getBody()).isEqualTo(PDF);
    }

    @Test
    void preview_発行日が未入力なら400を返す() {
        ResponseEntity<byte[]> response = controller.preview(new NozeiKanrininNinteiDto());

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verify(reportsService, never()).generatePdf(any());
    }

    @Test
    void print_印刷用のヘッダが付く() {
        NozeiKanrininNinteiDto dto = info();
        when(reportsService.generatePdf(dto)).thenReturn(PDF);

        ResponseEntity<byte[]> response = controller.print(dto);

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst("X-Print-Action")).isEqualTo("true");
        assertThat(response.getBody()).isEqualTo(PDF);
    }

    @Test
    void print_発行日が未入力なら400を返す() {
        ResponseEntity<byte[]> response = controller.print(new NozeiKanrininNinteiDto());

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verify(reportsService, never()).generatePdf(any());
    }
}

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
import jp.lg.asp.accommodation.dto.TokugimuShiteiTsuchiDto;
import jp.lg.asp.accommodation.service.TokugimuShiteiTsuchiService;
import jp.lg.asp.accommodation.service.TokugimuShiteiTsuchiReportsService;
import jp.lg.asp.accommodation.util.SessionHelper;

/**
 * 特別徴収義務者指定通知 Controller（ACCOMMODATION_TAX-348）の単体テスト。
 *
 * 画面表示時のセッション指定番号による分岐と、
 * PDF・プレビュー・印刷それぞれのレスポンスヘッダを検証する。
 */
@ExtendWith(MockitoExtension.class)
class TokugimuShiteiTsuchiControllerTest {

    @Mock TokugimuShiteiTsuchiService tokugimuShiteiTsuchiService;
    @Mock TokugimuShiteiTsuchiReportsService reportsService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks TokugimuShiteiTsuchiController controller;

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

    private TokugimuShiteiTsuchiDto info() {
        TokugimuShiteiTsuchiDto dto = new TokugimuShiteiTsuchiDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setHakkoYmd(LocalDate.of(2026, 4, 1));
        return dto;
    }

    // ===================================================================
    // index — 画面表示
    // ===================================================================

    @Test
    void index_セッションに指定番号があればサービスから取得した情報を表示する() {
        when(tokugimuShiteiTsuchiService.getTokugimuInfo(SHITEI_NO)).thenReturn(info());
        Model model = new ExtendedModelMap();

        String view = controller.index(sessionWith(SHITEI_NO), model);

        assertThat(view).isEqualTo("reports/tokugimuShiteiTsuchi");
        TokugimuShiteiTsuchiDto dto = (TokugimuShiteiTsuchiDto) model.asMap().get("dto");
        assertThat(dto.getShiteiNo()).isEqualTo(SHITEI_NO);
        assertThat(dto.getHakkoYmd()).isEqualTo(LocalDate.of(2026, 4, 1));
    }

    @Test
    void index_指定番号が無ければサービスを呼ばず発行日が本日になる() {
        Model model = new ExtendedModelMap();

        String view = controller.index(new MockHttpSession(), model);

        assertThat(view).isEqualTo("reports/tokugimuShiteiTsuchi");
        verify(tokugimuShiteiTsuchiService, never()).getTokugimuInfo(any());
        TokugimuShiteiTsuchiDto dto = (TokugimuShiteiTsuchiDto) model.asMap().get("dto");
        assertThat(dto.getHakkoYmd()).isEqualTo(LocalDate.now());
    }

    @Test
    void index_サービスがnullを返しても発行日が本日で画面を返す() {
        when(tokugimuShiteiTsuchiService.getTokugimuInfo(SHITEI_NO)).thenReturn(null);
        Model model = new ExtendedModelMap();

        String view = controller.index(sessionWith(SHITEI_NO), model);

        assertThat(view).isEqualTo("reports/tokugimuShiteiTsuchi");
        TokugimuShiteiTsuchiDto dto = (TokugimuShiteiTsuchiDto) model.asMap().get("dto");
        assertThat(dto.getHakkoYmd()).isEqualTo(LocalDate.now());
    }

    // ===================================================================
    // PDF / プレビュー / 印刷
    // ===================================================================

    @Test
    void generatePdf_PDFのレスポンスを返す() {
        TokugimuShiteiTsuchiDto dto = new TokugimuShiteiTsuchiDto();
        when(reportsService.generateTsuchiPdf(dto)).thenReturn(PDF);

        ResponseEntity<byte[]> response = controller.generatePdf(dto);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getBody()).isEqualTo(PDF);
    }

    @Test
    void preview_キャッシュを無効にするヘッダが付く() {
        TokugimuShiteiTsuchiDto dto = new TokugimuShiteiTsuchiDto();
        when(reportsService.generateTsuchiPdf(dto)).thenReturn(PDF);

        ResponseEntity<byte[]> response = controller.preview(dto);

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst("Cache-Control")).contains("no-cache");
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("inline");
        assertThat(response.getBody()).isEqualTo(PDF);
    }

    @Test
    void print_印刷用のヘッダが付く() {
        TokugimuShiteiTsuchiDto dto = new TokugimuShiteiTsuchiDto();
        when(reportsService.generateTsuchiPdf(dto)).thenReturn(PDF);

        ResponseEntity<byte[]> response = controller.print(dto);

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst("X-Print-Action")).isEqualTo("true");
        assertThat(response.getBody()).isEqualTo(PDF);
    }
}

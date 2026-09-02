package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import jakarta.servlet.http.HttpSession;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.NozeiKanrininNinteiDto;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.entity.Nokan;
import jp.lg.asp.accommodation.service.NokanService;
import jp.lg.asp.accommodation.service.NozeiKanrininNinteiReportsService;
import jp.lg.asp.accommodation.service.NozeiKanrininNinteiService;
import jp.lg.asp.accommodation.util.SessionHelper;

@ExtendWith(MockitoExtension.class)
class NozeiKanrininNinteiControllerTest {

    @Mock NozeiKanrininNinteiService nozeiKanrininNinteiService;
    @Mock NozeiKanrininNinteiReportsService reportsService;
    @Mock NokanService nokanService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks NozeiKanrininNinteiController controller;

    // -----------------------------------------------------------------------
    // セッションヘルパー
    // -----------------------------------------------------------------------

    private HttpSession sessionWith(String shiteiNo) {
        HttpSession session = mock(HttpSession.class);
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setShiteiNo(shiteiNo);
        when(session.getAttribute(SessionHelper.SHITEI_GASSAN_KEY)).thenReturn(dto);
        return session;
    }

    private HttpSession emptySession() {
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(SessionHelper.SHITEI_GASSAN_KEY)).thenReturn(null);
        return session;
    }

    private HttpSession gassanOnlySession(String gassanShiteiNo) {
        HttpSession session = mock(HttpSession.class);
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setShiteiNo(null);
        dto.setGassanShiteiNo(gassanShiteiNo);
        when(session.getAttribute(SessionHelper.SHITEI_GASSAN_KEY)).thenReturn(dto);
        return session;
    }

    private NozeiKanrininNinteiDto fullDto() {
        NozeiKanrininNinteiDto dto = new NozeiKanrininNinteiDto();
        dto.setHakkoYmd(LocalDate.of(2025, 4, 1));
        dto.setNintei("認定");
        dto.setShiteiNo("001001");
        dto.setKoin(new byte[]{1, 2, 3});
        return dto;
    }

    // =======================================================================
    // No.1 index - 正常系: セッションに指定番号・納税管理人登録済み・全情報あり
    // =======================================================================

    @Test
    void index_正常系_指定番号あり納税管理人登録済み_帳票画面を返す() {
        NozeiKanrininNinteiDto info = fullDto();
        when(nokanService.findByJichitaiCdAndShiteiNo("001001")).thenReturn(Optional.of(new Nokan()));
        when(nozeiKanrininNinteiService.getNinteiInfo("001001")).thenReturn(info);
        Model model = new ExtendedModelMap();

        String view = controller.index(sessionWith("001001"), model);

        assertThat(view).isEqualTo("reports/nozeiKanrininNintei");
        assertThat(model.asMap()).containsKey("dto");
    }

    // =======================================================================
    // No.2 index - 正常系: hakkoYmdがnull → LocalDate.nowをセット
    // =======================================================================

    @Test
    void index_正常系_hakkoYmdがnull_LocalDateNowをセット() {
        NozeiKanrininNinteiDto info = new NozeiKanrininNinteiDto();
        info.setHakkoYmd(null);
        info.setNintei("認定");
        when(nokanService.findByJichitaiCdAndShiteiNo("001001")).thenReturn(Optional.of(new Nokan()));
        when(nozeiKanrininNinteiService.getNinteiInfo("001001")).thenReturn(info);
        Model model = new ExtendedModelMap();

        controller.index(sessionWith("001001"), model);

        NozeiKanrininNinteiDto dto = (NozeiKanrininNinteiDto) model.asMap().get("dto");
        assertThat(dto.getHakkoYmd()).isEqualTo(LocalDate.now());
    }

    // =======================================================================
    // No.3 index - 正常系: ninteiがnull → "認定"をセット
    // =======================================================================

    @Test
    void index_正常系_ninteiがnull_認定をセット() {
        NozeiKanrininNinteiDto info = new NozeiKanrininNinteiDto();
        info.setHakkoYmd(LocalDate.now());
        info.setNintei(null);
        when(nokanService.findByJichitaiCdAndShiteiNo("001001")).thenReturn(Optional.of(new Nokan()));
        when(nozeiKanrininNinteiService.getNinteiInfo("001001")).thenReturn(info);
        Model model = new ExtendedModelMap();

        controller.index(sessionWith("001001"), model);

        NozeiKanrininNinteiDto dto = (NozeiKanrininNinteiDto) model.asMap().get("dto");
        assertThat(dto.getNintei()).isEqualTo("認定");
    }

    // =======================================================================
    // No.4 index - 正常系: getNinteiInfoがnullを返す → デフォルト値セット
    // =======================================================================

    @Test
    void index_正常系_getNinteiInfoがnull_デフォルト値をセット() {
        when(nokanService.findByJichitaiCdAndShiteiNo("001001")).thenReturn(Optional.of(new Nokan()));
        when(nozeiKanrininNinteiService.getNinteiInfo("001001")).thenReturn(null);
        Model model = new ExtendedModelMap();

        String view = controller.index(sessionWith("001001"), model);

        assertThat(view).isEqualTo("reports/nozeiKanrininNintei");
        NozeiKanrininNinteiDto dto = (NozeiKanrininNinteiDto) model.asMap().get("dto");
        assertThat(dto.getHakkoYmd()).isEqualTo(LocalDate.now());
        assertThat(dto.getNintei()).isEqualTo("認定");
    }

    // =======================================================================
    // No.5 index - 正常系: 合算指定番号のみ → 合算指定番号で取得
    // =======================================================================

    @Test
    void index_正常系_合算指定番号のみ_合算指定番号で取得() {
        NozeiKanrininNinteiDto info = fullDto();
        info.setShiteiNo("901001");
        when(nokanService.findByJichitaiCdAndShiteiNo("901001")).thenReturn(Optional.of(new Nokan()));
        when(nozeiKanrininNinteiService.getNinteiInfo("901001")).thenReturn(info);
        Model model = new ExtendedModelMap();

        String view = controller.index(gassanOnlySession("901001"), model);

        assertThat(view).isEqualTo("reports/nozeiKanrininNintei");
        verify(nozeiKanrininNinteiService).getNinteiInfo("901001");
    }

    // =======================================================================
    // No.6 index - 異常系: セッションがnull → モーダル表示
    // =======================================================================

    @Test
    void index_異常系_セッションがnull_モーダル表示() {
        Model model = new ExtendedModelMap();

        String view = controller.index(emptySession(), model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
    }

    // =======================================================================
    // No.7 index - 異常系: 指定番号・合算指定番号ともnull → モーダル表示
    // =======================================================================

    @Test
    void index_異常系_両番号ともnull_モーダル表示() {
        HttpSession session = mock(HttpSession.class);
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setShiteiNo(null);
        dto.setGassanShiteiNo(null);
        when(session.getAttribute(SessionHelper.SHITEI_GASSAN_KEY)).thenReturn(dto);
        Model model = new ExtendedModelMap();

        String view = controller.index(session, model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
    }

    // =======================================================================
    // No.8 index - 異常系: 納税管理人が未登録 → errorMessageセット
    // =======================================================================

    @Test
    void index_異常系_納税管理人未登録_errorMessageセット() {
        when(nokanService.findByJichitaiCdAndShiteiNo("001001")).thenReturn(Optional.empty());
        Model model = new ExtendedModelMap();

        String view = controller.index(sessionWith("001001"), model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
        assertThat(model.asMap()).containsEntry("errorMessage", "納税管理人情報が登録されていません。");
    }

    // =======================================================================
    // No.9 index - 異常系: getNinteiInfoで例外 → errorMessageセットして帳票画面
    // =======================================================================

    @Test
    void index_異常系_getNinteiInfoで例外_errorMessageセット() {
        when(nokanService.findByJichitaiCdAndShiteiNo("001001")).thenReturn(Optional.of(new Nokan()));
        when(nozeiKanrininNinteiService.getNinteiInfo("001001")).thenThrow(new RuntimeException("DB error"));
        Model model = new ExtendedModelMap();

        String view = controller.index(sessionWith("001001"), model);

        assertThat(view).isEqualTo("reports/nozeiKanrininNintei");
        assertThat(model.asMap().get("errorMessage").toString()).contains("001001");
    }

    // =======================================================================
    // No.10 index - 異常系: accessCheckerで例外 → システムエラーメッセージ
    // =======================================================================

    @Test
    void index_異常系_accessCheckerで例外_システムエラーメッセージ() {
        doThrow(new RuntimeException("access error")).when(accessChecker).checkAccess(any());
        Model model = new ExtendedModelMap();

        String view = controller.index(sessionWith("001001"), model);

        assertThat(view).isEqualTo("reports/nozeiKanrininNintei");
        assertThat(model.asMap()).containsEntry("errorMessage", "システムエラーが発生しました。");
        NozeiKanrininNinteiDto dto = (NozeiKanrininNinteiDto) model.asMap().get("dto");
        assertThat(dto.getHakkoYmd()).isEqualTo(LocalDate.now());
        assertThat(dto.getNintei()).isEqualTo("認定");
    }

    // =======================================================================
    // No.11 generatePdf - 正常系: hakkoYmd・koinあり → 200+PDF
    // =======================================================================

    @Test
    void generatePdf_正常系_hakkoYmdとkoinあり_200とPDF() {
        when(reportsService.generatePdf(any())).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<byte[]> response = controller.generatePdf(fullDto());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType().toString()).contains("application/pdf");
        assertThat(response.getBody()).isEqualTo(new byte[]{1, 2, 3});
    }

    // =======================================================================
    // No.12 generatePdf - 正常系: koinがnull・shiteiNoあり → getNinteiInfoでkoin補完
    // =======================================================================

    @Test
    void generatePdf_正常系_koinがnullでshiteiNoあり_getNinteiInfoでkoin補完() {
        NozeiKanrininNinteiDto info = new NozeiKanrininNinteiDto();
        info.setKoin(new byte[]{9});
        when(nozeiKanrininNinteiService.getNinteiInfo("001001")).thenReturn(info);
        when(reportsService.generatePdf(any())).thenReturn(new byte[]{1});

        NozeiKanrininNinteiDto dto = new NozeiKanrininNinteiDto();
        dto.setHakkoYmd(LocalDate.now());
        dto.setKoin(null);
        dto.setShiteiNo("001001");

        ResponseEntity<byte[]> response = controller.generatePdf(dto);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(nozeiKanrininNinteiService).getNinteiInfo("001001");
    }

    // =======================================================================
    // No.13 generatePdf - 正常系: koinがnull・shiteiNoがnull → getNinteiInfo呼ばない
    // =======================================================================

    @Test
    void generatePdf_正常系_koinがnullでshiteiNoがnull_getNinteiInfo呼ばない() {
        when(reportsService.generatePdf(any())).thenReturn(new byte[]{1});

        NozeiKanrininNinteiDto dto = new NozeiKanrininNinteiDto();
        dto.setHakkoYmd(LocalDate.now());
        dto.setKoin(null);
        dto.setShiteiNo(null);

        ResponseEntity<byte[]> response = controller.generatePdf(dto);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(nozeiKanrininNinteiService, never()).getNinteiInfo(any());
    }

    // =======================================================================
    // No.14 generatePdf - 正常系: getNinteiInfoがnullを返す → koinはnullのままPDF生成
    // =======================================================================

    @Test
    void generatePdf_正常系_getNinteiInfoがnull_koinはnullのままPDF生成() {
        when(nozeiKanrininNinteiService.getNinteiInfo("001001")).thenReturn(null);
        when(reportsService.generatePdf(any())).thenReturn(new byte[]{1});

        NozeiKanrininNinteiDto dto = new NozeiKanrininNinteiDto();
        dto.setHakkoYmd(LocalDate.now());
        dto.setKoin(null);
        dto.setShiteiNo("001001");

        ResponseEntity<byte[]> response = controller.generatePdf(dto);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    // =======================================================================
    // No.15 generatePdf - 異常系: hakkoYmdがnull → 400
    // =======================================================================

    @Test
    void generatePdf_異常系_hakkoYmdがnull_400() {
        NozeiKanrininNinteiDto dto = new NozeiKanrininNinteiDto();
        dto.setHakkoYmd(null);

        ResponseEntity<byte[]> response = controller.generatePdf(dto);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    // =======================================================================
    // No.16 generatePdf - 異常系: generatePdfで例外 → 500
    // =======================================================================

    @Test
    void generatePdf_異常系_generatePdfで例外_500() {
        when(reportsService.generatePdf(any())).thenThrow(new RuntimeException("PDF error"));

        ResponseEntity<byte[]> response = controller.generatePdf(fullDto());

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    // =======================================================================
    // No.17 generatePdf - 異常系: accessCheckerで例外 → 500
    // =======================================================================

    @Test
    void generatePdf_異常系_accessCheckerで例外_500() {
        doThrow(new RuntimeException("access error")).when(accessChecker).checkAccess(any());

        ResponseEntity<byte[]> response = controller.generatePdf(fullDto());

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    // =======================================================================
    // No.18 preview - 正常系: hakkoYmd・koinあり → 200+inline+Cache-Controlヘッダー
    // =======================================================================

    @Test
    void preview_正常系_hakkoYmdとkoinあり_200とinlineとCacheControl() {
        when(reportsService.generatePdf(any())).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<byte[]> response = controller.preview(fullDto());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst("Content-Disposition"))
                .isEqualTo("inline; filename=nozei_kanrinin_nintei_preview.pdf");
        assertThat(response.getHeaders().getFirst("Cache-Control"))
                .isEqualTo("no-cache, no-store, must-revalidate");
    }

    // =======================================================================
    // No.19 preview - 正常系: koinがnull・shiteiNoあり → getNinteiInfoでkoin補完
    // =======================================================================

    @Test
    void preview_正常系_koinがnullでshiteiNoあり_getNinteiInfoでkoin補完() {
        NozeiKanrininNinteiDto info = new NozeiKanrininNinteiDto();
        info.setKoin(new byte[]{9});
        when(nozeiKanrininNinteiService.getNinteiInfo("001001")).thenReturn(info);
        when(reportsService.generatePdf(any())).thenReturn(new byte[]{1});

        NozeiKanrininNinteiDto dto = new NozeiKanrininNinteiDto();
        dto.setHakkoYmd(LocalDate.now());
        dto.setKoin(null);
        dto.setShiteiNo("001001");

        ResponseEntity<byte[]> response = controller.preview(dto);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(nozeiKanrininNinteiService).getNinteiInfo("001001");
    }

    // =======================================================================
    // No.20 preview - 異常系: hakkoYmdがnull → 400
    // =======================================================================

    @Test
    void preview_異常系_hakkoYmdがnull_400() {
        NozeiKanrininNinteiDto dto = new NozeiKanrininNinteiDto();
        dto.setHakkoYmd(null);

        ResponseEntity<byte[]> response = controller.preview(dto);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    // =======================================================================
    // No.21 preview - 異常系: generatePdfで例外 → 500
    // =======================================================================

    @Test
    void preview_異常系_generatePdfで例外_500() {
        when(reportsService.generatePdf(any())).thenThrow(new RuntimeException("PDF error"));

        ResponseEntity<byte[]> response = controller.preview(fullDto());

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    // =======================================================================
    // No.22 print - 正常系: hakkoYmd・koinあり → 200+inline+X-Print-Actionヘッダー
    // =======================================================================

    @Test
    void print_正常系_hakkoYmdとkoinあり_200とinlineとXPrintAction() {
        when(reportsService.generatePdf(any())).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<byte[]> response = controller.print(fullDto());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst("Content-Disposition"))
                .isEqualTo("inline; filename=nozei_kanrinin_nintei_print.pdf");
        assertThat(response.getHeaders().getFirst("X-Print-Action")).isEqualTo("true");
    }

    // =======================================================================
    // No.23 print - 正常系: koinがnull・shiteiNoあり → getNinteiInfoでkoin補完
    // =======================================================================

    @Test
    void print_正常系_koinがnullでshiteiNoあり_getNinteiInfoでkoin補完() {
        NozeiKanrininNinteiDto info = new NozeiKanrininNinteiDto();
        info.setKoin(new byte[]{9});
        when(nozeiKanrininNinteiService.getNinteiInfo("001001")).thenReturn(info);
        when(reportsService.generatePdf(any())).thenReturn(new byte[]{1});

        NozeiKanrininNinteiDto dto = new NozeiKanrininNinteiDto();
        dto.setHakkoYmd(LocalDate.now());
        dto.setKoin(null);
        dto.setShiteiNo("001001");

        ResponseEntity<byte[]> response = controller.print(dto);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(nozeiKanrininNinteiService).getNinteiInfo("001001");
    }

    // =======================================================================
    // No.24 print - 異常系: hakkoYmdがnull → 400
    // =======================================================================

    @Test
    void print_異常系_hakkoYmdがnull_400() {
        NozeiKanrininNinteiDto dto = new NozeiKanrininNinteiDto();
        dto.setHakkoYmd(null);

        ResponseEntity<byte[]> response = controller.print(dto);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    // =======================================================================
    // No.25 print - 異常系: generatePdfで例外 → 500
    // =======================================================================

    @Test
    void print_異常系_generatePdfで例外_500() {
        when(reportsService.generatePdf(any())).thenThrow(new RuntimeException("PDF error"));

        ResponseEntity<byte[]> response = controller.print(fullDto());

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }
}

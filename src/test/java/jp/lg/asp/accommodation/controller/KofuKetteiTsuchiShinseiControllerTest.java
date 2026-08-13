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
import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.KofuKetteiTsuchiShinseiDto;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.service.KofuKetteiTsuchiShinseiReportsService;
import jp.lg.asp.accommodation.service.KofuKetteiTsuchiShinseiService;
import jp.lg.asp.accommodation.service.ReportsCommonService;
import jp.lg.asp.accommodation.util.SessionHelper;

/**
 * 交付金 決定通知書・交付申請書 Controller（ACCOMMODATION_TAX-354 / 355）の単体テスト。
 *
 * 年度の既定値（4月始まり）の算出と、PDF系の入力チェックを検証する。
 */
@ExtendWith(MockitoExtension.class)
class KofuKetteiTsuchiShinseiControllerTest {

    @Mock KofuKetteiTsuchiShinseiService KofuKetteiTsuchiShinseiService;
    @Mock KofuKetteiTsuchiShinseiReportsService shinseiReportsService;
    @Mock ScreenAccessChecker accessChecker;
    @Mock ReportsCommonService reportsCommonService;

    @InjectMocks KofuKetteiTsuchiShinseiController controller;

    private static final String SHITEI_NO = "00100001";
    private static final byte[] PDF = "%PDF-1.4 dummy".getBytes();

    private HttpSession sessionWith(String shiteiNo) {
        MockHttpSession session = new MockHttpSession();
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setShiteiNo(shiteiNo);
        SessionHelper.saveShiteiGassan(session, dto);
        return session;
    }

    private KofuKetteiTsuchiShinseiDto inputDto() {
        KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setNendo("2026");
        dto.setHakkoYmd("2026年4月1日");
        return dto;
    }

    /** 会計年度は4月始まり。1〜3月は前年が年度になる。 */
    private String expectedNendo() {
        LocalDate now = LocalDate.now();
        return String.valueOf(now.getMonthValue() >= 4 ? now.getYear() : now.getYear() - 1);
    }

    // ===================================================================
    // index — 画面表示
    // ===================================================================

    @Test
    void index_年度が指定されていればその年度を表示する() {
        Model model = new ExtendedModelMap();

        String view = controller.index(sessionWith(SHITEI_NO), "2025", model);

        assertThat(view).isEqualTo("reports/kofuKetteiTsuchiShinsei");
        KofuKetteiTsuchiShinseiDto dto = (KofuKetteiTsuchiShinseiDto) model.asMap().get("dto");
        assertThat(dto.getNendo()).isEqualTo("2025");
        assertThat(dto.getShiteiNo()).isEqualTo(SHITEI_NO);
    }

    @Test
    void index_年度が未指定なら4月始まりの会計年度を既定値にする() {
        Model model = new ExtendedModelMap();

        controller.index(sessionWith(SHITEI_NO), null, model);

        KofuKetteiTsuchiShinseiDto dto = (KofuKetteiTsuchiShinseiDto) model.asMap().get("dto");
        assertThat(dto.getNendo()).isEqualTo(expectedNendo());
    }

    @Test
    void index_年度が空文字でも既定値の会計年度になる() {
        Model model = new ExtendedModelMap();

        controller.index(sessionWith(SHITEI_NO), "", model);

        KofuKetteiTsuchiShinseiDto dto = (KofuKetteiTsuchiShinseiDto) model.asMap().get("dto");
        assertThat(dto.getNendo()).isEqualTo(expectedNendo());
    }

    @Test
    void index_セッションに指定番号が無ければnullのまま表示する() {
        Model model = new ExtendedModelMap();

        controller.index(new MockHttpSession(), "2026", model);

        KofuKetteiTsuchiShinseiDto dto = (KofuKetteiTsuchiShinseiDto) model.asMap().get("dto");
        assertThat(dto.getShiteiNo()).isNull();
    }

    // ===================================================================
    // generatePdf
    // ===================================================================

    @Test
    void generatePdf_年度が未入力なら400を返す() {
        KofuKetteiTsuchiShinseiDto dto = inputDto();
        dto.setNendo(null);

        ResponseEntity<byte[]> response = controller.generatePdf(dto);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verify(shinseiReportsService, never()).generatekofuKetteiTsuchiShinseiPdf(any());
    }

    @Test
    void generatePdf_帳票データが見つからなければ400を返す() {
        KofuKetteiTsuchiShinseiDto dto = inputDto();
        when(KofuKetteiTsuchiShinseiService.getReportData(SHITEI_NO, "2026")).thenReturn(null);

        ResponseEntity<byte[]> response = controller.generatePdf(dto);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verify(shinseiReportsService, never()).generatekofuKetteiTsuchiShinseiPdf(any());
    }

    @Test
    void generatePdf_PDFのレスポンスを返す() {
        KofuKetteiTsuchiShinseiDto dto = inputDto();
        KofuKetteiTsuchiShinseiDto reportData = new KofuKetteiTsuchiShinseiDto();
        when(KofuKetteiTsuchiShinseiService.getReportData(SHITEI_NO, "2026")).thenReturn(reportData);
        when(shinseiReportsService.generatekofuKetteiTsuchiShinseiPdf(reportData)).thenReturn(PDF);

        ResponseEntity<byte[]> response = controller.generatePdf(dto);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getBody()).isEqualTo(PDF);
    }

    /** 画面で選んだ発行日・印刷対象が、取得した帳票データに引き継がれる */
    @Test
    void generatePdf_発行日と印刷対象と操作種別が帳票データに引き継がれる() {
        KofuKetteiTsuchiShinseiDto dto = inputDto();
        dto.setKetteiTsuchi(true);
        dto.setShinsei(false);
        KofuKetteiTsuchiShinseiDto reportData = new KofuKetteiTsuchiShinseiDto();
        when(KofuKetteiTsuchiShinseiService.getReportData(SHITEI_NO, "2026")).thenReturn(reportData);
        when(shinseiReportsService.generatekofuKetteiTsuchiShinseiPdf(reportData)).thenReturn(PDF);

        controller.generatePdf(dto);

        assertThat(reportData.getHakkoYmd()).isEqualTo("2026年4月1日");
        assertThat(reportData.isKetteiTsuchi()).isTrue();
        assertThat(reportData.isShinsei()).isFalse();
        assertThat(reportData.getOperation()).isEqualTo(ReportsConstants.SOUSA_PDF);
    }

    @Test
    void generatePdf_サービスが例外を投げたら500を返す() {
        KofuKetteiTsuchiShinseiDto dto = inputDto();
        when(KofuKetteiTsuchiShinseiService.getReportData(SHITEI_NO, "2026"))
                .thenThrow(new RuntimeException("DB接続エラー"));

        ResponseEntity<byte[]> response = controller.generatePdf(dto);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    // ===================================================================
    // preview / print
    // ===================================================================

    @Test
    void preview_PDFのレスポンスを返す() {
        KofuKetteiTsuchiShinseiDto dto = inputDto();
        KofuKetteiTsuchiShinseiDto reportData = new KofuKetteiTsuchiShinseiDto();
        when(KofuKetteiTsuchiShinseiService.getReportData(SHITEI_NO, "2026")).thenReturn(reportData);
        when(shinseiReportsService.generatekofuKetteiTsuchiShinseiPdf(reportData)).thenReturn(PDF);

        ResponseEntity<byte[]> response = controller.preview(dto);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(reportData.getOperation()).isEqualTo(ReportsConstants.SOUSA_PREVIEW);
    }

    @Test
    void print_PDFのレスポンスを返す() {
        KofuKetteiTsuchiShinseiDto dto = inputDto();
        KofuKetteiTsuchiShinseiDto reportData = new KofuKetteiTsuchiShinseiDto();
        when(KofuKetteiTsuchiShinseiService.getReportData(SHITEI_NO, "2026")).thenReturn(reportData);
        when(shinseiReportsService.generatekofuKetteiTsuchiShinseiPdf(reportData)).thenReturn(PDF);

        ResponseEntity<byte[]> response = controller.print(dto);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(reportData.getOperation()).isEqualTo(ReportsConstants.SOUSA_PRINT);
    }

    @Test
    void preview_年度が未入力なら400を返す() {
        KofuKetteiTsuchiShinseiDto dto = inputDto();
        dto.setNendo("");

        ResponseEntity<byte[]> response = controller.preview(dto);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }
}

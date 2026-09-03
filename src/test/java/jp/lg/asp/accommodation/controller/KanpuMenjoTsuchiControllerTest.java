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
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import jakarta.servlet.http.HttpSession;
import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.KanpuMenjoTsuchiDto;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.dto.TokugimuForm;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.service.KanpuMenjoTsuchiReportsService;
import jp.lg.asp.accommodation.service.ReportsCommonService;
import jp.lg.asp.accommodation.service.TokugimuService;
import jp.lg.asp.accommodation.util.SessionHelper;

@ExtendWith(MockitoExtension.class)
class KanpuMenjoTsuchiControllerTest {

    @Mock TokugimuService tokugimuService;
    @Mock KanpuMenjoTsuchiReportsService kanpuMenjoTsuchiReportsService;
    @Mock ReportsCommonService reportsCommonService;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks KanpuMenjoTsuchiController controller;

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

    private HttpSession bothNullSession() {
        HttpSession session = mock(HttpSession.class);
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setShiteiNo(null);
        dto.setGassanShiteiNo(null);
        when(session.getAttribute(SessionHelper.SHITEI_GASSAN_KEY)).thenReturn(dto);
        return session;
    }

    private TokugimuForm fullTokugimuForm() {
        TokugimuForm form = new TokugimuForm();
        form.setName("テスト");
        form.setTokugimuAddressNo("1234567");
        form.setTokugimuAddress("住所");
        form.setFacilityName("施設");
        form.setFacilityAddressNo("7654321");
        form.setFacilityAddress("施設所在地");
        return form;
    }

    private Jichitai jichitai(String name) {
        Jichitai j = new Jichitai();
        j.setName(name);
        return j;
    }

    // =======================================================================
    // No.1 index - 正常系: shiteiNo存在・全情報あり → 帳票画面を返す
    // =======================================================================

    @Test
    void index_正常系_shiteiNo存在_全情報あり_帳票画面を返す() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("011001");
        when(tokugimuService.getTokugimuByShiteiNo("S001")).thenReturn(fullTokugimuForm());
        when(kanpuMenjoTsuchiReportsService.findJichitai("011001")).thenReturn(jichitai("大阪市"));
        when(reportsCommonService.getReportsDefData(any())).thenReturn(new byte[]{1});
        Model model = new ExtendedModelMap();

        String view = controller.index(sessionWith("S001"), null, model);

        assertThat(view).isEqualTo("reports/kanpuMenjoTsuchi");
        KanpuMenjoTsuchiDto dto = (KanpuMenjoTsuchiDto) model.asMap().get("dto");
        assertThat(dto.getShiteiNo()).isEqualTo("S001");
        assertThat(dto.getCityName()).isEqualTo("大阪市");
        assertThat(dto.getTokuName()).isEqualTo("テスト");
        assertThat(dto.getTokuYubin()).isEqualTo("〒1234567");
        assertThat(dto.getTokuJusho()).isEqualTo("住所");
        assertThat(dto.getShisetsuName()).isEqualTo("施設");
        assertThat(dto.getShisetsuYubin()).isEqualTo("〒7654321");
        assertThat(dto.getShisetsuJusho()).isEqualTo("施設所在地");
        assertThat(dto.getHakkoYmd()).isEqualTo(LocalDate.now());
    }

    // =======================================================================
    // No.2 index - 正常系: gassanShiteiNo存在・shiteiNo=null → gassanShiteiNoで取得
    // =======================================================================

    @Test
    void index_正常系_gassanShiteiNo存在_gassanShiteiNoで取得() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("011001");
        when(tokugimuService.getTokugimuByShiteiNo("G001")).thenReturn(fullTokugimuForm());
        when(kanpuMenjoTsuchiReportsService.findJichitai(any())).thenReturn(jichitai("市"));
        when(reportsCommonService.getReportsDefData(any())).thenReturn(new byte[]{1});
        Model model = new ExtendedModelMap();

        String view = controller.index(gassanOnlySession("G001"), null, model);

        assertThat(view).isEqualTo("reports/kanpuMenjoTsuchi");
        KanpuMenjoTsuchiDto dto = (KanpuMenjoTsuchiDto) model.asMap().get("dto");
        assertThat(dto.getShiteiNo()).isEqualTo("G001");
    }

    // =======================================================================
    // No.3 index - 正常系: findJichitaiがnull → エラー画面を返す
    // =======================================================================

    @Test
    void index_正常系_findJichitaiがnull_エラー画面を返す() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("011001");
        when(kanpuMenjoTsuchiReportsService.findJichitai(any()))
                .thenThrow(new IllegalArgumentException("自治体情報が見つかりませんでした。"));
        Model model = new ExtendedModelMap();

        String view = controller.index(sessionWith("S001"), null, model);

        assertThat(view).isEqualTo("error");
        assertThat(model.asMap()).containsEntry("errorMessage", "自治体情報が見つかりませんでした。");
    }

    // =======================================================================
    // No.4 index - 異常系: selected=null → モーダル表示
    // =======================================================================

    @Test
    void index_異常系_selectedがnull_モーダル表示() {
        Model model = new ExtendedModelMap();

        String view = controller.index(emptySession(), null, model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
    }

    // =======================================================================
    // No.5 index - 異常系: tokugimuFormがnull → エラー画面
    // =======================================================================

    @Test
    void index_異常系_tokugimuFormがnull_エラー画面() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("011001");
        when(kanpuMenjoTsuchiReportsService.findJichitai(any())).thenReturn(jichitai("大阪市"));
        when(tokugimuService.getTokugimuByShiteiNo("S001")).thenReturn(null);
        Model model = new ExtendedModelMap();

        String view = controller.index(sessionWith("S001"), null, model);

        assertThat(view).isEqualTo("error");
        assertThat(model.asMap()).containsEntry("errorMessage", "指定された特別徴収義務者が見つかりません。");
    }

    // =======================================================================
    // No.6 index - 異常系: サービスがRuntimeException → エラー画面
    // =======================================================================

    @Test
    void index_異常系_サービスがRuntimeException_エラー画面() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("011001");
        when(kanpuMenjoTsuchiReportsService.findJichitai(any())).thenReturn(jichitai("大阪市"));
        when(tokugimuService.getTokugimuByShiteiNo(any())).thenThrow(new RuntimeException("DBエラー"));
        Model model = new ExtendedModelMap();

        String view = controller.index(sessionWith("S001"), null, model);

        assertThat(view).isEqualTo("error");
        assertThat(model.asMap()).containsEntry("errorMessage", "画面表示中にエラーが発生しました。");
    }

    // =======================================================================
    // No.7 index - 境界値: shiteiNo・gassanShiteiNoともnull → モーダル表示
    // =======================================================================

    @Test
    void index_境界値_両番号ともnull_モーダル表示() {
        Model model = new ExtendedModelMap();

        String view = controller.index(bothNullSession(), null, model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
    }

    // =======================================================================
    // No.8 index - 境界値: facilityAddressNoが空文字 → エラー画面
    // =======================================================================

    @Test
    void index_境界値_facilityAddressNoが空文字_エラー画面() {
        TokugimuForm form = fullTokugimuForm();
        form.setFacilityAddressNo("");
        form.setFacilityAddress("");
        when(jichitaiContext.getJichitaiCd()).thenReturn("011001");
        when(tokugimuService.getTokugimuByShiteiNo("S001")).thenReturn(form);
        when(kanpuMenjoTsuchiReportsService.findJichitai(any())).thenReturn(jichitai("大阪市"));
        when(reportsCommonService.getReportsDefData(any())).thenReturn(new byte[]{1});
        Model model = new ExtendedModelMap();

        String view = controller.index(sessionWith("S001"), null, model);

        assertThat(view).isEqualTo("error");
        assertThat(model.asMap()).containsEntry("errorMessage", "施設情報が見つかりませんでした。");
    }

    // =======================================================================
    // No.9 index - 境界値: facilityAddressNoがnull → エラー画面
    // =======================================================================

    @Test
    void index_境界値_facilityAddressNoがnull_エラー画面() {
        TokugimuForm form = fullTokugimuForm();
        form.setFacilityAddressNo(null);
        form.setFacilityAddress(null);
        when(jichitaiContext.getJichitaiCd()).thenReturn("011001");
        when(tokugimuService.getTokugimuByShiteiNo("S001")).thenReturn(form);
        when(kanpuMenjoTsuchiReportsService.findJichitai(any())).thenReturn(jichitai("大阪市"));
        when(reportsCommonService.getReportsDefData(any())).thenReturn(new byte[]{1});
        Model model = new ExtendedModelMap();

        String view = controller.index(sessionWith("S001"), null, model);

        assertThat(view).isEqualTo("error");
        assertThat(model.asMap()).containsEntry("errorMessage", "施設情報が見つかりませんでした。");
    }

    // =======================================================================
    // No.10 generatePdf - 正常系: PDF生成 → 200+attachment+ファイル名
    // =======================================================================

    @Test
    void generatePdf_正常系_PDF生成_200とattachmentとファイル名() {
        when(kanpuMenjoTsuchiReportsService.generateTsuchiPdf(any())).thenReturn(new byte[]{1, 2, 3});
        KanpuMenjoTsuchiDto dto = new KanpuMenjoTsuchiDto();
        dto.setShiteiNo("S001");
        dto.setHakkoYmd(LocalDate.of(2025, 1, 15));

        ResponseEntity<byte[]> response = controller.generatePdf(dto, null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType().toString()).contains("application/pdf");
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("attachment");
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("S001");
        assertThat(response.getBody()).isEqualTo(new byte[]{1, 2, 3});
        assertThat(response.getHeaders().getContentLength()).isEqualTo(3);
    }

    // =======================================================================
    // No.11 generatePdf - 異常系: サービスがRuntimeException → 500
    // =======================================================================

    @Test
    void generatePdf_異常系_サービスがRuntimeException_500() {
        when(kanpuMenjoTsuchiReportsService.generateTsuchiPdf(any()))
                .thenThrow(new RuntimeException("PDF生成失敗"));
        KanpuMenjoTsuchiDto dto = new KanpuMenjoTsuchiDto();
        dto.setShiteiNo("S001");

        ResponseEntity<byte[]> response = controller.generatePdf(dto, null);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNull();
    }

    // =======================================================================
    // No.12 generatePdf - 境界値: pdfDataが空バイト配列 → 200+Content-Length=0
    // =======================================================================

    @Test
    void generatePdf_境界値_pdfDataが空バイト配列_200とContentLength0() {
        when(kanpuMenjoTsuchiReportsService.generateTsuchiPdf(any())).thenReturn(new byte[]{});
        KanpuMenjoTsuchiDto dto = new KanpuMenjoTsuchiDto();
        dto.setShiteiNo("S001");

        ResponseEntity<byte[]> response = controller.generatePdf(dto, null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentLength()).isEqualTo(0);
        assertThat(response.getBody()).isEqualTo(new byte[]{});
    }

    // =======================================================================
    // No.13 preview - 正常系: PDF生成 → 200+inline
    // =======================================================================

    @Test
    void preview_正常系_PDF生成_200とinline() {
        when(kanpuMenjoTsuchiReportsService.generateTsuchiPdf(any())).thenReturn(new byte[]{1, 2, 3});
        KanpuMenjoTsuchiDto dto = new KanpuMenjoTsuchiDto();
        dto.setShiteiNo("S001");

        ResponseEntity<byte[]> response = controller.preview(dto, null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType().toString()).contains("application/pdf");
        assertThat(response.getHeaders().getFirst("Content-Disposition")).isEqualTo("inline");
        assertThat(response.getBody()).isEqualTo(new byte[]{1, 2, 3});
    }

    // =======================================================================
    // No.14 preview - 異常系: サービスがRuntimeException → 500
    // =======================================================================

    @Test
    void preview_異常系_サービスがRuntimeException_500() {
        when(kanpuMenjoTsuchiReportsService.generateTsuchiPdf(any()))
                .thenThrow(new RuntimeException("プレビュー失敗"));
        KanpuMenjoTsuchiDto dto = new KanpuMenjoTsuchiDto();
        dto.setShiteiNo("S001");

        ResponseEntity<byte[]> response = controller.preview(dto, null);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNull();
    }

    // =======================================================================
    // No.15 preview - 境界値: pdfDataが空バイト配列 → 200+Content-Length=0
    // =======================================================================

    @Test
    void preview_境界値_pdfDataが空バイト配列_200とContentLength0() {
        when(kanpuMenjoTsuchiReportsService.generateTsuchiPdf(any())).thenReturn(new byte[]{});
        KanpuMenjoTsuchiDto dto = new KanpuMenjoTsuchiDto();
        dto.setShiteiNo("S001");

        ResponseEntity<byte[]> response = controller.preview(dto, null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentLength()).isEqualTo(0);
        assertThat(response.getBody()).isEqualTo(new byte[]{});
    }

    // =======================================================================
    // No.16 print - 正常系: PDF生成 → 200+inline
    // =======================================================================

    @Test
    void print_正常系_PDF生成_200とinline() {
        when(kanpuMenjoTsuchiReportsService.generateTsuchiPdf(any())).thenReturn(new byte[]{1, 2, 3});
        KanpuMenjoTsuchiDto dto = new KanpuMenjoTsuchiDto();
        dto.setShiteiNo("S001");

        ResponseEntity<byte[]> response = controller.print(dto, null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType().toString()).contains("application/pdf");
        assertThat(response.getHeaders().getFirst("Content-Disposition")).isEqualTo("inline");
        assertThat(response.getBody()).isEqualTo(new byte[]{1, 2, 3});
    }

    // =======================================================================
    // No.17 print - 異常系: サービスがRuntimeException → 500
    // =======================================================================

    @Test
    void print_異常系_サービスがRuntimeException_500() {
        when(kanpuMenjoTsuchiReportsService.generateTsuchiPdf(any()))
                .thenThrow(new RuntimeException("印刷失敗"));
        KanpuMenjoTsuchiDto dto = new KanpuMenjoTsuchiDto();
        dto.setShiteiNo("S001");

        ResponseEntity<byte[]> response = controller.print(dto, null);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNull();
    }

    // =======================================================================
    // No.18 print - 境界値: pdfDataが空バイト配列 → 200+Content-Length=0
    // =======================================================================

    @Test
    void print_境界値_pdfDataが空バイト配列_200とContentLength0() {
        when(kanpuMenjoTsuchiReportsService.generateTsuchiPdf(any())).thenReturn(new byte[]{});
        KanpuMenjoTsuchiDto dto = new KanpuMenjoTsuchiDto();
        dto.setShiteiNo("S001");

        ResponseEntity<byte[]> response = controller.print(dto, null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentLength()).isEqualTo(0);
        assertThat(response.getBody()).isEqualTo(new byte[]{});
    }
}

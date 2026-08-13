package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import jakarta.servlet.http.HttpSession;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.NonyushoDataResponse;
import jp.lg.asp.accommodation.dto.NonyushoDto;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.dto.TokugimuForm;
import jp.lg.asp.accommodation.service.NonyushoReportsService;
import jp.lg.asp.accommodation.service.TokugimuService;
import jp.lg.asp.accommodation.util.SessionHelper;

/**
 * 納入書 Controller（ACCOMMODATION_TAX-352）の単体テスト。
 *
 * 画面表示、動的データ取得API、PDF・プレビュー・印刷を検証する。
 * PDF系は dataCheck が true（＝賦課データ無し）のとき 400 を返す点が要点。
 */
@ExtendWith(MockitoExtension.class)
class NonyushoControllerTest {

    @Mock NonyushoReportsService nonyushoReportsService;
    @Mock TokugimuService tokugimuService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks NonyushoController controller;

    private static final String SHITEI_NO = "00100001";
    private static final byte[] PDF = "%PDF-1.4 dummy".getBytes();

    private HttpSession sessionWith(String shiteiNo) {
        MockHttpSession session = new MockHttpSession();
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setShiteiNo(shiteiNo);
        SessionHelper.saveShiteiGassan(session, dto);
        return session;
    }

    private TokugimuForm tokugimuForm() {
        TokugimuForm form = new TokugimuForm();
        form.setName("株式会社ホテルA");
        form.setFacilityName("ホテルA 札幌");
        form.setTokugimuAddress("札幌市中央区北1条西1丁目");
        form.setFacilityAddress("札幌市中央区北2条西2丁目");
        return form;
    }

    private NonyushoDto dto() {
        NonyushoDto dto = new NonyushoDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setNendo("2026");
        dto.setShinkokuYmd("202603");
        return dto;
    }

    // ===================================================================
    // index — 画面表示
    // ===================================================================

    @Test
    void index_指定番号が無ければ特別徴収義務者指定モーダルを表示する() {
        Model model = new ExtendedModelMap();

        String view = controller.index(model, new MockHttpSession());

        assertThat(view).isEqualTo("reports/nonyusho");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
        verify(tokugimuService, never()).getTokugimuByShiteiNo(any());
    }

    @Test
    void index_指定番号があれば特別徴収義務者の情報を表示する() {
        when(tokugimuService.getTokugimuByShiteiNo(SHITEI_NO)).thenReturn(tokugimuForm());
        Model model = new ExtendedModelMap();

        String view = controller.index(model, sessionWith(SHITEI_NO));

        assertThat(view).isEqualTo("reports/nonyusho");
        assertThat(model.asMap()).containsEntry("shiteiNo", SHITEI_NO);
        assertThat(model.asMap()).containsEntry("tokuName", "株式会社ホテルA");
        assertThat(model.asMap()).containsEntry("shisetsuName", "ホテルA 札幌");
        assertThat(model.asMap()).doesNotContainKey("showShiteiGassanModal");
    }

    /** 特別徴収義務者の取得に失敗しても、指定番号だけ載せて画面を返す */
    @Test
    void index_特別徴収義務者の取得に失敗しても画面を返す() {
        when(tokugimuService.getTokugimuByShiteiNo(SHITEI_NO))
                .thenThrow(new RuntimeException("該当なし"));
        Model model = new ExtendedModelMap();

        String view = controller.index(model, sessionWith(SHITEI_NO));

        assertThat(view).isEqualTo("reports/nonyusho");
        assertThat(model.asMap()).containsEntry("shiteiNo", SHITEI_NO);
        assertThat(model.asMap()).doesNotContainKey("tokuName");
    }

    // ===================================================================
    // getNonyushoData — 動的データ取得API
    // ===================================================================

    @Test
    void getNonyushoData_サービスの結果を200で返す() {
        NonyushoDataResponse res = new NonyushoDataResponse();
        res.setZeigaku("50000");
        when(nonyushoReportsService.getNonyushoData(SHITEI_NO, "2026", "2026-03")).thenReturn(res);

        ResponseEntity<NonyushoDataResponse> response =
                controller.getNonyushoData(SHITEI_NO, "2026", "2026-03");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getZeigaku()).isEqualTo("50000");
    }

    @Test
    void getNonyushoData_申告年月が未指定でもサービスに渡す() {
        when(nonyushoReportsService.getNonyushoData(SHITEI_NO, "2026", null))
                .thenReturn(new NonyushoDataResponse());

        controller.getNonyushoData(SHITEI_NO, "2026", null);

        verify(nonyushoReportsService).getNonyushoData(SHITEI_NO, "2026", null);
    }

    @Test
    void getNonyushoData_サービスが例外を投げたら500を返す() {
        when(nonyushoReportsService.getNonyushoData(any(), any(), any()))
                .thenThrow(new RuntimeException("DB接続エラー"));

        ResponseEntity<NonyushoDataResponse> response =
                controller.getNonyushoData(SHITEI_NO, "2026", "2026-03");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ===================================================================
    // PDF / プレビュー / 印刷
    // ===================================================================

    @Test
    void generatePdf_PDFのレスポンスを返す() {
        NonyushoDto dto = dto();
        when(nonyushoReportsService.dataCheck(dto)).thenReturn(false);
        when(nonyushoReportsService.generateNonyushoPdf(dto)).thenReturn(PDF);

        ResponseEntity<?> response = (ResponseEntity<?>) controller.generatePdf(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getBody()).isEqualTo(PDF);
    }

    /** dataCheck が true は「賦課データ無し」を意味する */
    @Test
    void generatePdf_賦課データが無ければ400を返しPDFを生成しない() {
        NonyushoDto dto = dto();
        when(nonyushoReportsService.dataCheck(dto)).thenReturn(true);

        ResponseEntity<?> response = (ResponseEntity<?>) controller.generatePdf(dto);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verify(nonyushoReportsService, never()).generateNonyushoPdf(any());
    }

    @Test
    void generatePdf_サービスが例外を投げたら400でメッセージを返す() {
        NonyushoDto dto = dto();
        when(nonyushoReportsService.dataCheck(dto)).thenReturn(false);
        when(nonyushoReportsService.generateNonyushoPdf(dto))
                .thenThrow(new RuntimeException("賦課情報が見つかりません。"));

        ResponseEntity<?> response = (ResponseEntity<?>) controller.generatePdf(dto);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void previewPdf_インラインのPDFを返す() {
        NonyushoDto dto = dto();
        when(nonyushoReportsService.dataCheck(dto)).thenReturn(false);
        when(nonyushoReportsService.generateNonyushoPdf(dto)).thenReturn(PDF);

        ResponseEntity<?> response = (ResponseEntity<?>) controller.previewPdf(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("inline");
    }

    @Test
    void previewPdf_賦課データが無ければ400を返す() {
        NonyushoDto dto = dto();
        when(nonyushoReportsService.dataCheck(dto)).thenReturn(true);

        ResponseEntity<?> response = (ResponseEntity<?>) controller.previewPdf(dto);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verify(nonyushoReportsService, never()).generateNonyushoPdf(any());
    }

    @Test
    void printPDF_インラインのPDFを返す() {
        NonyushoDto dto = dto();
        when(nonyushoReportsService.dataCheck(dto)).thenReturn(false);
        when(nonyushoReportsService.generateNonyushoPdf(dto)).thenReturn(PDF);

        ResponseEntity<?> response = (ResponseEntity<?>) controller.printPDF(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getBody()).isEqualTo(PDF);
    }

    @Test
    void printPDF_賦課データが無ければ400を返す() {
        NonyushoDto dto = dto();
        when(nonyushoReportsService.dataCheck(dto)).thenReturn(true);

        ResponseEntity<?> response = (ResponseEntity<?>) controller.printPDF(dto);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verify(nonyushoReportsService, never()).generateNonyushoPdf(any());
    }
}

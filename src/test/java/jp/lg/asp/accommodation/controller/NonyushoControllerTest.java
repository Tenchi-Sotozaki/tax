package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NonyushoControllerTest {

    @Mock NonyushoReportsService nonyushoReportsService;
    @Mock TokugimuService tokugimuService;
    @Mock ScreenAccessChecker accessChecker;
    @Mock HttpSession session;
    @InjectMocks NonyushoController controller;

    private static final String SHITEI_NO = "S001";

    @BeforeEach
    void setUp() {
        ShiteiGassanSearchDto selected = new ShiteiGassanSearchDto();
        selected.setShiteiNo(SHITEI_NO);
        try (var mock = mockStatic(SessionHelper.class)) {
            mock.when(() -> SessionHelper.getShiteiNo(session)).thenReturn(SHITEI_NO);
            mock.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(selected);
        }
    }

    // ===== index =====

    // No.1 正常系: セッションに指定番号が存在し特別徴収義務者情報取得成功の場合、納入書画面を返す
    @Test
    void index_指定番号あり_特別徴収義務者情報取得成功_納入書画面を返す() throws Exception {
        ShiteiGassanSearchDto selected = new ShiteiGassanSearchDto();
        selected.setShiteiNo(SHITEI_NO);
        TokugimuForm tokugimuForm = new TokugimuForm();
        tokugimuForm.setFacilityName("テストホテル");
        tokugimuForm.setName("テスト太郎");

        try (var mock = mockStatic(SessionHelper.class)) {
            mock.when(() -> SessionHelper.getShiteiNo(session)).thenReturn(SHITEI_NO);
            mock.when(() -> SessionHelper.getGassanShiteiNo(session)).thenReturn(null);
            mock.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(selected);
            when(tokugimuService.getTokugimuByShiteiNo(SHITEI_NO)).thenReturn(tokugimuForm);

            Model model = new ExtendedModelMap();
            String view = controller.index(model, session);

            assertThat(view).isEqualTo("reports/nonyusho");
            assertThat(model.asMap().get("shiteiNo")).isEqualTo(SHITEI_NO);
            assertThat(model.asMap().get("shisetsuName")).isEqualTo("テストホテル");
            assertThat(model.asMap().get("tokuName")).isEqualTo("テスト太郎");
        }
    }

    // No.2 正常系: セッションに指定番号が存在し特別徴収義務者情報取得失敗の場合、納入書画面を返す（shiteiNoのみ設定）
    @Test
    void index_指定番号あり_特別徴収義務者情報取得失敗_shiteiNoのみ設定して納入書画面を返す() throws Exception {
        ShiteiGassanSearchDto selected = new ShiteiGassanSearchDto();
        selected.setShiteiNo(SHITEI_NO);

        try (var mock = mockStatic(SessionHelper.class)) {
            mock.when(() -> SessionHelper.getShiteiNo(session)).thenReturn(SHITEI_NO);
            mock.when(() -> SessionHelper.getGassanShiteiNo(session)).thenReturn(null);
            mock.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(selected);
            when(tokugimuService.getTokugimuByShiteiNo(SHITEI_NO)).thenThrow(new RuntimeException("取得失敗"));

            Model model = new ExtendedModelMap();
            String view = controller.index(model, session);

            assertThat(view).isEqualTo("reports/nonyusho");
            assertThat(model.asMap().get("shiteiNo")).isEqualTo(SHITEI_NO);
            assertThat(model.asMap()).doesNotContainKey("shisetsuName");
        }
    }

    // No.3 異常系: セッションにShiteiGassanSearchDtoが存在しない場合、tTokugimuReport画面を返す
    @Test
    void index_セッションにShiteiGassanSearchDtoなし_tTokugimuReport画面を返す() {
        try (var mock = mockStatic(SessionHelper.class)) {
            mock.when(() -> SessionHelper.getShiteiNo(session)).thenReturn(null);
            mock.when(() -> SessionHelper.getGassanShiteiNo(session)).thenReturn(null);
            mock.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(null);

            Model model = new ExtendedModelMap();
            String view = controller.index(model, session);

            assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
            assertThat(model.asMap().get("showShiteiGassanModal")).isEqualTo(true);
        }
    }

    // No.4 異常系: セッションのshiteiNoが空の場合、tTokugimuReport画面を返す
    @Test
    void index_セッションのshiteiNoが空_tTokugimuReport画面を返す() {
        ShiteiGassanSearchDto selected = new ShiteiGassanSearchDto();
        selected.setShiteiNo("");

        try (var mock = mockStatic(SessionHelper.class)) {
            // SessionHelperの実装は空文字のnullを返すため、nullを返すようにモック
            mock.when(() -> SessionHelper.getShiteiNo(session)).thenReturn(null);
            mock.when(() -> SessionHelper.getGassanShiteiNo(session)).thenReturn(null);
            mock.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(selected);

            Model model = new ExtendedModelMap();
            String view = controller.index(model, session);

            assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
            assertThat(model.asMap().get("showShiteiGassanModal")).isEqualTo(true);
        }
    }

    // ===== getNonyushoData =====

    // No.5 正常系: 正常にデータ取得できた場合、HTTP 200とNonyushoDataResponseを返す
    @Test
    void getNonyushoData_正常取得_HTTP200とNonyushoDataResponseを返す() {
        NonyushoDataResponse response = new NonyushoDataResponse();
        response.setZeigaku("10000");
        when(nonyushoReportsService.getNonyushoData(SHITEI_NO, "2024", "2024-04")).thenReturn(response);

        ResponseEntity<NonyushoDataResponse> result =
                controller.getNonyushoData(SHITEI_NO, "2024", "2024-04");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    // No.6 正常系: taishoYmがnullの場合、HTTP 200とNonyushoDataResponseを返す
    @Test
    void getNonyushoData_taishoYmがnull_HTTP200とNonyushoDataResponseを返す() {
        NonyushoDataResponse response = new NonyushoDataResponse();
        when(nonyushoReportsService.getNonyushoData(SHITEI_NO, "2024", null)).thenReturn(response);

        ResponseEntity<NonyushoDataResponse> result =
                controller.getNonyushoData(SHITEI_NO, "2024", null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    // No.7 異常系: サービスが例外をスローした場合、HTTP 500を返す
    @Test
    void getNonyushoData_サービスが例外をスロー_HTTP500を返す() {
        when(nonyushoReportsService.getNonyushoData(any(), any(), any()))
                .thenThrow(new RuntimeException("エラー"));

        ResponseEntity<NonyushoDataResponse> result =
                controller.getNonyushoData(SHITEI_NO, "2024", "2024-04");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ===== generatePdf =====

    // No.8 正常系: データが存在しPDF生成成功の場合、HTTP 200とPDFバイト列を返す
    @Test
    void generatePdf_データあり_PDF生成成功_HTTP200とPDFを返す() throws Exception {
        NonyushoDto dto = new NonyushoDto();
        dto.setShiteiNo(SHITEI_NO);
        byte[] pdf = "PDF".getBytes();
        when(nonyushoReportsService.dataCheck(dto)).thenReturn(false);
        when(nonyushoReportsService.generateNonyushoPdf(dto)).thenReturn(pdf);

        Object result = controller.generatePdf(dto);

        assertThat(result).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> response = (ResponseEntity<?>) result;
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).contains("application/pdf");
        assertThat(response.getHeaders().getContentDisposition().isAttachment()).isTrue();
    }

    // No.9 異常系: データが存在しない場合、HTTP 400とエラーメッセージを返す
    @Test
    void generatePdf_データなし_HTTP400とエラーメッセージを返す() throws Exception {
        NonyushoDto dto = new NonyushoDto();
        dto.setShiteiNo(SHITEI_NO);
        when(nonyushoReportsService.dataCheck(dto)).thenReturn(true);

        Object result = controller.generatePdf(dto);

        assertThat(result).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> response = (ResponseEntity<?>) result;
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(new String((byte[]) response.getBody(), StandardCharsets.UTF_8))
                .isEqualTo("指定された条件のデータが見つかりません。");
    }

    // No.10 異常系: サービスがRuntimeExceptionをスローした場合、HTTP 400とエラーメッセージを返す
    @Test
    void generatePdf_RuntimeExceptionをスロー_HTTP400とエラーメッセージを返す() throws Exception {
        NonyushoDto dto = new NonyushoDto();
        dto.setShiteiNo(SHITEI_NO);
        when(nonyushoReportsService.dataCheck(dto)).thenReturn(false);
        when(nonyushoReportsService.generateNonyushoPdf(dto))
                .thenThrow(new RuntimeException("賦課情報が見つかりません。"));

        Object result = controller.generatePdf(dto);

        assertThat(result).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> response = (ResponseEntity<?>) result;
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(new String((byte[]) response.getBody(), StandardCharsets.UTF_8))
                .isEqualTo("賦課情報が見つかりません。");
    }

    // No.11 異常系: サービスがRuntimeExceptionをスローした場合（PDF生成内部エラー）、HTTP 400を返す
    @Test
    void generatePdf_PDF生成内部エラー_HTTP400を返す() throws Exception {
        NonyushoDto dto = new NonyushoDto();
        dto.setShiteiNo(SHITEI_NO);
        when(nonyushoReportsService.dataCheck(dto)).thenReturn(false);
        when(nonyushoReportsService.generateNonyushoPdf(dto))
                .thenThrow(new RuntimeException("PDF生成に失敗しました: 予期せぬエラー"));

        Object result = controller.generatePdf(dto);

        assertThat(result).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> response = (ResponseEntity<?>) result;
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(new String((byte[]) response.getBody(), StandardCharsets.UTF_8))
                .isEqualTo("PDF生成に失敗しました: 予期せぬエラー");
    }

    // ===== previewPdf =====

    // No.12 正常系: データが存在しPDF生成成功の場合、HTTP 200とPDFバイト列を返す（inline）
    @Test
    void previewPdf_データあり_PDF生成成功_HTTP200とPDFをinlineで返す() throws Exception {
        NonyushoDto dto = new NonyushoDto();
        dto.setShiteiNo(SHITEI_NO);
        byte[] pdf = "PDF".getBytes();
        when(nonyushoReportsService.dataCheck(dto)).thenReturn(false);
        when(nonyushoReportsService.generateNonyushoPdf(dto)).thenReturn(pdf);

        Object result = controller.previewPdf(dto);

        assertThat(result).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> response = (ResponseEntity<?>) result;
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).contains("application/pdf");
        assertThat(response.getHeaders().getContentDisposition().isInline()).isTrue();
    }

    // No.13 異常系: データが存在しない場合、HTTP 400とエラーメッセージを返す
    @Test
    void previewPdf_データなし_HTTP400とエラーメッセージを返す() throws Exception {
        NonyushoDto dto = new NonyushoDto();
        dto.setShiteiNo(SHITEI_NO);
        when(nonyushoReportsService.dataCheck(dto)).thenReturn(true);

        Object result = controller.previewPdf(dto);

        assertThat(result).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> response = (ResponseEntity<?>) result;
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(new String((byte[]) response.getBody(), StandardCharsets.UTF_8))
                .isEqualTo("指定された条件のデータが見つかりません。");
    }

    // No.14 異常系: サービスがRuntimeExceptionをスローした場合、HTTP 400とエラーメッセージを返す
    @Test
    void previewPdf_RuntimeExceptionをスロー_HTTP400とエラーメッセージを返す() throws Exception {
        NonyushoDto dto = new NonyushoDto();
        dto.setShiteiNo(SHITEI_NO);
        when(nonyushoReportsService.dataCheck(dto)).thenReturn(false);
        when(nonyushoReportsService.generateNonyushoPdf(dto))
                .thenThrow(new RuntimeException("賦課情報が見つかりません。"));

        Object result = controller.previewPdf(dto);

        assertThat(result).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> response = (ResponseEntity<?>) result;
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(new String((byte[]) response.getBody(), StandardCharsets.UTF_8))
                .isEqualTo("賦課情報が見つかりません。");
    }

    // No.15 異常系: サービスがRuntimeExceptionをスローした場合（PDF生成内部エラー）、HTTP 400を返す
    @Test
    void previewPdf_PDF生成内部エラー_HTTP400を返す() throws Exception {
        NonyushoDto dto = new NonyushoDto();
        dto.setShiteiNo(SHITEI_NO);
        when(nonyushoReportsService.dataCheck(dto)).thenReturn(false);
        when(nonyushoReportsService.generateNonyushoPdf(dto))
                .thenThrow(new RuntimeException("PDF生成に失敗しました: 予期せぬエラー"));

        Object result = controller.previewPdf(dto);

        assertThat(result).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> response = (ResponseEntity<?>) result;
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(new String((byte[]) response.getBody(), StandardCharsets.UTF_8))
                .isEqualTo("PDF生成に失敗しました: 予期せぬエラー");
    }

    // ===== printPDF =====

    // No.16 正常系: データが存在しPDF生成成功の場合、HTTP 200とPDFバイト列を返す
    @Test
    void printPDF_データあり_PDF生成成功_HTTP200とPDFを返す() throws Exception {
        NonyushoDto dto = new NonyushoDto();
        dto.setShiteiNo(SHITEI_NO);
        byte[] pdf = "PDF".getBytes();
        when(nonyushoReportsService.dataCheck(dto)).thenReturn(false);
        when(nonyushoReportsService.generateNonyushoPdf(dto)).thenReturn(pdf);

        Object result = controller.printPDF(dto);

        assertThat(result).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> response = (ResponseEntity<?>) result;
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).contains("application/pdf");
    }

    // No.17 異常系: データが存在しない場合、HTTP 400とエラーメッセージを返す
    @Test
    void printPDF_データなし_HTTP400とエラーメッセージを返す() throws Exception {
        NonyushoDto dto = new NonyushoDto();
        dto.setShiteiNo(SHITEI_NO);
        when(nonyushoReportsService.dataCheck(dto)).thenReturn(true);

        Object result = controller.printPDF(dto);

        assertThat(result).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> response = (ResponseEntity<?>) result;
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(new String((byte[]) response.getBody(), StandardCharsets.UTF_8))
                .isEqualTo("指定された条件のデータが見つかりません。");
    }

    // No.18 異常系: サービスがRuntimeExceptionをスローした場合、HTTP 400とエラーメッセージを返す
    @Test
    void printPDF_RuntimeExceptionをスロー_HTTP400とエラーメッセージを返す() throws Exception {
        NonyushoDto dto = new NonyushoDto();
        dto.setShiteiNo(SHITEI_NO);
        when(nonyushoReportsService.dataCheck(dto)).thenReturn(false);
        when(nonyushoReportsService.generateNonyushoPdf(dto))
                .thenThrow(new RuntimeException("賦課情報が見つかりません。"));

        Object result = controller.printPDF(dto);

        assertThat(result).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> response = (ResponseEntity<?>) result;
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(new String((byte[]) response.getBody(), StandardCharsets.UTF_8))
                .isEqualTo("賦課情報が見つかりません。");
    }

    // No.19 異常系: サービスがRuntimeExceptionをスローした場合（PDF生成内部エラー）、HTTP 400を返す
    @Test
    void printPDF_PDF生成内部エラー_HTTP400を返す() throws Exception {
        NonyushoDto dto = new NonyushoDto();
        dto.setShiteiNo(SHITEI_NO);
        when(nonyushoReportsService.dataCheck(dto)).thenReturn(false);
        when(nonyushoReportsService.generateNonyushoPdf(dto))
                .thenThrow(new RuntimeException("PDF生成に失敗しました: 予期せぬエラー"));

        Object result = controller.printPDF(dto);

        assertThat(result).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> response = (ResponseEntity<?>) result;
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(new String((byte[]) response.getBody(), StandardCharsets.UTF_8))
                .isEqualTo("PDF生成に失敗しました: 予期せぬエラー");
    }
}

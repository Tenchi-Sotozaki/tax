package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.KofuKetteiTsuchiShinseiDto;
import jp.lg.asp.accommodation.dto.KofukinBulkPrintForm;
import jp.lg.asp.accommodation.service.KofuKetteiTsuchiShinseiReportsService;
import jp.lg.asp.accommodation.service.KofuKetteiTsuchiShinseiService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KofukinBulkPrintControllerTest {

	@InjectMocks
	private KofukinBulkPrintController controller;

	@Mock
	private KofuKetteiTsuchiShinseiService kofuKetteiTsuchiShinseiService;

	@Mock
	private KofuKetteiTsuchiShinseiReportsService kofuKetteiTsuchiShinseiReportsService;

	@Mock
	private ScreenAccessChecker accessChecker;

	@Nested
	@DisplayName("index メソッドのテスト")
	class IndexTest {

		@Test
		@DisplayName("正常系：初期表示処理が正常に行われ、フォームがモデルに設定されてビュー名が返却されること")
		void success() {
			Model model = new ConcurrentModel();

			String viewName = controller.index(model);

			assertThat(viewName).isEqualTo("reports/kofukinBulkPrint");
			KofukinBulkPrintForm form = (KofukinBulkPrintForm) model.getAttribute("form");
			assertThat(form).isNotNull();
			assertThat(form.getHakkoYmd()).isEqualTo(LocalDate.now().toString());
			assertThat(form.isKofuShinsei()).isTrue();
			assertThat(form.isKofuKetteiTsuchi()).isTrue();
			verify(accessChecker).checkAccess(ScreenManagement.KOFUKIN_BULK_PRINT);
		}
	}

	@Nested
	@DisplayName("pdf メソッドのテスト")
	class PdfTest {

		@Test
		@DisplayName("正常系：PDF出力処理が正常に行われ、ダウンロード用ヘッダーとPDFデータが返却されること")
		void success() {
			KofukinBulkPrintForm form = new KofukinBulkPrintForm();
			form.setHakkoYmd("2026-04-01");
			form.setNendo("2026");
			form.setKofuShinsei(true);
			form.setKofuKetteiTsuchi(true);
			Model model = new ConcurrentModel();

			List<KofuKetteiTsuchiShinseiDto> dtoList = List.of(new KofuKetteiTsuchiShinseiDto());
			byte[] dummyPdf = new byte[] { 1, 2, 3, 4 };

			when(kofuKetteiTsuchiShinseiService.getAllReportData("2026")).thenReturn(dtoList);
			when(kofuKetteiTsuchiShinseiReportsService.generateBulkPdf(dtoList)).thenReturn(dummyPdf);

			ResponseEntity<byte[]> response = controller.pdf(form, model);

			assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
			assertThat(response.getHeaders().getContentType().toString()).contains("application/pdf");
			assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("attachment; filename=kofukin_bulk.pdf");
			assertThat(response.getBody()).isEqualTo(dummyPdf);
			verify(accessChecker).checkAccess(ScreenManagement.KOFUKIN_BULK_PRINT);
		}
	}

	@Nested
	@DisplayName("preview メソッドのテスト")
	class PreviewTest {

		@Test
		@DisplayName("正常系：プレビュー処理が正常に行われ、インライン表示用ヘッダーとPDFデータが返却されること")
		void success() {
			KofukinBulkPrintForm form = new KofukinBulkPrintForm();
			form.setHakkoYmd("2026-04-01");
			form.setNendo("2026");
			form.setKofuShinsei(true);
			form.setKofuKetteiTsuchi(true);
			Model model = new ConcurrentModel();

			List<KofuKetteiTsuchiShinseiDto> dtoList = List.of(new KofuKetteiTsuchiShinseiDto());
			byte[] dummyPdf = new byte[] { 1, 2, 3, 4 };

			when(kofuKetteiTsuchiShinseiService.getAllReportData("2026")).thenReturn(dtoList);
			when(kofuKetteiTsuchiShinseiReportsService.generateBulkPdf(dtoList)).thenReturn(dummyPdf);

			ResponseEntity<byte[]> response = controller.preview(form, model);

			assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
			assertThat(response.getHeaders().getContentType().toString()).contains("application/pdf");
			assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("inline; filename=kofukin_bulk_preview.pdf");
			assertThat(response.getBody()).isEqualTo(dummyPdf);
			verify(accessChecker).checkAccess(ScreenManagement.KOFUKIN_BULK_PRINT);
		}
	}

	@Nested
	@DisplayName("print メソッドのテスト")
	class PrintTest {

		@Test
		@DisplayName("正常系：印刷処理が正常に行われ、印刷アクションヘッダー付きのPDFデータが返却されること")
		void success() {
			KofukinBulkPrintForm form = new KofukinBulkPrintForm();
			form.setHakkoYmd("2026-04-01");
			form.setNendo("2026");
			form.setKofuShinsei(true);
			form.setKofuKetteiTsuchi(true);
			Model model = new ConcurrentModel();

			List<KofuKetteiTsuchiShinseiDto> dtoList = List.of(new KofuKetteiTsuchiShinseiDto());
			byte[] dummyPdf = new byte[] { 1, 2, 3, 4 };

			when(kofuKetteiTsuchiShinseiService.getAllReportData("2026")).thenReturn(dtoList);
			when(kofuKetteiTsuchiShinseiReportsService.generateBulkPdf(dtoList)).thenReturn(dummyPdf);

			ResponseEntity<byte[]> response = controller.print(form, model);

			assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
			assertThat(response.getHeaders().getFirst("X-Print-Action")).isEqualTo("true");
			assertThat(response.getBody()).isEqualTo(dummyPdf);
			verify(accessChecker).checkAccess(ScreenManagement.KOFUKIN_BULK_PRINT);
		}
	}

	@Nested
	@DisplayName("generateResponse 共通処理の境界値・異常系テスト")
	class GenerateResponseEdgeAndErrorTest {

		@Test
		@DisplayName("境界値：交付申請と交付決定通知の双方がfalseの場合に不正リクエスト（400）が返却されること")
		void badRequest_whenBothFlagsAreFalse() {
			KofukinBulkPrintForm form = new KofukinBulkPrintForm();
			form.setKofuShinsei(false);
			form.setKofuKetteiTsuchi(false);
			Model model = new ConcurrentModel();

			ResponseEntity<byte[]> response = controller.pdf(form, model);

			assertThat(response.getStatusCode().is4xxClientError()).isTrue();
			verify(kofuKetteiTsuchiShinseiService, never()).getAllReportData(anyString());
		}

		@Test
		@DisplayName("境界値：取得したレポートデータリストがnullの場合に不正リクエスト（400）が返却されること")
		void badRequest_whenDtoListIsNull() {
			KofukinBulkPrintForm form = new KofukinBulkPrintForm();
			form.setNendo("2026");
			form.setKofuShinsei(true);
			form.setKofuKetteiTsuchi(false);
			Model model = new ConcurrentModel();

			when(kofuKetteiTsuchiShinseiService.getAllReportData("2026")).thenReturn(null);

			ResponseEntity<byte[]> response = controller.pdf(form, model);

			assertThat(response.getStatusCode().is4xxClientError()).isTrue();
		}

		@Test
		@DisplayName("境界値：取得したレポートデータリストが空の場合に不正リクエスト（400）が返却されること")
		void badRequest_whenDtoListIsEmpty() {
			KofukinBulkPrintForm form = new KofukinBulkPrintForm();
			form.setNendo("2026");
			form.setKofuShinsei(true);
			form.setKofuKetteiTsuchi(false);
			Model model = new ConcurrentModel();

			when(kofuKetteiTsuchiShinseiService.getAllReportData("2026")).thenReturn(Collections.emptyList());

			ResponseEntity<byte[]> response = controller.pdf(form, model);

			assertThat(response.getStatusCode().is4xxClientError()).isTrue();
		}

		@Test
		@DisplayName("境界値：発行年月日が不正なフォーマットやnull・空の場合にそのまま保持されてPDF生成が行われること")
		void success_withInvalidOrNullHakkoYmd() {
			KofukinBulkPrintForm form = new KofukinBulkPrintForm();
			form.setHakkoYmd("invalid-date"); // パースエラーを起こす文字列
			form.setNendo("2026");
			form.setKofuShinsei(true);
			form.setKofuKetteiTsuchi(true);
			Model model = new ConcurrentModel();

			List<KofuKetteiTsuchiShinseiDto> dtoList = List.of(new KofuKetteiTsuchiShinseiDto());
			byte[] dummyPdf = new byte[] { 1, 2 };

			when(kofuKetteiTsuchiShinseiService.getAllReportData("2026")).thenReturn(dtoList);
			when(kofuKetteiTsuchiShinseiReportsService.generateBulkPdf(dtoList)).thenReturn(dummyPdf);

			ResponseEntity<byte[]> response = controller.pdf(form, model);

			assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
			assertThat(dtoList.get(0).getHakkoYmd()).isEqualTo("invalid-date");
		}

		@Test
		@DisplayName("異常系：サービス処理中に例外が発生した場合に内部サーバーエラー（500）が返却されること")
		void internalServerError_whenExceptionThrown() {
			KofukinBulkPrintForm form = new KofukinBulkPrintForm();
			form.setNendo("2026");
			form.setKofuShinsei(true);
			form.setKofuKetteiTsuchi(true);
			Model model = new ConcurrentModel();

			when(kofuKetteiTsuchiShinseiService.getAllReportData("2026"))
					.thenThrow(new RuntimeException("Database error"));

			ResponseEntity<byte[]> response = controller.pdf(form, model);

			assertThat(response.getStatusCode().is5xxServerError()).isTrue();
		}
	}
}
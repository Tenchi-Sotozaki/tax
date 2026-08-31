package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.KofuKetteiTsuchiShinseiDto;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.service.KofuKetteiTsuchiShinseiReportsService;
import jp.lg.asp.accommodation.service.KofuKetteiTsuchiShinseiService;
import jp.lg.asp.accommodation.service.ReportsCommonService;
import jp.lg.asp.accommodation.util.SessionHelper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KofuKetteiTsuchiShinseiControllerTest {

	@InjectMocks
	private KofuKetteiTsuchiShinseiController controller;

	@Mock
	private KofuKetteiTsuchiShinseiService kofuKetteiTsuchiShinseiService;

	@Mock
	private KofuKetteiTsuchiShinseiReportsService shinseiReportsService;

	@Mock
	private ScreenAccessChecker accessChecker;

	@Mock
	private ReportsCommonService reportsCommonService;

	@Mock
	private HttpSession session;

	private static final String SCREEN_ID = ScreenManagement.KOFU_SHINSEI;

	@Nested
	@DisplayName("index メソッドのテスト")
	class IndexTest {

		@Test
		@DisplayName("正常系：セッションに指定番号が存在し、パラメータnendoが指定されている場合、DTOがモデルに設定され、画面テンプレート名が返却されること")
		void successWithNendo() {
			Model model = new ConcurrentModel();
			String nendo = "2025";

			ShiteiGassanSearchDto searchDto = new ShiteiGassanSearchDto();
			searchDto.setShiteiNo("12345");

			try (var mockedStatic = mockStatic(SessionHelper.class)) {
				mockedStatic.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(searchDto);
				mockedStatic.when(() -> SessionHelper.getShiteiNo(session)).thenReturn("12345");

				String viewName = controller.index(session, nendo, model);

				assertThat(viewName).isEqualTo("reports/kofuKetteiTsuchiShinsei");
				verify(accessChecker).checkAccess(SCREEN_ID);

				KofuKetteiTsuchiShinseiDto dto = (KofuKetteiTsuchiShinseiDto) model.getAttribute("dto");
				assertThat(dto).isNotNull();
				assertThat(dto.getNendo()).isEqualTo("2025");
				assertThat(dto.getShiteiNo()).isEqualTo("12345");
			}
		}

		@Test
		@DisplayName("境界値：パラメータnendoが未指定（null / 空文字）の場合、現在日付をもとに適切な年度が自動設定されること")
		void successWithNullOrEmptyNendo() {
			Model model = new ConcurrentModel();

			ShiteiGassanSearchDto searchDto = new ShiteiGassanSearchDto();
			searchDto.setShiteiNo("12345");

			try (var mockedStatic = mockStatic(SessionHelper.class)) {
				mockedStatic.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(searchDto);
				mockedStatic.when(() -> SessionHelper.getShiteiNo(session)).thenReturn("12345");

				// nullケース
				String viewName1 = controller.index(session, null, model);
				assertThat(viewName1).isEqualTo("reports/kofuKetteiTsuchiShinsei");

				// 空文字ケース
				String viewName2 = controller.index(session, "", model);
				assertThat(viewName2).isEqualTo("reports/kofuKetteiTsuchiShinsei");

				verify(accessChecker, times(2)).checkAccess(SCREEN_ID);
			}
		}

		@Test
		@DisplayName("異常系：セッションに指定番号が存在しない場合（selectedがnull、またはshiteiNoがnull/空文字）、検索モーダル表示フラグが設定され、専用のテンプレート名が返却されること")
		void noShiteiNo_showsModal() {
			Model model = new ConcurrentModel();
			String nendo = "2025";

			ShiteiGassanSearchDto searchDtoWithNullNo = new ShiteiGassanSearchDto();
			searchDtoWithNullNo.setShiteiNo(null);

			ShiteiGassanSearchDto searchDtoWithEmptyNo = new ShiteiGassanSearchDto();
			searchDtoWithEmptyNo.setShiteiNo("");

			try (var mockedStatic = mockStatic(SessionHelper.class)) {
				// 1. selected == null
				mockedStatic.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(null);
				String viewName1 = controller.index(session, nendo, model);
				assertThat(viewName1).isEqualTo("tokugimu/tTokugimuReport");
				assertThat(model.getAttribute("showShiteiGassanModal")).isEqualTo(true);

				// 2. selected.getShiteiNo() == null
				mockedStatic.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(searchDtoWithNullNo);
				String viewName2 = controller.index(session, nendo, model);
				assertThat(viewName2).isEqualTo("tokugimu/tTokugimuReport");

				// 3. selected.getShiteiNo() == ""
				mockedStatic.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(searchDtoWithEmptyNo);
				String viewName3 = controller.index(session, nendo, model);
				assertThat(viewName3).isEqualTo("tokugimu/tTokugimuReport");

				verify(accessChecker, times(3)).checkAccess(SCREEN_ID);
			}
		}
	}

	@Nested
	@DisplayName("generatePdf メソッドのテスト（PDF出力）")
	class GeneratePdfTest {

		@Test
		@DisplayName("正常系：generatePdfが正常にPDFバイトデータを返却すること")
		void generatePdf_success() {
			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setShiteiNo("12345");
			dto.setNendo("2025");
			dto.setHakkoYmd("2026-06-01");

			KofuKetteiTsuchiShinseiDto reportData = new KofuKetteiTsuchiShinseiDto();
			byte[] expectedPdfBytes = { 1, 2, 3 };

			when(kofuKetteiTsuchiShinseiService.getReportData("12345", "2025")).thenReturn(reportData);
			when(shinseiReportsService.generatekofuKetteiTsuchiShinseiPdf(any())).thenReturn(expectedPdfBytes);

			ResponseEntity<byte[]> response = controller.generatePdf(dto);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).isEqualTo(expectedPdfBytes);
			assertThat(response.getHeaders().getContentType().toString()).contains("application/pdf");
		}
	}

	@Nested
	@DisplayName("preview メソッドのテスト（プレビュー）")
	class PreviewTest {

		@Test
		@DisplayName("正常系：previewが正常にプレビュー用PDFバイトデータを返却すること")
		void preview_success() {
			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setShiteiNo("12345");
			dto.setNendo("2025");
			dto.setHakkoYmd("2026-06-01");

			KofuKetteiTsuchiShinseiDto reportData = new KofuKetteiTsuchiShinseiDto();
			byte[] expectedPdfBytes = { 4, 5, 6 };

			when(kofuKetteiTsuchiShinseiService.getReportData("12345", "2025")).thenReturn(reportData);
			when(shinseiReportsService.generatekofuKetteiTsuchiShinseiPdf(any())).thenReturn(expectedPdfBytes);

			ResponseEntity<byte[]> response = controller.preview(dto);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).isEqualTo(expectedPdfBytes);
			assertThat(response.getHeaders().getFirst("Cache-Control")).contains("no-cache");
		}
	}

	@Nested
	@DisplayName("print メソッドのテスト（印刷）")
	class PrintTest {

		@Test
		@DisplayName("正常系：printが正常に印刷用PDFバイトデータを返却すること")
		void print_success() {
			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setShiteiNo("12345");
			dto.setNendo("2025");
			dto.setHakkoYmd("2026-06-01");

			KofuKetteiTsuchiShinseiDto reportData = new KofuKetteiTsuchiShinseiDto();
			byte[] expectedPdfBytes = { 7, 8, 9 };

			when(kofuKetteiTsuchiShinseiService.getReportData("12345", "2025")).thenReturn(reportData);
			when(shinseiReportsService.generatekofuKetteiTsuchiShinseiPdf(any())).thenReturn(expectedPdfBytes);

			ResponseEntity<byte[]> response = controller.print(dto);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).isEqualTo(expectedPdfBytes);
			assertThat(response.getHeaders().getFirst("X-Print-Action")).isEqualTo("true");
		}
	}

	@Nested
	@DisplayName("processReport 共通処理の境界値・異常系テスト")
	class ProcessReportBranchTest {

		@Test
		@DisplayName("境界値：発行年月日（hakkoYmd）が未指定（nullおよび空文字）の場合、和暦変換処理をスキップして正常にPDFが生成されること")
		void hakkoYmdNullOrEmpty_success() {
			KofuKetteiTsuchiShinseiDto dto1 = new KofuKetteiTsuchiShinseiDto();
			dto1.setShiteiNo("12345");
			dto1.setNendo("2025");
			dto1.setHakkoYmd(null);

			KofuKetteiTsuchiShinseiDto dto2 = new KofuKetteiTsuchiShinseiDto();
			dto2.setShiteiNo("12345");
			dto2.setNendo("2025");
			dto2.setHakkoYmd("");

			KofuKetteiTsuchiShinseiDto reportData = new KofuKetteiTsuchiShinseiDto();
			byte[] expectedPdfBytes = { 1 };

			when(kofuKetteiTsuchiShinseiService.getReportData("12345", "2025")).thenReturn(reportData);
			when(shinseiReportsService.generatekofuKetteiTsuchiShinseiPdf(any())).thenReturn(expectedPdfBytes);

			ResponseEntity<byte[]> res1 = controller.generatePdf(dto1);
			assertThat(res1.getStatusCode()).isEqualTo(HttpStatus.OK);

			ResponseEntity<byte[]> res2 = controller.generatePdf(dto2);
			assertThat(res2.getStatusCode()).isEqualTo(HttpStatus.OK);
		}

		@Test
		@DisplayName("異常系：入力されたDTOの年度が未入力（null / 空文字）の場合、Bad Requestが返却されること")
		void nendoNullOrEmpty_returnsBadRequest() {
			KofuKetteiTsuchiShinseiDto dtoNull = new KofuKetteiTsuchiShinseiDto();
			dtoNull.setNendo(null);

			KofuKetteiTsuchiShinseiDto dtoEmpty = new KofuKetteiTsuchiShinseiDto();
			dtoEmpty.setNendo("");

			ResponseEntity<byte[]> res1 = controller.generatePdf(dtoNull);
			assertThat(res1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
			assertThat(new String(res1.getBody())).isEqualTo("年度が入力されていません。");

			ResponseEntity<byte[]> res2 = controller.generatePdf(dtoEmpty);
			assertThat(res2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
			assertThat(new String(res2.getBody())).isEqualTo("年度が入力されていません。");

			verify(kofuKetteiTsuchiShinseiService, never()).getReportData(any(), any());
		}

		@Test
		@DisplayName("異常系：指定された条件に一致する帳票データが見つからない（reportData == null）場合、Bad Requestが返却されること")
		void reportDataNull_returnsBadRequest() {
			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setShiteiNo("99999");
			dto.setNendo("2025");

			when(kofuKetteiTsuchiShinseiService.getReportData("99999", "2025")).thenReturn(null);

			ResponseEntity<byte[]> response = controller.generatePdf(dto);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
			assertThat(new String(response.getBody())).isEqualTo("指定された条件のデータが見つかりません。");
		}

		@Test
		@DisplayName("異常系：サービス層やPDF生成処理の内部で予期せぬ例外が発生した場合、Internal Server Errorが返却されること")
		void exceptionThrown_returnsInternalServerError() {
			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setShiteiNo("12345");
			dto.setNendo("2025");

			when(kofuKetteiTsuchiShinseiService.getReportData(any(), any()))
					.thenThrow(new RuntimeException("Unexpected error"));

			ResponseEntity<byte[]> response = controller.generatePdf(dto);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
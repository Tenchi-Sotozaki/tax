package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.constant.ReportsConstants;
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

	@BeforeEach
	void setUp() {
		when(reportsCommonService.getReportsDefText(any())).thenReturn("テスト定義");
	}

	@Nested
	@DisplayName("index メソッドのテスト")
	class IndexTest {

		@Test
		@DisplayName("正常系：セッションに指定番号が存在し、パラメータ nendo が指定されている場合")
		void successWithNendo() {
			Model model = new ConcurrentModel();
			String nendo = "2025";

			ShiteiGassanSearchDto searchDto = new ShiteiGassanSearchDto();
			searchDto.setShiteiNo("12345");

			try (var mockedStatic = mockStatic(SessionHelper.class)) {
				mockedStatic.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(searchDto);
				mockedStatic.when(() -> SessionHelper.getShiteiNo(session)).thenReturn("12345");
				mockedStatic.when(() -> SessionHelper.getGassanShiteiNo(session)).thenReturn(null);

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
		@DisplayName("正常系：指定番号（shiteiNo）が null で合算指定番号（gassanShiteiNo）が存在する場合")
		void successWithGassanShiteiNo() {
			Model model = new ConcurrentModel();
			String nendo = "2025";

			ShiteiGassanSearchDto searchDto = new ShiteiGassanSearchDto();
			
			try (var mockedStatic = mockStatic(SessionHelper.class)) {
				mockedStatic.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(searchDto);
				mockedStatic.when(() -> SessionHelper.getShiteiNo(session)).thenReturn(null);
				mockedStatic.when(() -> SessionHelper.getGassanShiteiNo(session)).thenReturn("Gassan001");

				String viewName = controller.index(session, nendo, model);

				assertThat(viewName).isEqualTo("reports/kofuKetteiTsuchiShinsei");
				KofuKetteiTsuchiShinseiDto dto = (KofuKetteiTsuchiShinseiDto) model.getAttribute("dto");
				assertThat(dto.getShiteiNo()).isEqualTo("Gassan001");
			}
		}

		@Test
		@DisplayName("境界値：パラメータ nendo が未指定（null / 空文字）の場合、自動計算された年度が設定される")
		void successWithNullOrEmptyNendo() {
			Model model = new ConcurrentModel();

			ShiteiGassanSearchDto searchDto = new ShiteiGassanSearchDto();
			searchDto.setShiteiNo("12345");

			try (var mockedStatic = mockStatic(SessionHelper.class)) {
				mockedStatic.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(searchDto);
				mockedStatic.when(() -> SessionHelper.getShiteiNo(session)).thenReturn("12345");
				mockedStatic.when(() -> SessionHelper.getGassanShiteiNo(session)).thenReturn(null);

				String viewName1 = controller.index(session, null, model);
				assertThat(viewName1).isEqualTo("reports/kofuKetteiTsuchiShinsei");

				String viewName2 = controller.index(session, "", model);
				assertThat(viewName2).isEqualTo("reports/kofuKetteiTsuchiShinsei");

				verify(accessChecker, times(2)).checkAccess(SCREEN_ID);
			}
		}

		@Test
		@DisplayName("異常系：セッションに指定番号も合算指定番号も存在しない場合、モーダル表示フラグが設定される")
		void noShiteiNo_showsModal() {
			Model model = new ConcurrentModel();
			String nendo = "2025";

			try (var mockedStatic = mockStatic(SessionHelper.class)) {
				mockedStatic.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(null);
				mockedStatic.when(() -> SessionHelper.getShiteiNo(session)).thenReturn(null);
				mockedStatic.when(() -> SessionHelper.getGassanShiteiNo(session)).thenReturn(null);

				String viewName = controller.index(session, nendo, model);
				assertThat(viewName).isEqualTo("tokugimu/tTokugimuReport");
				assertThat(model.getAttribute("showShiteiGassanModal")).isEqualTo(true);
			}
		}
	}

	@Nested
	@DisplayName("PDF / プレビュー / 印刷 エンドポイントのテスト")
	class EndpointMethodsTest {

		@Test
		@DisplayName("generatePdf メソッドの正常系テスト（PDF出力）")
		void generatePdf_success() {
			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setShiteiNo("12345");
			dto.setNendo("2025");

			KofuKetteiTsuchiShinseiDto reportData = new KofuKetteiTsuchiShinseiDto();
			byte[] expectedPdfBytes = { 1, 2, 3 };

			when(kofuKetteiTsuchiShinseiService.getReportData("12345", "2025")).thenReturn(reportData);
			when(shinseiReportsService.generatekofuKetteiTsuchiShinseiPdf(any())).thenReturn(expectedPdfBytes);

			ResponseEntity<byte[]> response = controller.generatePdf(dto);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).isEqualTo(expectedPdfBytes);
			assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
		}

		@Test
		@DisplayName("preview メソッドの正常系テスト（プレビュー出力）")
		void preview_success() {
			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setShiteiNo("12345");
			dto.setNendo("2025");

			KofuKetteiTsuchiShinseiDto reportData = new KofuKetteiTsuchiShinseiDto();
			byte[] expectedPdfBytes = { 1, 2, 3 };

			when(kofuKetteiTsuchiShinseiService.getReportData("12345", "2025")).thenReturn(reportData);
			when(shinseiReportsService.generatekofuKetteiTsuchiShinseiPdf(any())).thenReturn(expectedPdfBytes);

			ResponseEntity<byte[]> response = controller.preview(dto);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).isEqualTo(expectedPdfBytes);
			assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
			assertThat(response.getHeaders().get("Cache-Control")).isNotNull();
		}

		@Test
		@DisplayName("print メソッドの正常系テスト（印刷出力）")
		void print_success() {
			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setShiteiNo("12345");
			dto.setNendo("2025");

			KofuKetteiTsuchiShinseiDto reportData = new KofuKetteiTsuchiShinseiDto();
			byte[] expectedPdfBytes = { 1, 2, 3 };

			when(kofuKetteiTsuchiShinseiService.getReportData("12345", "2025")).thenReturn(reportData);
			when(shinseiReportsService.generatekofuKetteiTsuchiShinseiPdf(any())).thenReturn(expectedPdfBytes);

			ResponseEntity<byte[]> response = controller.print(dto);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).isEqualTo(expectedPdfBytes);
			assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
			assertThat(response.getHeaders().get("X-Print-Action")).isNotNull();
		}
	}

	@Nested
	@DisplayName("processReport メソッドの詳細テスト（和暦変換およびエラー分岐）")
	class ProcessReportTest {

		@Test
		@DisplayName("正常系：有効なDTOと年度・発行年月日が指定されている場合、和暦変換が行われてPDFが返却される")
		void processReport_withHakkoYmd_success() {
			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setShiteiNo("12345");
			dto.setNendo("2025");
			dto.setHakkoYmd("2026-06-01"); // 西暦日付
			dto.setKetteiTsuchi(true);
			dto.setShinsei(true);

			KofuKetteiTsuchiShinseiDto reportData = new KofuKetteiTsuchiShinseiDto();
			byte[] expectedPdfBytes = { 1, 2, 3 };

			when(kofuKetteiTsuchiShinseiService.getReportData("12345", "2025")).thenReturn(reportData);
			when(shinseiReportsService.generatekofuKetteiTsuchiShinseiPdf(any())).thenReturn(expectedPdfBytes);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);

			ResponseEntity<byte[]> response = controller.processReport(dto, ReportsConstants.SOUSA_PDF, headers, "エラー");

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).isEqualTo(expectedPdfBytes);
			assertThat(reportData.getHakkoYmd()).isNotNull(); // 和暦に変換されていること
		}

		@Test
		@DisplayName("異常系：年度（nendo）が未入力（null / 空文字）の場合、Bad Request が返却される")
		void nendoNullOrEmpty_returnsBadRequest() {
			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setNendo(null);

			HttpHeaders headers = new HttpHeaders();
			ResponseEntity<byte[]> res1 = controller.processReport(dto, ReportsConstants.SOUSA_PDF, headers, "エラー");
			assertThat(res1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
			assertThat(new String(res1.getBody())).isEqualTo("年度が入力されていません。");

			dto.setNendo("");
			ResponseEntity<byte[]> res2 = controller.processReport(dto, ReportsConstants.SOUSA_PDF, headers, "エラー");
			assertThat(res2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

			verify(kofuKetteiTsuchiShinseiService, never()).getReportData(any(), any());
		}

		@Test
		@DisplayName("異常系：指定条件の帳票データが見つからない場合、Bad Request が返却される")
		void reportDataNull_returnsBadRequest() {
			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setShiteiNo("99999");
			dto.setNendo("2025");

			when(kofuKetteiTsuchiShinseiService.getReportData("99999", "2025")).thenReturn(null);

			HttpHeaders headers = new HttpHeaders();
			ResponseEntity<byte[]> response = controller.processReport(dto, ReportsConstants.SOUSA_PDF, headers, "エラー");

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
			assertThat(new String(response.getBody())).isEqualTo("指定された条件のデータが見つかりません。");
		}

		@Test
		@DisplayName("異常系：内部で例外が発生した場合、Internal Server Error が返却される")
		void exceptionThrown_returnsInternalServerError() {
			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setShiteiNo("12345");
			dto.setNendo("2025");

			when(kofuKetteiTsuchiShinseiService.getReportData(any(), any()))
					.thenThrow(new RuntimeException("Unexpected error"));

			HttpHeaders headers = new HttpHeaders();
			ResponseEntity<byte[]> response = controller.processReport(dto, ReportsConstants.SOUSA_PDF, headers, "エラー");

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
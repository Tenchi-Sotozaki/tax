package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.dto.TokureiShiteiCancelDto;
import jp.lg.asp.accommodation.dto.TokureiShiteiDto;
import jp.lg.asp.accommodation.service.ReportsCommonService;
import jp.lg.asp.accommodation.service.TokureiShiteiCancelReportsService;
import jp.lg.asp.accommodation.service.TokureiShiteiService;
import jp.lg.asp.accommodation.util.SessionHelper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TokureiShiteiCancelControllerTest {

	@InjectMocks
	private TokureiShiteiCancelController tokureiShiteiCancelController;

	@Mock
	private TokureiShiteiService tokureiShiteiService;

	@Mock
	private TokureiShiteiCancelReportsService reportsService;

	@Mock
	private ReportsCommonService reportsCommonService;

	@Mock
	private ScreenAccessChecker accessChecker;

	@Mock
	private HttpSession session;

	@Mock
	private Model model;

	private MockedStatic<SessionHelper> sessionHelperMock;

	private static final String SCREEN_ID = ScreenManagement.TOKUREI_SHITEI_CANCEL;

	@BeforeEach
	void setUp() {
		sessionHelperMock = mockStatic(SessionHelper.class);
	}

	@AfterEach
	void tearDown() {
		sessionHelperMock.close();
	}

	@Nested
	@DisplayName("index メソッドのテスト")
	class IndexTest {

		@Test
		@DisplayName("正常系：指定合算情報・指定番号が存在し、特例情報も取得できる場合に正常に初期表示画面へ遷移すること")
		void successWithTokureiInfo() {
			ShiteiGassanSearchDto selected = new ShiteiGassanSearchDto();
			selected.setShiteiNo("12345");

			TokureiShiteiDto shiteiDto = new TokureiShiteiDto();
			shiteiDto.setShiteiNo("12345");

			sessionHelperMock.when(() -> SessionHelper.getShiteiNo(session)).thenReturn("12345");
			sessionHelperMock.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(selected);
			when(tokureiShiteiService.getTokugimuInfo("12345")).thenReturn(shiteiDto);
			when(reportsCommonService.getReportsDefText(any())).thenReturn("条例テキスト");

			String viewName = tokureiShiteiCancelController.index(session, model);

			assertThat(viewName).isEqualTo("reports/tokureiShiteiCancel");
			verify(accessChecker).checkAccess(SCREEN_ID);
			verify(model).addAttribute(eq("dto"), any(TokureiShiteiCancelDto.class));
		}

		@Test
		@DisplayName("境界値：指定合算情報（selected）がnullの場合にモーダル表示用フラグが設定され検索画面に戻ること")
		void boundarySelectedIsNull() {
			sessionHelperMock.when(() -> SessionHelper.getShiteiNo(session)).thenReturn("12345");
			sessionHelperMock.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(null);

			String viewName = tokureiShiteiCancelController.index(session, model);

			assertThat(viewName).isEqualTo("tokugimu/tTokugimuReport");
			verify(accessChecker).checkAccess(SCREEN_ID);
			verify(model).addAttribute("showShiteiGassanModal", true);
		}

		@Test
		@DisplayName("境界値：指定合算情報の指定番号（shiteiNo）がnullの場合にモーダル表示用フラグが設定され検索画面に戻ること")
		void boundaryShiteiNoIsNull() {
			ShiteiGassanSearchDto selected = new ShiteiGassanSearchDto();
			selected.setShiteiNo(null);

			sessionHelperMock.when(() -> SessionHelper.getShiteiNo(session)).thenReturn("12345");
			sessionHelperMock.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(selected);

			String viewName = tokureiShiteiCancelController.index(session, model);

			assertThat(viewName).isEqualTo("tokugimu/tTokugimuReport");
			verify(model).addAttribute("showShiteiGassanModal", true);
		}

		@Test
		@DisplayName("境界値：指定合算情報の指定番号（shiteiNo）が空文字の場合にモーダル表示用フラグが設定され検索画面に戻ること")
		void boundaryShiteiNoIsEmpty() {
			ShiteiGassanSearchDto selected = new ShiteiGassanSearchDto();
			selected.setShiteiNo("");

			sessionHelperMock.when(() -> SessionHelper.getShiteiNo(session)).thenReturn("12345");
			sessionHelperMock.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(selected);

			String viewName = tokureiShiteiCancelController.index(session, model);

			assertThat(viewName).isEqualTo("tokugimu/tTokugimuReport");
			verify(model).addAttribute("showShiteiGassanModal", true);
		}

		@Test
		@DisplayName("異常系：特例情報（shiteiDto）が取得できない（null）場合にエラーがスローされること")
		void error_whenShiteiDtoIsNull() {
			ShiteiGassanSearchDto selected = new ShiteiGassanSearchDto();
			selected.setShiteiNo("12345");

			sessionHelperMock.when(() -> SessionHelper.getShiteiNo(session)).thenReturn("12345");
			sessionHelperMock.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(selected);
			when(tokureiShiteiService.getTokugimuInfo("12345")).thenReturn(null);

			assertThatThrownBy(() -> tokureiShiteiCancelController.index(session, model))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("特例情報が存在しません。");
		}

		@Test
		@DisplayName("境界値：発行年月日（hakkoYmd）が既に設定されている場合にそのまま維持されること")
		void boundaryHakkoYmdAlreadySet() {
			ShiteiGassanSearchDto selected = new ShiteiGassanSearchDto();
			selected.setShiteiNo("12345");

			TokureiShiteiDto shiteiDto = new TokureiShiteiDto();
			shiteiDto.setShiteiNo("12345");

			sessionHelperMock.when(() -> SessionHelper.getShiteiNo(session)).thenReturn("12345");
			sessionHelperMock.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(selected);
			when(tokureiShiteiService.getTokugimuInfo("12345")).thenReturn(shiteiDto);
			when(reportsCommonService.getReportsDefText(any())).thenReturn("条例テキスト");

			String viewName = tokureiShiteiCancelController.index(session, model);

			assertThat(viewName).isEqualTo("reports/tokureiShiteiCancel");
		}

		@Test
		@DisplayName("異常系：画面アクセス権限がない場合に例外がスローされること")
		void accessDeniedThrowsException() {
			doThrow(new AccessDeniedException("Access Denied"))
					.when(accessChecker).checkAccess(SCREEN_ID);

			assertThatThrownBy(() -> tokureiShiteiCancelController.index(session, model))
					.isInstanceOf(AccessDeniedException.class);
		}
	}

	@Nested
	@DisplayName("generatePdf メソッドのテスト")
	class GeneratePdfTest {

		@Test
		@DisplayName("正常系：PDFデータが正常に生成されレスポンスとして返却されること")
		void success() {
			TokureiShiteiCancelDto dto = new TokureiShiteiCancelDto();
			byte[] expectedPdf = new byte[]{1, 2, 3};

			when(reportsService.generateTsuchiPdf(dto)).thenReturn(expectedPdf);

			ResponseEntity<byte[]> response = tokureiShiteiCancelController.generatePdf(dto);

			assertThat(response.getStatusCode().value()).isEqualTo(200);
			assertThat(response.getBody()).isEqualTo(expectedPdf);
			assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/pdf");
			verify(accessChecker).checkAccess(SCREEN_ID);
		}

		@Test
		@DisplayName("異常系：アクセス権限がない場合にPDF生成処理が中断されること")
		void accessDeniedThrowsException() {
			TokureiShiteiCancelDto dto = new TokureiShiteiCancelDto();
			doThrow(new AccessDeniedException("Access Denied"))
					.when(accessChecker).checkAccess(SCREEN_ID);

			assertThatThrownBy(() -> tokureiShiteiCancelController.generatePdf(dto))
					.isInstanceOf(AccessDeniedException.class);
		}
	}

	@Nested
	@DisplayName("preview メソッドのテスト")
	class PreviewTest {

		@Test
		@DisplayName("正常系：プレビュー用のPDFデータおよび専用ヘッダーが正常に返却されること")
		void success() {
			TokureiShiteiCancelDto dto = new TokureiShiteiCancelDto();
			byte[] expectedPdf = new byte[]{4, 5, 6};

			when(reportsService.generateTsuchiPdf(dto)).thenReturn(expectedPdf);

			ResponseEntity<byte[]> response = tokureiShiteiCancelController.preview(dto);

			assertThat(response.getStatusCode().value()).isEqualTo(200);
			assertThat(response.getBody()).isEqualTo(expectedPdf);
			assertThat(response.getHeaders().getFirst("Cache-Control")).contains("no-cache");
			verify(accessChecker).checkAccess(SCREEN_ID);
		}

		@Test
		@DisplayName("異常系：アクセス権限がない場合にプレビュー処理が中断されること")
		void accessDeniedThrowsException() {
			TokureiShiteiCancelDto dto = new TokureiShiteiCancelDto();
			doThrow(new AccessDeniedException("Access Denied"))
					.when(accessChecker).checkAccess(SCREEN_ID);

			assertThatThrownBy(() -> tokureiShiteiCancelController.preview(dto))
					.isInstanceOf(AccessDeniedException.class);
		}
	}

	@Nested
	@DisplayName("print メソッドのテスト")
	class PrintTest {

		@Test
		@DisplayName("正常系：印刷用のPDFデータおよび印刷制御ヘッダーが正常に返却されること")
		void success() {
			TokureiShiteiCancelDto dto = new TokureiShiteiCancelDto();
			byte[] expectedPdf = new byte[]{7, 8, 9};

			when(reportsService.generateTsuchiPdf(dto)).thenReturn(expectedPdf);

			ResponseEntity<byte[]> response = tokureiShiteiCancelController.print(dto);

			assertThat(response.getStatusCode().value()).isEqualTo(200);
			assertThat(response.getBody()).isEqualTo(expectedPdf);
			assertThat(response.getHeaders().getFirst("X-Print-Action")).isEqualTo("true");
			verify(accessChecker).checkAccess(SCREEN_ID);
		}

		@Test
		@DisplayName("異常系：アクセス権限がない場合に印刷処理が中断されること")
		void accessDeniedThrowsException() {
			TokureiShiteiCancelDto dto = new TokureiShiteiCancelDto();
			doThrow(new AccessDeniedException("Access Denied"))
					.when(accessChecker).checkAccess(SCREEN_ID);

			assertThatThrownBy(() -> tokureiShiteiCancelController.print(dto))
					.isInstanceOf(AccessDeniedException.class);
		}
	}
}
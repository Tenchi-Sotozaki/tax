package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.GassanNonyuTsuchiDto;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.service.GassanNonyuTsuchiReportsService;
import jp.lg.asp.accommodation.service.GassanNonyuTsuchiService;
import jp.lg.asp.accommodation.util.SessionHelper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GassanNonyuTsuchiControllerTest {

	@InjectMocks
	private GassanNonyuTsuchiController gassanNonyuTsuchiController;

	@Mock
	private GassanNonyuTsuchiService gassanNonyuTsuchiService;

	@Mock
	private GassanNonyuTsuchiReportsService reportsService;

	@Mock
	private ScreenAccessChecker accessChecker;

	@Mock
	private JichitaiContext jichitaiContext;

	private static final String JICHITAI_CD = "123456";
	private static final String SHITEI_NO = "S001";
	private static final String GASSAN_SHITEI_NO = "GS001";

	@BeforeEach
	void setUp() {
		when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
	}

	@Nested
	@DisplayName("index メソッドのテスト")
	class IndexTest {

		@Test
		@DisplayName("正常系：合算指定情報および合算指定番号が存在し、発行年月日が設定済みの場合に画面表示が行われること")
		void success_withHakkoYmd() {
			MockHttpSession session = new MockHttpSession();
			Model model = new ConcurrentModel();

			ShiteiGassanSearchDto searchDto = new ShiteiGassanSearchDto();
			searchDto.setShiteiNo(SHITEI_NO);
			searchDto.setGassanShiteiNo(GASSAN_SHITEI_NO);
			
			GassanNonyuTsuchiDto infoDto = new GassanNonyuTsuchiDto();
			infoDto.setHakkoYmd(LocalDate.of(2026, 4, 1));
			when(gassanNonyuTsuchiService.getGassanNonyuTsuchiInfo(SHITEI_NO)).thenReturn(infoDto);

			try (MockedStatic<SessionHelper> mockedSessionHelper = Mockito.mockStatic(SessionHelper.class)) {
				mockedSessionHelper.when(() -> SessionHelper.getShiteiNo(session)).thenReturn(SHITEI_NO);
				mockedSessionHelper.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(searchDto);

				String viewName = gassanNonyuTsuchiController.index(session, model);

				assertThat(viewName).isEqualTo("reports/gassanNonyuTsuchi");
				assertThat(model.getAttribute("dto")).isEqualTo(infoDto);
				verify(accessChecker).checkAccess(ScreenManagement.GASSAN_NONYU_TSUCHI);
			}
		}

		@Test
		@DisplayName("正常系：合算指定番号はあるが発行年月日がnullの場合に本日日付が自動設定されて画面表示が行われること")
		void success_defaultHakkoYmd() {
			MockHttpSession session = new MockHttpSession();
			Model model = new ConcurrentModel();

			ShiteiGassanSearchDto searchDto = new ShiteiGassanSearchDto();
			searchDto.setShiteiNo(SHITEI_NO);
			searchDto.setGassanShiteiNo(GASSAN_SHITEI_NO);

			GassanNonyuTsuchiDto infoDto = new GassanNonyuTsuchiDto();
			infoDto.setHakkoYmd(null);
			when(gassanNonyuTsuchiService.getGassanNonyuTsuchiInfo(SHITEI_NO)).thenReturn(infoDto);

			try (MockedStatic<SessionHelper> mockedSessionHelper = Mockito.mockStatic(SessionHelper.class)) {
				mockedSessionHelper.when(() -> SessionHelper.getShiteiNo(session)).thenReturn(SHITEI_NO);
				mockedSessionHelper.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(searchDto);

				String viewName = gassanNonyuTsuchiController.index(session, model);

				assertThat(viewName).isEqualTo("reports/gassanNonyuTsuchi");
				GassanNonyuTsuchiDto resultDto = (GassanNonyuTsuchiDto) model.getAttribute("dto");
				assertThat(resultDto.getHakkoYmd()).isEqualTo(LocalDate.now());
				verify(accessChecker).checkAccess(ScreenManagement.GASSAN_NONYU_TSUCHI);
			}
		}

		@Test
		@DisplayName("境界値：セッションの選択情報（shiteiGassan）がnullの場合にモーダル表示とエラーメッセージが設定されること")
		void error_shiteiGassanIsNull() {
			MockHttpSession session = new MockHttpSession();
			Model model = new ConcurrentModel();

			try (MockedStatic<SessionHelper> mockedSessionHelper = Mockito.mockStatic(SessionHelper.class)) {
				mockedSessionHelper.when(() -> SessionHelper.getShiteiNo(session)).thenReturn(SHITEI_NO);
				mockedSessionHelper.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(null);

				String viewName = gassanNonyuTsuchiController.index(session, model);

				assertThat(viewName).isEqualTo("tokugimu/tTokugimuReport");
				assertThat(model.getAttribute("showShiteiGassanModal")).isEqualTo(true);
				assertThat(model.getAttribute("errorMessage")).isEqualTo("特別徴収義務者を指定してください。");
				verify(accessChecker).checkAccess(ScreenManagement.GASSAN_NONYU_TSUCHI);
			}
		}

		@Test
		@DisplayName("境界値：指定番号（shiteiNo）がnullまたは空の場合にモーダル表示とエラーメッセージが設定されること")
		void error_shiteiNoIsNull() {
			MockHttpSession session = new MockHttpSession();
			Model model = new ConcurrentModel();

			ShiteiGassanSearchDto searchDto = new ShiteiGassanSearchDto();
			searchDto.setShiteiNo(null);

			try (MockedStatic<SessionHelper> mockedSessionHelper = Mockito.mockStatic(SessionHelper.class)) {
				mockedSessionHelper.when(() -> SessionHelper.getShiteiNo(session)).thenReturn(null);
				mockedSessionHelper.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(searchDto);

				String viewName = gassanNonyuTsuchiController.index(session, model);

				assertThat(viewName).isEqualTo("tokugimu/tTokugimuReport");
				assertThat(model.getAttribute("showShiteiGassanModal")).isEqualTo(true);
				assertThat(model.getAttribute("errorMessage")).isEqualTo("特別徴収義務者を指定してください。");
			}
		}

		@Test
		@DisplayName("境界値：合算指定番号（gassanShiteiNo）がnullの場合にサービス呼び出しを行わず画面表示されること")
		void success_gassanShiteiNoIsNull() {
			MockHttpSession session = new MockHttpSession();
			Model model = new ConcurrentModel();

			ShiteiGassanSearchDto searchDto = new ShiteiGassanSearchDto();
			searchDto.setShiteiNo(SHITEI_NO);
			searchDto.setGassanShiteiNo(null);

			try (MockedStatic<SessionHelper> mockedSessionHelper = Mockito.mockStatic(SessionHelper.class)) {
				mockedSessionHelper.when(() -> SessionHelper.getShiteiNo(session)).thenReturn(SHITEI_NO);
				mockedSessionHelper.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(searchDto);

				String viewName = gassanNonyuTsuchiController.index(session, model);

				assertThat(viewName).isEqualTo("reports/gassanNonyuTsuchi");
				verify(gassanNonyuTsuchiService, never()).getGassanNonyuTsuchiInfo(anyString());
				verify(accessChecker).checkAccess(ScreenManagement.GASSAN_NONYU_TSUCHI);
			}
		}
	}

	@Nested
	@DisplayName("generatePdf メソッドのテスト")
	class GeneratePdfTest {

		@Test
		@DisplayName("正常系：PDF出力処理が正常に行われ、PDFデータと適切なヘッダーが返却されること")
		void success() {
			GassanNonyuTsuchiDto dto = new GassanNonyuTsuchiDto();
			dto.setShiteiNo(SHITEI_NO);
			byte[] dummyPdf = new byte[] { 1, 2, 3, 4 };

			when(reportsService.generateTsuchiPdf(dto)).thenReturn(dummyPdf);

			ResponseEntity<byte[]> response = gassanNonyuTsuchiController.generatePdf(dto);

			assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
			assertThat(response.getHeaders().getContentType().toString()).contains("application/pdf");
			assertThat(response.getBody()).isEqualTo(dummyPdf);
			verify(accessChecker).checkAccess(ScreenManagement.GASSAN_NONYU_TSUCHI);
		}
	}

	@Nested
	@DisplayName("preview メソッドのテスト")
	class PreviewTest {

		@Test
		@DisplayName("正常系：プレビュー処理が正常に行われ、キャッシュ制御ヘッダー付きのPDFデータが返却されること")
		void success() {
			GassanNonyuTsuchiDto dto = new GassanNonyuTsuchiDto();
			dto.setShiteiNo(SHITEI_NO);
			byte[] dummyPdf = new byte[] { 1, 2, 3, 4 };

			when(reportsService.generateTsuchiPdf(dto)).thenReturn(dummyPdf);

			ResponseEntity<byte[]> response = gassanNonyuTsuchiController.preview(dto);

			assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
			assertThat(response.getHeaders().getFirst("Cache-Control")).contains("no-cache");
			assertThat(response.getBody()).isEqualTo(dummyPdf);
			verify(accessChecker).checkAccess(ScreenManagement.GASSAN_NONYU_TSUCHI);
		}
	}

	@Nested
	@DisplayName("print メソッドのテスト")
	class PrintTest {

		@Test
		@DisplayName("正常系：印刷処理が正常に行われ、印刷アクション用ヘッダー付きのPDFデータが返却されること")
		void success() {
			GassanNonyuTsuchiDto dto = new GassanNonyuTsuchiDto();
			dto.setShiteiNo(SHITEI_NO);
			byte[] dummyPdf = new byte[] { 1, 2, 3, 4 };

			when(reportsService.generateTsuchiPdf(dto)).thenReturn(dummyPdf);

			ResponseEntity<byte[]> response = gassanNonyuTsuchiController.print(dto);

			assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
			assertThat(response.getHeaders().getFirst("X-Print-Action")).isEqualTo("true");
			assertThat(response.getBody()).isEqualTo(dummyPdf);
			verify(accessChecker).checkAccess(ScreenManagement.GASSAN_NONYU_TSUCHI);
		}
	}

	@Nested
	@DisplayName("権限チェックのテスト")
	class AccessCheckTest {

		@Test
		@DisplayName("異常系：画面アクセス権限がない場合に例外がスローされること")
		void accessDenied_throwsException() {
			MockHttpSession session = new MockHttpSession();
			Model model = new ConcurrentModel();

			doThrow(new AccessDeniedException("Access Denied"))
					.when(accessChecker).checkAccess(ScreenManagement.GASSAN_NONYU_TSUCHI);

			try (MockedStatic<SessionHelper> mockedSessionHelper = Mockito.mockStatic(SessionHelper.class)) {
				mockedSessionHelper.when(() -> SessionHelper.getShiteiNo(session)).thenReturn(SHITEI_NO);
				mockedSessionHelper.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(new ShiteiGassanSearchDto());

				assertThatThrownBy(() -> gassanNonyuTsuchiController.index(session, model))
						.isInstanceOf(AccessDeniedException.class);
			}
		}
	}
}
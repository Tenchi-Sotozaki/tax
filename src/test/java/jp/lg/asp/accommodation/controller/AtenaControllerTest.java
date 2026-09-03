package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.AtenaConfigForm;
import jp.lg.asp.accommodation.dto.AtenaDaichoItem;
import jp.lg.asp.accommodation.dto.AtenaSearchForm;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.AtenaRenkei;
import jp.lg.asp.accommodation.entity.AtenaRenkeiDef;
import jp.lg.asp.accommodation.exception.BusinessException;
import jp.lg.asp.accommodation.service.AtenaConfigService;
import jp.lg.asp.accommodation.service.AtenaImportService;
import jp.lg.asp.accommodation.service.AtenaService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AtenaControllerTest {

	@InjectMocks
	private AtenaController atenaController;

	@Mock
	private AtenaImportService atenaImportService;

	@Mock
	private AtenaConfigService atenaConfigService;

	@Mock
	private AtenaService atenaService;

	@Mock
	private ScreenAccessChecker accessChecker;

	@Mock
	private JichitaiContext jichitaiContext;

	@Mock
	private Model model;

	@Mock
	private BindingResult bindingResult;

	@Mock
	private RedirectAttributes redirectAttributes;

	private static final String JICHITAI_CD = "123456";

	@BeforeEach
	void setUp() {
		when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
	}

	@Nested
	@DisplayName("showRegister メソッドのテスト")
	class ShowRegisterTest {

		@Test
		@DisplayName("正常系：新規登録画面を表示し、空のフォームとモードが設定されること")
		void success() {
			String viewName = atenaController.showRegister(model);

			assertThat(viewName).isEqualTo("atena/atenaConfig");
			verify(model).addAttribute(eq("form"), any(AtenaConfigForm.class));
			verify(model).addAttribute("mode", "register");
		}
	}

	@Nested
	@DisplayName("register メソッドのテスト")
	class RegisterTest {

		@Test
		@DisplayName("正常系：必須項目および正しい個人番号（または法人番号）が入力され、正常に登録されること")
		void success() {
			AtenaConfigForm form = new AtenaConfigForm();
			form.setAtenaNo(BigDecimal.ONE);
			form.setName("テスト太郎");
			form.setKojinNo("123456789012");
			form.setHojinNo(null);

			when(bindingResult.hasErrors()).thenReturn(false);

			String viewName = atenaController.register(form, bindingResult, model);

			assertThat(viewName).isEqualTo("atena/atenaConfig");
			verify(atenaConfigService).register(any(Atena.class), eq(JICHITAI_CD));
			verify(model).addAttribute("successMessage", "宛名を登録しました。");
			verify(model).addAttribute("mode", "view");
		}

		@Test
		@DisplayName("異常系：宛名番号（atenaNo）が未設定の場合のバリデーションエラー")
		void atenaNoNull() {
			AtenaConfigForm form = new AtenaConfigForm();
			form.setAtenaNo(null);
			form.setName("テスト太郎");
			form.setKojinNo("123456789012");

			when(bindingResult.hasErrors()).thenReturn(true);

			String viewName = atenaController.register(form, bindingResult, model);

			assertThat(viewName).isEqualTo("atena/atenaConfig");
			verify(bindingResult).rejectValue(eq("atenaNo"), eq(""), eq("宛名の開始番号が設定されていません。管理者にお問い合わせください。"));
			verify(atenaConfigService, never()).register(any(), any());
			verify(model).addAttribute(eq("mode"), eq("register"));
		}

		@Test
		@DisplayName("境界値：個人番号と法人番号が両方未入力の場合のバリデーションエラー")
		void bothNumbersBlank() {
			AtenaConfigForm form = new AtenaConfigForm();
			form.setAtenaNo(BigDecimal.ONE);
			form.setName("テスト太郎");

			when(bindingResult.hasErrors()).thenReturn(true);

			String viewName = atenaController.register(form, bindingResult, model);

			assertThat(viewName).isEqualTo("atena/atenaConfig");
			verify(bindingResult).rejectValue(eq("kojinNo"), eq(""), any());
			verify(bindingResult).rejectValue(eq("hojinNo"), eq(""), any());
			verify(atenaConfigService, never()).register(any(), any());
			verify(model).addAttribute(eq("mode"), eq("register"));
		}

		@Test
		@DisplayName("異常系：個人番号と法人番号が同時に両方入力されている場合のバリデーションエラー")
		void bothNumbersProvided() {
			AtenaConfigForm form = new AtenaConfigForm();
			form.setAtenaNo(BigDecimal.ONE);
			form.setName("テスト太郎");
			form.setKojinNo("1234");
			form.setHojinNo("5678");

			when(bindingResult.hasErrors()).thenReturn(true);

			String viewName = atenaController.register(form, bindingResult, model);

			assertThat(viewName).isEqualTo("atena/atenaConfig");
			verify(bindingResult).rejectValue(eq("kojinNo"), eq(""), any());
			verify(bindingResult).rejectValue(eq("hojinNo"), eq(""), any());
			verify(atenaConfigService, never()).register(any(), any());
			verify(model).addAttribute(eq("mode"), eq("register"));
		}

		@Test
		@DisplayName("異常系：氏名が空の場合のバリデーションエラー")
		void nameBlank() {
			AtenaConfigForm form = new AtenaConfigForm();
			form.setAtenaNo(BigDecimal.ONE);
			form.setName("");
			form.setKojinNo("123456789012");

			when(bindingResult.hasErrors()).thenReturn(true);

			String viewName = atenaController.register(form, bindingResult, model);

			assertThat(viewName).isEqualTo("atena/atenaConfig");
			verify(bindingResult).rejectValue(eq("name"), eq(""), any());
			verify(model).addAttribute(eq("mode"), eq("register"));
		}

		@Test
		@DisplayName("異常系：法人番号でのビジネスロジック重複例外が発生した場合のエラーハンドリング")
		void duplicateHojinNoException() {
			AtenaConfigForm form = new AtenaConfigForm();
			form.setAtenaNo(BigDecimal.ONE);
			form.setName("テスト法人");
			form.setHojinNo("1234567890123");

			when(bindingResult.hasErrors()).thenReturn(false);
			BusinessException ex = new BusinessException("DUPLICATE_HOJIN_NO", "法人番号重複エラー");
			doThrow(ex).when(atenaConfigService).register(any(Atena.class), eq(JICHITAI_CD));

			String viewName = atenaController.register(form, bindingResult, model);

			assertThat(viewName).isEqualTo("atena/atenaConfig");
			verify(bindingResult).rejectValue(eq("hojinNo"), eq(""), any());
			verify(model).addAttribute(eq("mode"), eq("register"));
		}
	}

	@Nested
	@DisplayName("showEdit メソッドのテスト")
	class ShowEditTest {

		@Test
		@DisplayName("正常系：編集画面表示時に、取得したデータの個人番号がマスク（null化）された状態でフォームにセットされること")
		void success() {
			BigDecimal atenaNo = BigDecimal.ONE;
			Atena atena = new Atena();
			atena.setAtenaNo(atenaNo);
			atena.setKojinNo("sample_kojin_no");
			atena.setName("テスト太郎");

			when(atenaConfigService.findByAtenaNo(JICHITAI_CD, atenaNo)).thenReturn(atena);

			String viewName = atenaController.showEdit(atenaNo, model);

			assertThat(viewName).isEqualTo("atena/atenaConfig");
			verify(atenaConfigService).findByAtenaNo(JICHITAI_CD, atenaNo);
			verify(model).addAttribute("mode", "edit");
			verify(model).addAttribute(eq("form"), any(AtenaConfigForm.class));
		}
	}

	@Nested
	@DisplayName("edit メソッドのテスト")
	class EditTest {

		@Test
		@DisplayName("正常系：編集内容に問題がなく、正常に更新処理が完了すること")
		void success() {
			AtenaConfigForm form = new AtenaConfigForm();
			form.setAtenaNo(BigDecimal.ONE);
			form.setName("テスト次郎");

			when(bindingResult.hasErrors()).thenReturn(false);

			String viewName = atenaController.edit(form, bindingResult, model, redirectAttributes);

			assertThat(viewName).isEqualTo("redirect:/atena/view/1");
			verify(atenaConfigService).update(any(Atena.class), eq(JICHITAI_CD));
			verify(redirectAttributes).addFlashAttribute("successMessage", "宛名を更新しました。");
		}

		@Test
		@DisplayName("異常系：更新時にビジネスロジック例外（重複エラー）が発生した場合のエラーハンドリング")
		void businessException() {
			AtenaConfigForm form = new AtenaConfigForm();
			form.setAtenaNo(BigDecimal.ONE);
			form.setName("テスト次郎");
			form.setKojinNo("sample_kojin_no");

			when(bindingResult.hasErrors()).thenReturn(false);
			BusinessException ex = new BusinessException("DUPLICATE_KOJIN_NO", "重複エラー");
			doThrow(ex).when(atenaConfigService).update(any(Atena.class), eq(JICHITAI_CD));

			String viewName = atenaController.edit(form, bindingResult, model, redirectAttributes);

			assertThat(viewName).isEqualTo("atena/atenaConfig");
			verify(bindingResult).rejectValue(eq("kojinNo"), eq(""), any());
			verify(model).addAttribute("mode", "edit");
		}

		@Test
		@DisplayName("異常系：編集時に法人番号のビジネスロジック重複例外が発生した場合のエラーハンドリング")
		void duplicateHojinNoException() {
			AtenaConfigForm form = new AtenaConfigForm();
			form.setAtenaNo(BigDecimal.ONE);
			form.setName("テスト次郎");
			form.setHojinNo("1234567890123");

			when(bindingResult.hasErrors()).thenReturn(false);
			BusinessException ex = new BusinessException("DUPLICATE_HOJIN_NO", "重複エラー");
			doThrow(ex).when(atenaConfigService).update(any(Atena.class), eq(JICHITAI_CD));

			String viewName = atenaController.edit(form, bindingResult, model, redirectAttributes);

			assertThat(viewName).isEqualTo("atena/atenaConfig");
			verify(bindingResult).rejectValue(eq("hojinNo"), eq(""), any());
			verify(model).addAttribute("mode", "edit");
		}

		@Test
		@DisplayName("異常系：編集時に個人番号と法人番号が両方入力されている場合のバリデーションエラー")
		void bothNumbersProvided() {
			AtenaConfigForm form = new AtenaConfigForm();
			form.setAtenaNo(BigDecimal.ONE);
			form.setName("テスト次郎");
			form.setKojinNo("1234");
			form.setHojinNo("5678");

			when(bindingResult.hasErrors()).thenReturn(true);

			String viewName = atenaController.edit(form, bindingResult, model, redirectAttributes);

			assertThat(viewName).isEqualTo("atena/atenaConfig");
			verify(bindingResult).rejectValue(eq("kojinNo"), eq(""), any());
			verify(bindingResult).rejectValue(eq("hojinNo"), eq(""), any());
			verify(model).addAttribute("mode", "edit");
		}
	}

	@Nested
	@DisplayName("view メソッドのテスト")
	class ViewTest {

		@Test
		@DisplayName("正常系：指定した宛先Noのデータを取得し、参照モードで画面を表示すること")
		void success() {
			BigDecimal atenaNo = BigDecimal.ONE;
			Atena atena = new Atena();
			atena.setAtenaNo(atenaNo);
			atena.setName("テスト太郎");

			when(atenaConfigService.findByAtenaNo(JICHITAI_CD, atenaNo)).thenReturn(atena);

			String viewName = atenaController.view(atenaNo, model);

			assertThat(viewName).isEqualTo("atena/atenaConfig");
			verify(atenaConfigService).findByAtenaNo(JICHITAI_CD, atenaNo);
			verify(model).addAttribute("mode", "view");
			verify(model).addAttribute(eq("form"), any(AtenaConfigForm.class));
		}
	}

	@Nested
	@DisplayName("list メソッドのテスト")
	class ListTest {

		@Test
		@DisplayName("正常系：検索条件と searched=true を指定して一覧画面を表示すること")
		void list_searchedTrue_returnsViewWithItems() {
			AtenaSearchForm searchForm = new AtenaSearchForm();
			Model model = new ConcurrentModel();

			Atena atena = new Atena();
			atena.setAtenaNo(BigDecimal.ONE);
			List<AtenaDaichoItem> expectedItems = List.of(new AtenaDaichoItem(atena, BigDecimal.valueOf(100)));

			when(atenaService.searchDaicho(JICHITAI_CD, searchForm, true)).thenReturn(expectedItems);

			String viewName = atenaController.list(searchForm, true, model);

			assertThat(viewName).isEqualTo("atena/atenaDaicho");
			assertThat(model.getAttribute("items")).isEqualTo(expectedItems);
			assertThat(model.getAttribute("searchForm")).isEqualTo(searchForm);
			assertThat(model.getAttribute("isSearched")).isEqualTo(true);
			verify(accessChecker).checkAccess(ScreenManagement.ATENA_DAICHO);
		}

		@Test
		@DisplayName("境界値：検索結果が 0件（該当データなし）の場合、空のリストが設定されて一覧画面を表示すること")
		void list_emptyResult_returnsViewWithEmptyList() {
			AtenaSearchForm searchForm = new AtenaSearchForm();
			Model model = new ConcurrentModel();

			when(atenaService.searchDaicho(JICHITAI_CD, searchForm, true)).thenReturn(List.of());

			String viewName = atenaController.list(searchForm, true, model);

			assertThat(viewName).isEqualTo("atena/atenaDaicho");
			@SuppressWarnings("unchecked")
			List<AtenaDaichoItem> items = (List<AtenaDaichoItem>) model.getAttribute("items");
			assertThat(items).isEmpty();
			assertThat(model.getAttribute("isSearched")).isEqualTo(true);
			verify(accessChecker).checkAccess(ScreenManagement.ATENA_DAICHO);
		}

		@Test
		@DisplayName("異常系：画面アクセス権限がない場合に例外がスローされること")
		void list_accessDenied_throwsException() {
			AtenaSearchForm searchForm = new AtenaSearchForm();
			Model model = new ConcurrentModel();

			doThrow(new AccessDeniedException("Access Denied"))
					.when(accessChecker).checkAccess(ScreenManagement.ATENA_DAICHO);

			assertThatThrownBy(() -> atenaController.list(searchForm, false, model))
					.isInstanceOf(AccessDeniedException.class);
		}
	}

	@Nested
	@DisplayName("showImport メソッドのテスト")
	class ShowImportTest {

		@Test
		@DisplayName("正常系：取込画面を表示し、過去の取込履歴がモデルに設定されること")
		void success() {
			List<AtenaRenkei> expectedHistory = List.of();
			when(atenaImportService.findHistory(JICHITAI_CD)).thenReturn(expectedHistory);

			String viewName = atenaController.showImport(model);

			assertThat(viewName).isEqualTo("atena/atenaRenkei");
			verify(accessChecker).checkAccess(ScreenManagement.ATENA_INSERT);
			verify(atenaImportService).findHistory(JICHITAI_CD);
			verify(model).addAttribute("history", expectedHistory);
		}
	}

	@Nested
	@DisplayName("analyze メソッドのテスト")
	class AnalyzeTest {

		@Test
		@DisplayName("正常系：正常なCSVファイルをアップロードし、解析結果（プレビュー）が返却されセッションに保持されること")
		void success() throws Exception {
			MultipartFile file = new org.springframework.mock.web.MockMultipartFile(
					"file", "test.csv", "text/csv", "dummy,data".getBytes());
			HttpSession session = mock(HttpSession.class);
			jp.lg.asp.accommodation.dto.AtenaImportPreviewDto preview = new jp.lg.asp.accommodation.dto.AtenaImportPreviewDto();

			when(atenaImportService.analyze(file, JICHITAI_CD)).thenReturn(preview);

			ResponseEntity<?> response = atenaController.analyze(file, session);

			assertThat(response.getStatusCode().value()).isEqualTo(200);
			assertThat(response.getBody()).isEqualTo(preview);
			verify(accessChecker).checkWriteAccess(ScreenManagement.ATENA_INSERT);
			verify(session).setAttribute(eq("atenaImportPreview"), eq(preview));
		}

		@Test
		@DisplayName("境界値：ファイルが空の場合のエラーハンドリング")
		void fileEmpty() {
			MultipartFile file = new org.springframework.mock.web.MockMultipartFile(
					"file", "", "text/csv", new byte[0]);
			HttpSession session = mock(HttpSession.class);

			ResponseEntity<?> response = atenaController.analyze(file, session);

			assertThat(response.getStatusCode().value()).isEqualTo(400);
			verify(accessChecker).checkWriteAccess(ScreenManagement.ATENA_INSERT);
		}

		@Test
		@DisplayName("異常系：不正なファイル形式がアップロードされた場合のエラー")
		void invalidFileFormat() {
			MultipartFile file = new org.springframework.mock.web.MockMultipartFile(
					"file", "test.txt", "text/plain", "dummy,data".getBytes());
			HttpSession session = mock(HttpSession.class);

			ResponseEntity<?> response = atenaController.analyze(file, session);

			assertThat(response.getStatusCode().value()).isEqualTo(400);
			verify(accessChecker).checkWriteAccess(ScreenManagement.ATENA_INSERT);
		}

		@Test
		@DisplayName("境界値：Content-Type が不正またはnullの場合でも処理が継続されること")
		void invalidContentType() throws Exception {
			MultipartFile file = new org.springframework.mock.web.MockMultipartFile(
					"file", "test.csv", "application/octet-stream", "dummy,data".getBytes());
			HttpSession session = mock(HttpSession.class);
			jp.lg.asp.accommodation.dto.AtenaImportPreviewDto preview = new jp.lg.asp.accommodation.dto.AtenaImportPreviewDto();

			when(atenaImportService.analyze(file, JICHITAI_CD)).thenReturn(preview);

			ResponseEntity<?> response = atenaController.analyze(file, session);

			assertThat(response.getStatusCode().value()).isEqualTo(200);
			verify(atenaImportService).analyze(file, JICHITAI_CD);
		}

		@Test
		@DisplayName("異常系：analyze処理中に RuntimeException が発生した場合に 400 が返ること")
		void runtimeException() throws Exception {
			MultipartFile file = new org.springframework.mock.web.MockMultipartFile(
					"file", "test.csv", "text/csv", "dummy,data".getBytes());
			HttpSession session = mock(HttpSession.class);

			when(atenaImportService.analyze(file, JICHITAI_CD)).thenThrow(new RuntimeException("解析エラー"));

			ResponseEntity<?> response = atenaController.analyze(file, session);

			assertThat(response.getStatusCode().value()).isEqualTo(400);
		}

		@Test
		@DisplayName("異常系：analyze処理中に予期せぬ Exception が発生した場合に 400 が返ること")
		void systemException() throws Exception {
			MultipartFile file = new org.springframework.mock.web.MockMultipartFile(
					"file", "test.csv", "text/csv", "dummy,data".getBytes());
			HttpSession session = mock(HttpSession.class);

			when(atenaImportService.analyze(file, JICHITAI_CD)).thenThrow(new RuntimeException(new Exception("想定外エラー")));

			ResponseEntity<?> response = atenaController.analyze(file, session);

			assertThat(response.getStatusCode().value()).isEqualTo(400);
		}
	}

	@Nested
	@DisplayName("confirmImport メソッドのテスト")
	class ConfirmImportTest {

		@Test
		@DisplayName("正常系：セッションにプレビューが存在する状態で、選択された宛先Noをもとに取込が実行されること")
		void success() {
			HttpSession session = mock(HttpSession.class);
			jp.lg.asp.accommodation.dto.AtenaImportPreviewDto preview = new jp.lg.asp.accommodation.dto.AtenaImportPreviewDto();
			when(session.getAttribute("atenaImportPreview")).thenReturn(preview);

			org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
			when(auth.getName()).thenReturn("testUser");
			org.springframework.security.core.context.SecurityContext securityContext = mock(org.springframework.security.core.context.SecurityContext.class);
			when(securityContext.getAuthentication()).thenReturn(auth);
			SecurityContextHolder.setContext(securityContext);

			List<String> torikomuAtenaNo = List.of("1", "2");

			ResponseEntity<?> response = atenaController.confirmImport(torikomuAtenaNo, session);

			assertThat(response.getStatusCode().value()).isEqualTo(200);
			verify(accessChecker).checkWriteAccess(ScreenManagement.ATENA_INSERT);
			verify(atenaImportService).confirm(eq(preview), any(), eq(JICHITAI_CD), eq("testUser"));
			verify(session).removeAttribute("atenaImportPreview");
		}

		@Test
		@DisplayName("境界値：取込対象が1件も選択されていない場合の動作")
		void nullOrEmptySelection() {
			HttpSession session = mock(HttpSession.class);
			jp.lg.asp.accommodation.dto.AtenaImportPreviewDto preview = new jp.lg.asp.accommodation.dto.AtenaImportPreviewDto();
			when(session.getAttribute("atenaImportPreview")).thenReturn(preview);

			org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
			when(auth.getName()).thenReturn("testUser");
			org.springframework.security.core.context.SecurityContext securityContext = mock(org.springframework.security.core.context.SecurityContext.class);
			when(securityContext.getAuthentication()).thenReturn(auth);
			SecurityContextHolder.setContext(securityContext);

			ResponseEntity<?> response = atenaController.confirmImport(null, session);

			assertThat(response.getStatusCode().value()).isEqualTo(200);
			verify(accessChecker).checkWriteAccess(ScreenManagement.ATENA_INSERT);
			verify(atenaImportService).confirm(eq(preview), any(), eq(JICHITAI_CD), eq("testUser"));
			verify(session).removeAttribute("atenaImportPreview");
		}

		@Test
		@DisplayName("異常系：プレビューがnullの場合")
		void previewNull() {
			HttpSession session = mock(HttpSession.class);
			when(session.getAttribute("atenaImportPreview")).thenReturn(null);

			List<String> torikomuAtenaNo = List.of("1");

			ResponseEntity<?> response = atenaController.confirmImport(torikomuAtenaNo, session);

			assertThat(response.getStatusCode().value()).isEqualTo(400);
			verify(accessChecker).checkWriteAccess(ScreenManagement.ATENA_INSERT);
			verify(atenaImportService, never()).confirm(any(), any(), any(), any());
		}

		@Test
		@DisplayName("異常系：confirm処理中に RuntimeException が発生した場合に 400 が返ること")
		void runtimeException() {
			HttpSession session = mock(HttpSession.class);
			jp.lg.asp.accommodation.dto.AtenaImportPreviewDto preview = new jp.lg.asp.accommodation.dto.AtenaImportPreviewDto();
			when(session.getAttribute("atenaImportPreview")).thenReturn(preview);

			org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
			when(auth.getName()).thenReturn("testUser");
			org.springframework.security.core.context.SecurityContext securityContext = mock(org.springframework.security.core.context.SecurityContext.class);
			when(securityContext.getAuthentication()).thenReturn(auth);
			SecurityContextHolder.setContext(securityContext);

			doThrow(new RuntimeException("取込エラー")).when(atenaImportService).confirm(any(), any(), any(), any());

			ResponseEntity<?> response = atenaController.confirmImport(List.of("1"), session);

			assertThat(response.getStatusCode().value()).isEqualTo(400);
		}

		@Test
		@DisplayName("異常系：confirm処理中に予期せぬ Exception が発生した場合に 400 が返ること")
		void systemException() {
			HttpSession session = mock(HttpSession.class);
			jp.lg.asp.accommodation.dto.AtenaImportPreviewDto preview = new jp.lg.asp.accommodation.dto.AtenaImportPreviewDto();
			when(session.getAttribute("atenaImportPreview")).thenReturn(preview);

			org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
			when(auth.getName()).thenReturn("testUser");
			org.springframework.security.core.context.SecurityContext securityContext = mock(org.springframework.security.core.context.SecurityContext.class);
			when(securityContext.getAuthentication()).thenReturn(auth);
			SecurityContextHolder.setContext(securityContext);

			doThrow(new RuntimeException(new Exception("予期せぬエラー"))).when(atenaImportService).confirm(any(), any(), any(), any());

			ResponseEntity<?> response = atenaController.confirmImport(List.of("1"), session);

			assertThat(response.getStatusCode().value()).isEqualTo(400);
		}
	}

	@Nested
	@DisplayName("importDetail メソッドのテスト")
	class ImportDetailTest {

		@Test
		@DisplayName("正常系：指定したシーケンス番号に対応する取込結果明細リストを取得して返却すること")
		void success() {
			BigDecimal seq = BigDecimal.valueOf(1);
			AtenaRenkeiDef detail = new AtenaRenkeiDef();
			detail.setAtenaNo(BigDecimal.ONE);
			detail.setName("テスト");
			detail.setKbn("1");

			when(atenaImportService.findDetail(JICHITAI_CD, seq)).thenReturn(List.of(detail));

			ResponseEntity<?> response = atenaController.importDetail(seq);

			assertThat(response.getStatusCode().value()).isEqualTo(200);
			verify(accessChecker).checkAccess(ScreenManagement.ATENA_INSERT);
			verify(atenaImportService).findDetail(JICHITAI_CD, seq);
		}
	}
}
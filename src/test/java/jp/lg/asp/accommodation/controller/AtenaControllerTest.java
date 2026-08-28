package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.AtenaConfigForm;
import jp.lg.asp.accommodation.dto.AtenaImportPreviewDto;
import jp.lg.asp.accommodation.entity.Atena;
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
	private HttpSession session;

	@Mock
	private BindingResult bindingResult;

	@Mock
	private RedirectAttributes redirectAttributes;

	@Mock
	private Authentication authentication;

	@Mock
	private SecurityContext securityContext;

	private static final String JICHITAI_CD = "123456";

	@BeforeEach
	void setUp() {
		when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
	}

	@Nested
	@DisplayName("showImport メソッドのテスト")
	class ShowImportTest {

		@Test
		@DisplayName("正常系：取込画面を表示し、過去の取込履歴がモデルに設定されること")
		void success() {
			List historyList = List.of();
			when(atenaImportService.findHistory(JICHITAI_CD)).thenReturn(historyList);

			String viewName = atenaController.showImport(model);

			assertThat(viewName).isEqualTo("atena/atenaRenkei");
			verify(accessChecker).checkAccess(ScreenManagement.ATENA_INSERT);
			verify(atenaImportService).findHistory(JICHITAI_CD);
			verify(model).addAttribute("history", historyList);
		}
	}

	@Nested
	@DisplayName("analyze メソッドのテスト")
	class AnalyzeTest {

		@Test
		@DisplayName("正常系：正常なCSVファイルをアップロードし、解析結果（プレビュー）が返却されセッションに保持されること")
		void success() {
			MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "content".getBytes());
			AtenaImportPreviewDto previewDto = new AtenaImportPreviewDto();

			when(atenaImportService.analyze(file, JICHITAI_CD)).thenReturn(previewDto);

			ResponseEntity<?> response = atenaController.analyze(file, session);

			assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
			assertThat(response.getBody()).isEqualTo(previewDto);
			verify(accessChecker).checkWriteAccess(ScreenManagement.ATENA_INSERT);
			verify(session).setAttribute("atenaImportPreview", previewDto);
		}

		@Test
		@DisplayName("境界値：ファイルが空の場合のエラーハンドリング")
		void emptyFile() {
			MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", new byte[0]);

			ResponseEntity<?> response = atenaController.analyze(file, session);

			assertThat(response.getStatusCode().is4xxClientError()).isTrue();
			verify(accessChecker).checkWriteAccess(ScreenManagement.ATENA_INSERT);
			verify(atenaImportService, never()).analyze(any(), any());
		}

		@Test
		@DisplayName("異常系：不正なファイル形式がアップロードされた場合のエラー")
		void invalidFileFormat() {
			MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());

			ResponseEntity<?> response = atenaController.analyze(file, session);

			assertThat(response.getStatusCode().is4xxClientError()).isTrue();
			verify(atenaImportService, never()).analyze(any(), any());
		}
	}

	@Nested
	@DisplayName("confirmImport メソッドのテスト")
	class ConfirmImportTest {

		@Test
		@DisplayName("正常系：セッションにプレビューが存在する状態で、選択された宛先Noをもとに取込が実行されること")
		void success() {
			AtenaImportPreviewDto previewDto = new AtenaImportPreviewDto();
			when(session.getAttribute("atenaImportPreview")).thenReturn(previewDto);

			SecurityContextHolder.setContext(securityContext);
			when(securityContext.getAuthentication()).thenReturn(authentication);
			when(authentication.getName()).thenReturn("testUser");

			List<String> torikomuAtenaNo = List.of("1", "2");

			ResponseEntity<?> response = atenaController.confirmImport(torikomuAtenaNo, session);

			assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
			verify(accessChecker).checkWriteAccess(ScreenManagement.ATENA_INSERT);
			verify(atenaImportService).confirm(eq(previewDto), eq(Set.of("1", "2")), eq(JICHITAI_CD), eq("testUser"));
			verify(session).removeAttribute("atenaImportPreview");
		}

		@Test
		@DisplayName("境界値：取込対象が1件も選択されていない（null）場合の動作")
		void nullSelection() {
			AtenaImportPreviewDto previewDto = new AtenaImportPreviewDto();
			when(session.getAttribute("atenaImportPreview")).thenReturn(previewDto);

			SecurityContextHolder.setContext(securityContext);
			when(securityContext.getAuthentication()).thenReturn(authentication);
			when(authentication.getName()).thenReturn("testUser");

			ResponseEntity<?> response = atenaController.confirmImport(null, session);

			assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
			verify(atenaImportService).confirm(eq(previewDto), eq(Set.of()), eq(JICHITAI_CD), eq("testUser"));
			verify(session).removeAttribute("atenaImportPreview");
		}

		@Test
		@DisplayName("異常系：プレビューがnullの場合のエラーハンドリング")
		void previewNull() {
			when(session.getAttribute("atenaImportPreview")).thenReturn(null);

			ResponseEntity<?> response = atenaController.confirmImport(List.of("1"), session);

			assertThat(response.getStatusCode().is4xxClientError()).isTrue();
			verify(atenaImportService, never()).confirm(any(), any(), any(), any());
		}
	}

	@Nested
	@DisplayName("importDetail メソッドのテスト")
	class ImportDetailTest {

		@Test
		@DisplayName("正常系：指定したシーケンス番号に対応する取込結果明細リストを取得して返却すること")
		void success() {
			BigDecimal seq = BigDecimal.valueOf(1);
			when(atenaImportService.findDetail(JICHITAI_CD, seq)).thenReturn(List.of());

			ResponseEntity<?> response = atenaController.importDetail(seq);

			assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
			verify(accessChecker).checkAccess(ScreenManagement.ATENA_INSERT);
			verify(atenaImportService).findDetail(JICHITAI_CD, seq);
		}
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
		@DisplayName("正常系：必須項目および正しい個人番号が入力され、正常に登録されること")
		void success() {
			AtenaConfigForm form = new AtenaConfigForm();
			form.setName("テスト太郎");
			form.setKojinNo("123456789012");

			when(bindingResult.hasErrors()).thenReturn(false);

			String viewName = atenaController.register(form, bindingResult, model, redirectAttributes);

			assertThat(viewName).isEqualTo("redirect:/atena/list");
			verify(atenaConfigService).register(any(Atena.class), eq(JICHITAI_CD));
			verify(redirectAttributes).addFlashAttribute("successMessage", "宛名を登録しました。");
		}

		@Test
		@DisplayName("境界値：個人番号と法人番号が両方未入力の場合のバリデーションエラー")
		void bothNumbersBlank() {
			AtenaConfigForm form = new AtenaConfigForm();
			form.setName("テスト太郎");

			when(bindingResult.hasErrors()).thenReturn(true);

			String viewName = atenaController.register(form, bindingResult, model, redirectAttributes);

			assertThat(viewName).isEqualTo("atena/atenaConfig");
			verify(bindingResult).rejectValue(eq("kojinNo"), eq(""), any());
			verify(bindingResult).rejectValue(eq("hojinNo"), eq(""), any());
			verify(atenaConfigService, never()).register(any(), any());
		}

		@Test
		@DisplayName("異常系：個人番号と法人番号が同時に両方入力されている場合のバリデーションエラー")
		void bothNumbersProvided() {
			AtenaConfigForm form = new AtenaConfigForm();
			form.setName("テスト太郎");
			form.setKojinNo("123456");
			form.setHojinNo("789012");

			when(bindingResult.hasErrors()).thenReturn(true);

			String viewName = atenaController.register(form, bindingResult, model, redirectAttributes);

			assertThat(viewName).isEqualTo("atena/atenaConfig");
			verify(bindingResult).rejectValue(eq("kojinNo"), eq(""), any());
			verify(bindingResult).rejectValue(eq("hojinNo"), eq(""), any());
			verify(atenaConfigService, never()).register(any(), any());
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
			atena.setKojinNo("123456789012");
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
		@DisplayName("異常系：更新時にビジネスロジック例外が発生した場合のエラーハンドリング")
		void businessException() {
			AtenaConfigForm form = new AtenaConfigForm();
			form.setAtenaNo(BigDecimal.ONE);
			form.setName("テスト次郎");
			form.setKojinNo("123456789012");

			when(bindingResult.hasErrors()).thenReturn(false);
			BusinessException ex = new BusinessException("DUPLICATE_KOJIN_NO", "重複エラー");
			doThrow(ex).when(atenaConfigService).update(any(Atena.class), eq(JICHITAI_CD));

			String viewName = atenaController.edit(form, bindingResult, model, redirectAttributes);

			assertThat(viewName).isEqualTo("atena/atenaConfig");
			verify(bindingResult).rejectValue(eq("kojinNo"), eq(""), any());
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
}
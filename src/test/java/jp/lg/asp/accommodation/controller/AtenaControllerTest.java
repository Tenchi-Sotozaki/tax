package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

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
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.AtenaConfigForm;
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
		@DisplayName("異常系：更新時にビジネスロジック例外（例: 重複エラー）が発生した場合のエラーハンドリング")
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
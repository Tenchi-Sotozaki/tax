package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import org.assertj.core.api.InstanceOfAssertFactories;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.ShoreikinConfigDto;
import jp.lg.asp.accommodation.service.ShoreikinConfigService;
import jp.lg.asp.accommodation.util.SessionHelper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShoreikinConfigControllerTest {

    @Mock
    private ShoreikinConfigService shoreikinConfigService;

    @Mock
    private ScreenAccessChecker accessChecker;

    @InjectMocks
    private ShoreikinConfigController shoreikinConfigController;

    @Nested
    @DisplayName("config メソッドのテスト")
    class ConfigTest {

        private MockedStatic<SessionHelper> sessionHelperMock;

        @BeforeEach
        void setUp() {
            sessionHelperMock = mockStatic(SessionHelper.class);
        }

        @org.junit.jupiter.api.AfterEach
        void tearDown() {
            sessionHelperMock.close();
        }

        @Test
        @DisplayName("正常系：指定番号が存在し、年度が指定されている場合に正常にデータが取得できること")
        void config_success_withNendo() {
            MockHttpSession session = new MockHttpSession();
            Model model = new ConcurrentModel();
            sessionHelperMock.when(() -> SessionHelper.getShiteiNo(session)).thenReturn("12345");
            when(shoreikinConfigService.getShoreikin("12345", "2026")).thenReturn(new ShoreikinConfigDto());

            String viewName = shoreikinConfigController.config(session, "2026", model);

            assertThat(viewName).isEqualTo("shoreikin/shoreikinConfig");
            assertThat(model.containsAttribute("configForm")).isTrue();
            verify(shoreikinConfigService, times(1)).getShoreikin("12345", "2026");
        }

        @Test
        @DisplayName("境界値：年度パラメータが省略（null）された状態で取得できること")
        void config_nullNendo_success() {
            MockHttpSession session = new MockHttpSession();
            Model model = new ConcurrentModel();
            sessionHelperMock.when(() -> SessionHelper.getShiteiNo(session)).thenReturn("12345");
            when(shoreikinConfigService.getShoreikin("12345", null)).thenReturn(new ShoreikinConfigDto());

            String viewName = shoreikinConfigController.config(session, null, model);

            assertThat(viewName).isEqualTo("shoreikin/shoreikinConfig");
            assertThat(model.containsAttribute("configForm")).isTrue();
        }

        @Test
        @DisplayName("異常系：指定番号がnullの場合、合算モーダル表示フラグが立つこと")
        void config_noShiteiNo_null_showsModal() {
            MockHttpSession session = new MockHttpSession();
            Model model = new ConcurrentModel();
            sessionHelperMock.when(() -> SessionHelper.getShiteiNo(session)).thenReturn(null);

            String viewName = shoreikinConfigController.config(session, "2026", model);

            assertThat(viewName).isEqualTo("shoreikin/shoreikinConfig");
            assertThat(model.getAttribute("showShiteiGassanModal")).isEqualTo(true);
            assertThat(model.containsAttribute("configForm")).isTrue();
            verify(shoreikinConfigService, never()).getShoreikin(any(), any());
        }

        @Test
        @DisplayName("異常系：指定番号が空文字の場合、合算モーダル表示フラグが立つこと")
        void config_noShiteiNo_empty_showsModal() {
            MockHttpSession session = new MockHttpSession();
            Model model = new ConcurrentModel();
            sessionHelperMock.when(() -> SessionHelper.getShiteiNo(session)).thenReturn("");

            String viewName = shoreikinConfigController.config(session, "2026", model);

            assertThat(viewName).isEqualTo("shoreikin/shoreikinConfig");
            assertThat(model.getAttribute("showShiteiGassanModal")).isEqualTo(true);
            verify(shoreikinConfigService, never()).getShoreikin(any(), any());
        }
    }

    @Nested
    @DisplayName("switchMode メソッドのテスト")
    class SwitchModeTest {

        @Test
        @DisplayName("正常系：指定したモードに正常に切り替わり、フォームが維持されること")
        void switchMode_success() {
            ShoreikinConfigDto form = new ShoreikinConfigDto();
            Model model = new ConcurrentModel();

            String viewName = shoreikinConfigController.switchMode("edit", form, model);

            assertThat(viewName).isEqualTo("shoreikin/shoreikinConfig");
            assertThat(form.getMode()).isEqualTo("edit");
            assertThat(model.containsAttribute("configForm")).isTrue();
        }
    }

    @Nested
    @DisplayName("calculate メソッドのテスト")
    class CalculateTest {

        @Test
        @DisplayName("正常系：交付金情報の算出が正常に行われること")
        void calculate_success() {
            ShoreikinConfigDto form = new ShoreikinConfigDto();
            Model model = new ConcurrentModel();
            when(shoreikinConfigService.calculateShoreikin(form)).thenReturn(form);

            String viewName = shoreikinConfigController.calculate(form, model);

            assertThat(viewName).isEqualTo("shoreikin/shoreikinConfig");
            assertThat(model.containsAttribute("configForm")).isTrue();
        }

        @Test
        @DisplayName("異常系：IllegalStateException発生時、交付率がnullにクリアされること")
        void calculate_illegalState_clearsKofuRitsu() {
            ShoreikinConfigDto form = new ShoreikinConfigDto();
            form.setKofuRitsu(BigDecimal.TEN);
            Model model = new ConcurrentModel();
            when(shoreikinConfigService.calculateShoreikin(form)).thenThrow(new IllegalStateException("error"));

            String viewName = shoreikinConfigController.calculate(form, model);

            assertThat(viewName).isEqualTo("shoreikin/shoreikinConfig");
            assertThat(form.getKofuRitsu()).isNull();
            assertThat(model.containsAttribute("configForm")).isTrue();
        }

        @Test
        @DisplayName("異常系：予期せぬ例外発生時、エラーメッセージが設定されること")
        void calculate_unexpectedException_setsErrorMessage() {
            ShoreikinConfigDto form = new ShoreikinConfigDto();
            Model model = new ConcurrentModel();
            when(shoreikinConfigService.calculateShoreikin(form)).thenThrow(new RuntimeException("error"));

            String viewName = shoreikinConfigController.calculate(form, model);

            assertThat(viewName).isEqualTo("shoreikin/shoreikinConfig");
            assertThat(model.getAttribute("errorMessage")).isEqualTo("交付金情報の算出に失敗しました。時間をおいて再度お試しください。");
            assertThat(model.containsAttribute("configForm")).isTrue();
        }
    }

    @Nested
    @DisplayName("create メソッドのテスト")
    class CreateTest {

        @Mock
        private BindingResult bindingResult;

        @Test
        @DisplayName("異常系：バリデーションエラーがある場合、登録処理を行わずに画面を再描画すること")
        void create_hasErrors_returnsView() {
            ShoreikinConfigDto form = new ShoreikinConfigDto();
            Model model = new ConcurrentModel();
            RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
            when(bindingResult.hasErrors()).thenReturn(true);

            String viewName = shoreikinConfigController.create(form, bindingResult, model, redirectAttributes);

            assertThat(viewName).isEqualTo("shoreikin/shoreikinConfig");
            assertThat(form.getMode()).isEqualTo("create");
            assertThat(model.containsAttribute("validationErrors")).isTrue();
            verify(shoreikinConfigService, never()).createShoreikin(any());
        }

        @Test
        @DisplayName("正常系：入力値に問題がなく、交付金情報が新規登録されること")
        void create_success() {
            ShoreikinConfigDto form = new ShoreikinConfigDto();
            Model model = new ConcurrentModel();
            RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
            when(bindingResult.hasErrors()).thenReturn(false);

            String viewName = shoreikinConfigController.create(form, bindingResult, model, redirectAttributes);

            assertThat(viewName).isEqualTo("redirect:/shoreikin/list");
            assertThat(redirectAttributes.getFlashAttributes())
                    .asInstanceOf(InstanceOfAssertFactories.MAP)
                    .containsEntry("successMessage", "交付金情報を登録しました。");
            verify(shoreikinConfigService, times(1)).createShoreikin(form);
        }

        @Test
        @DisplayName("異常系：主キー重複時、専用のエラーメッセージが設定されること")
        void create_duplicateKey_setsErrorMessage() {
            ShoreikinConfigDto form = new ShoreikinConfigDto();
            Model model = new ConcurrentModel();
            RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
            when(bindingResult.hasErrors()).thenReturn(false);
            doThrow(new DataIntegrityViolationException("duplicate")).when(shoreikinConfigService).createShoreikin(form);

            String viewName = shoreikinConfigController.create(form, bindingResult, model, redirectAttributes);

            assertThat(viewName).isEqualTo("redirect:/shoreikin/list");
            assertThat(redirectAttributes.getFlashAttributes())
                    .asInstanceOf(InstanceOfAssertFactories.MAP)
                    .containsEntry("errorMessage", "この年度の交付金情報は既に登録されています。一覧から対象を選び直してください。");
        }

        @Test
        @DisplayName("異常系：予期せぬ例外発生時、エラーメッセージが設定されること")
        void create_unexpectedException_setsErrorMessage() {
            ShoreikinConfigDto form = new ShoreikinConfigDto();
            Model model = new ConcurrentModel();
            RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
            when(bindingResult.hasErrors()).thenReturn(false);
            doThrow(new RuntimeException("error")).when(shoreikinConfigService).createShoreikin(form);

            String viewName = shoreikinConfigController.create(form, bindingResult, model, redirectAttributes);

            assertThat(viewName).isEqualTo("redirect:/shoreikin/list");
            assertThat(redirectAttributes.getFlashAttributes())
                    .asInstanceOf(InstanceOfAssertFactories.MAP)
                    .containsEntry("errorMessage", "交付金情報の登録に失敗しました。時間をおいて再度お試しください。");
        }
    }

    @Nested
    @DisplayName("update メソッドのテスト")
    class UpdateTest {

        @Mock
        private BindingResult bindingResult;

        @Test
        @DisplayName("異常系：バリデーションエラーがある場合、更新処理を行わずに画面を再描画すること")
        void update_hasErrors_returnsView() {
            ShoreikinConfigDto form = new ShoreikinConfigDto();
            Model model = new ConcurrentModel();
            RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
            when(bindingResult.hasErrors()).thenReturn(true);

            String viewName = shoreikinConfigController.update(form, bindingResult, model, redirectAttributes);

            assertThat(viewName).isEqualTo("shoreikin/shoreikinConfig");
            assertThat(form.getMode()).isEqualTo("edit");
            assertThat(model.containsAttribute("validationErrors")).isTrue();
            verify(shoreikinConfigService, never()).updateShoreikin(any());
        }

        @Test
        @DisplayName("正常系：入力値に問題がなく、交付金情報が正常に更新されること")
        void update_success() {
            ShoreikinConfigDto form = new ShoreikinConfigDto();
            Model model = new ConcurrentModel();
            RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
            when(bindingResult.hasErrors()).thenReturn(false);

            String viewName = shoreikinConfigController.update(form, bindingResult, model, redirectAttributes);

            assertThat(viewName).isEqualTo("redirect:/shoreikin/list");
            assertThat(redirectAttributes.getFlashAttributes())
                    .asInstanceOf(InstanceOfAssertFactories.MAP)
                    .containsEntry("successMessage", "交付金情報を更新しました。");
            verify(shoreikinConfigService, times(1)).updateShoreikin(form);
        }

        @Test
        @DisplayName("異常系：楽観的ロックエラー時、共通の更新エラーメッセージが設定されること")
        void update_optimisticLock_setsErrorMessage() {
            ShoreikinConfigDto form = new ShoreikinConfigDto();
            Model model = new ConcurrentModel();
            RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
            when(bindingResult.hasErrors()).thenReturn(false);
            doThrow(new OptimisticLockingFailureException("conflict")).when(shoreikinConfigService).updateShoreikin(form);

            String viewName = shoreikinConfigController.update(form, bindingResult, model, redirectAttributes);

            assertThat(viewName).isEqualTo("redirect:/shoreikin/list");
            assertThat(redirectAttributes.getFlashAttributes())
                    .asInstanceOf(InstanceOfAssertFactories.MAP)
                    .containsEntry("errorMessage", "交付金情報の更新に失敗しました。時間をおいて再度お試しください。");
        }

        @Test
        @DisplayName("異常系：予期せぬ例外発生時、共通の更新エラーメッセージが設定されること")
        void update_unexpectedException_setsErrorMessage() {
            ShoreikinConfigDto form = new ShoreikinConfigDto();
            Model model = new ConcurrentModel();
            RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
            when(bindingResult.hasErrors()).thenReturn(false);
            doThrow(new RuntimeException("error")).when(shoreikinConfigService).updateShoreikin(form);

            String viewName = shoreikinConfigController.update(form, bindingResult, model, redirectAttributes);

            assertThat(viewName).isEqualTo("redirect:/shoreikin/list");
            assertThat(redirectAttributes.getFlashAttributes())
                    .asInstanceOf(InstanceOfAssertFactories.MAP)
                    .containsEntry("errorMessage", "交付金情報の更新に失敗しました。時間をおいて再度お試しください。");
        }
    }
}
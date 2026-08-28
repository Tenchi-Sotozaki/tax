package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.UserForm;
import jp.lg.asp.accommodation.dto.UserSearchForm;
import jp.lg.asp.accommodation.entity.Role;
import jp.lg.asp.accommodation.entity.User;
import jp.lg.asp.accommodation.service.AdminUserService;

@ExtendWith(MockitoExtension.class)
class AdminUserCrudControllerTest {

    @Mock AdminUserService adminUserService;
    @Mock ScreenAccessChecker accessChecker;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks AdminUserController controller;

    private static final String SCREEN_ID_CONFIG = ScreenManagement.USER_CONFIG;
    private static final String JICHITAI_CD = "01100";

    private Role role(long roleId, String name) {
        Role r = new Role();
        r.setRoleId(roleId);
        r.setName(name);
        return r;
    }

    private User user(String id) {
        User u = new User();
        u.setId(id);
        u.setName("テスト");
        u.setNameKana("テスト");
        u.setBusho("総務課");
        u.setRoleId(BigDecimal.ONE);
        return u;
    }

    private UserForm validForm(String id, String password, String passwordConfirm) {
        UserForm form = new UserForm();
        form.setId(id);
        form.setName("山田太郎");
        form.setNameKana("ヤマダタロウ");
        form.setBusho("総務課");
        form.setRoleId(BigDecimal.ONE);
        form.setPassword(password);
        form.setPasswordConfirm(passwordConfirm);
        return form;
    }

    // ── showRegistrationForm ──────────────────────────────────────

    @Test
    void showRegistrationForm_登録画面初期表示() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(adminUserService.selectableRoles(JICHITAI_CD, null)).thenReturn(List.of(role(1L, "管理者")));
        Model model = new ExtendedModelMap();

        String view = controller.showRegistrationForm(model);

        assertThat(view).isEqualTo("admin/userConfig");
        assertThat(model.asMap().get("userForm")).isNotNull();
        assertThat(model.asMap().get("isEdit")).isEqualTo(false);
        assertThat(model.asMap().get("isView")).isEqualTo(false);
        assertThat(model.asMap().get("isDefaultUser")).isEqualTo(false);
        verify(accessChecker).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    // ── register ──────────────────────────────────────────────────

    @Test
    void register_正常登録_successMessageを積んでリダイレクト() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(adminUserService.existsActiveUser("U001")).thenReturn(false);
        when(adminUserService.selectableRoles(eq(JICHITAI_CD), any())).thenReturn(List.of(role(1L, "管理者")));
        UserForm form = validForm("U001", "pass", "pass");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.register(form, bindingResult, new ExtendedModelMap(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/user-view/U001");
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
                .isEqualTo("ユーザーを登録しました。");
        verify(adminUserService).register(form);
        verify(accessChecker).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    @Test
    void register_パスワード不一致_エラーが設定され登録画面に戻る() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(adminUserService.selectableRoles(eq(JICHITAI_CD), any())).thenReturn(List.of(role(1L, "管理者")));
        UserForm form = validForm("U001", "pass1", "pass2");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");

        String view = controller.register(form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/userConfig");
        assertThat(bindingResult.hasFieldErrors("passwordConfirm")).isTrue();
        verify(adminUserService, never()).register(any());
        verify(accessChecker).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    @Test
    void register_重複ID_bindingResultにエラーが設定され登録画面に戻る() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(adminUserService.existsActiveUser("U001")).thenReturn(true);
        when(adminUserService.selectableRoles(eq(JICHITAI_CD), any())).thenReturn(List.of(role(1L, "管理者")));
        UserForm form = validForm("U001", "pass", "pass");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");

        String view = controller.register(form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/userConfig");
        assertThat(bindingResult.hasFieldErrors("id")).isTrue();
        verify(adminUserService, never()).register(any());
        verify(accessChecker).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    // ── showEditForm ──────────────────────────────────────────────

    @Test
    void showEditForm_編集画面初期表示_isDefaultUser_false() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(adminUserService.findById("U001")).thenReturn(user("U001"));
        when(adminUserService.selectableRoles(eq(JICHITAI_CD), any())).thenReturn(List.of(role(1L, "管理者")));
        Model model = new ExtendedModelMap();

        String view = controller.showEditForm("U001", model);

        assertThat(view).isEqualTo("admin/userConfig");
        assertThat(model.asMap().get("isEdit")).isEqualTo(true);
        assertThat(model.asMap().get("isView")).isEqualTo(false);
        assertThat(model.asMap().get("isDefaultUser")).isEqualTo(false);
        verify(accessChecker).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    @Test
    void showEditForm_ADMIN_IDユーザー_isDefaultUser_true() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(adminUserService.findById(InitialPasswordController.ADMIN_ID))
                .thenReturn(user(InitialPasswordController.ADMIN_ID));
        when(adminUserService.selectableRoles(eq(JICHITAI_CD), any())).thenReturn(List.of(role(1L, "管理者")));
        Model model = new ExtendedModelMap();

        controller.showEditForm(InitialPasswordController.ADMIN_ID, model);

        assertThat(model.asMap().get("isDefaultUser")).isEqualTo(true);
        verify(accessChecker).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    // ── showViewForm ──────────────────────────────────────────────

    @Test
    void showViewForm_照会画面表示_isEdit_true_isView_true() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(adminUserService.findById("U001")).thenReturn(user("U001"));
        when(adminUserService.selectableRoles(eq(JICHITAI_CD), any())).thenReturn(List.of(role(1L, "管理者")));
        Model model = new ExtendedModelMap();

        String view = controller.showViewForm("U001", model);

        assertThat(view).isEqualTo("admin/userConfig");
        assertThat(model.asMap().get("isEdit")).isEqualTo(true);
        assertThat(model.asMap().get("isView")).isEqualTo(true);
        verify(accessChecker).checkAccess(SCREEN_ID_CONFIG);
    }

    // ── update ────────────────────────────────────────────────────

    @Test
    void update_正常更新_successMessageを積んでリダイレクト() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(adminUserService.findById("U001")).thenReturn(user("U001"));
        UserForm form = validForm("U001", "", "");
        form.setName("鈴木");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.update("U001", form, bindingResult, new ExtendedModelMap(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/user-view/U001");
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
                .isEqualTo("ユーザー情報を更新しました。");
        verify(adminUserService).update("U001", form);
        verify(accessChecker).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    @Test
    void update_パスワード不一致_エラーが設定され編集画面に戻る() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(adminUserService.findById("U001")).thenReturn(user("U001"));
        when(adminUserService.selectableRoles(eq(JICHITAI_CD), any())).thenReturn(List.of(role(1L, "管理者")));
        UserForm form = validForm("U001", "pass1", "pass2");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");

        String view = controller.update("U001", form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/userConfig");
        assertThat(bindingResult.hasFieldErrors("passwordConfirm")).isTrue();
        verify(adminUserService, never()).update(any(), any());
        verify(accessChecker).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    @Test
    void update_パスワード欄がnullのまま更新_パスワードチェックをスキップして正常に更新処理へ流れる() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(adminUserService.findById("U001")).thenReturn(user("U001"));
        UserForm form = validForm("U001", null, null);
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.update("U001", form, bindingResult, new ExtendedModelMap(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/user-view/U001");
        verify(adminUserService).update("U001", form);
        verify(accessChecker).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    // ── delete ────────────────────────────────────────────────────

    @Test
    void delete_正常削除_successMessageを積んで検索画面へリダイレクト() {
        when(adminUserService.isLoginUser("U001")).thenReturn(false);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.delete("U001", redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/user-search");
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
                .isEqualTo("ユーザーを削除しました。");
        verify(adminUserService).delete("U001");
        verify(accessChecker).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    @Test
    void delete_ログイン中の自ユーザーを削除しようとした場合_errorMessageを積んで検索画面へリダイレクト() {
        when(adminUserService.isLoginUser("U001")).thenReturn(true);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.delete("U001", redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/user-search");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("ログイン中のユーザーは削除できません。");
        verify(adminUserService, never()).delete(any());
        verify(accessChecker).checkWriteAccess(SCREEN_ID_CONFIG);
    }
}

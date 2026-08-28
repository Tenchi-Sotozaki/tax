package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
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
class AdminUserControllerTest {

    @Mock AdminUserService adminUserService;
    @Mock ScreenAccessChecker accessChecker;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks AdminUserController controller;

    private static final String SCREEN_ID = ScreenManagement.USER_MANAGEMENT;
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

    // ── list ──────────────────────────────────────────────────────

    @Test
    void list_searched_falseの場合_検索を実行せず空リストを返す() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(adminUserService.selectableRoles(JICHITAI_CD, null)).thenReturn(List.of(role(1L, "管理者")));
        Model model = new ExtendedModelMap();

        String view = controller.list(new UserSearchForm(), false, model);

        assertThat(view).isEqualTo("admin/userDaicho");
        assertThat((List<?>) model.asMap().get("items")).isEmpty();
        assertThat(model.asMap().get("searched")).isEqualTo(false);
        verify(adminUserService, never()).searchAll(any());
        verify(accessChecker).checkAccess(SCREEN_ID);
    }

    @Test
    void list_searched_trueの場合_searchAllを呼び結果をモデルに設定する() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        User user1 = user("U001");
        when(adminUserService.searchAll(any())).thenReturn(List.of(user1));
        when(adminUserService.selectableRoles(JICHITAI_CD, null)).thenReturn(List.of(role(1L, "管理者")));
        Model model = new ExtendedModelMap();

        String view = controller.list(new UserSearchForm(), true, model);

        assertThat(view).isEqualTo("admin/userDaicho");
        assertThat((List<?>) model.asMap().get("items")).hasSize(1);
        assertThat(model.asMap().get("searched")).isEqualTo(true);
        verify(accessChecker).checkAccess(SCREEN_ID);
    }

    @Test
    void list_rolesとroleMapがモデルに設定される() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        Role role1 = role(1L, "管理者");
        when(adminUserService.selectableRoles(JICHITAI_CD, null)).thenReturn(List.of(role1));
        Model model = new ExtendedModelMap();

        controller.list(new UserSearchForm(), false, model);

        @SuppressWarnings("unchecked")
        List<Role> roles = (List<Role>) model.asMap().get("roles");
        assertThat(roles).containsExactly(role1);
        @SuppressWarnings("unchecked")
        var roleMap = (java.util.Map<Long, String>) model.asMap().get("roleMap");
        assertThat(roleMap).containsEntry(1L, "管理者");
        verify(accessChecker).checkAccess(SCREEN_ID);
    }

    // ── register ──────────────────────────────────────────────────

    @Test
    void register_パスワード不一致の場合_エラーが設定され登録画面に戻る() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(adminUserService.selectableRoles(eq(JICHITAI_CD), any())).thenReturn(List.of(role(1L, "管理者")));
        UserForm form = new UserForm();
        form.setId("U001");
        form.setName("山田太郎");
        form.setNameKana("ヤマダタロウ");
        form.setBusho("総務課");
        form.setRoleId(BigDecimal.ONE);
        form.setPassword("pass1");
        form.setPasswordConfirm("pass2");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");
        Model model = new ExtendedModelMap();

        String view = controller.register(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/userConfig");
        assertThat(bindingResult.hasFieldErrors("passwordConfirm")).isTrue();
        verify(adminUserService, never()).register(any());
        verify(accessChecker).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    @Test
    void register_既存アクティブユーザーと重複するIDで登録_エラーが設定され登録画面に戻る() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(adminUserService.existsActiveUser("U001")).thenReturn(true);
        when(adminUserService.selectableRoles(eq(JICHITAI_CD), any())).thenReturn(List.of(role(1L, "管理者")));
        UserForm form = new UserForm();
        form.setId("U001");
        form.setName("山田太郎");
        form.setNameKana("ヤマダタロウ");
        form.setBusho("総務課");
        form.setRoleId(BigDecimal.ONE);
        form.setPassword("pass");
        form.setPasswordConfirm("pass");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");
        Model model = new ExtendedModelMap();

        String view = controller.register(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/userConfig");
        assertThat(bindingResult.hasFieldErrors("id")).isTrue();
        verify(adminUserService, never()).register(any());
        verify(accessChecker).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    // ── update ────────────────────────────────────────────────────

    @Test
    void update_パスワード欄が空のまま更新_パスワードチェックをスキップして正常に更新処理へ流れる() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(adminUserService.findById("U001")).thenReturn(user("U001"));
        UserForm form = new UserForm();
        form.setId("U001");
        form.setName("山田太郎");
        form.setNameKana("ヤマダタロウ");
        form.setBusho("総務課");
        form.setRoleId(BigDecimal.ONE);
        form.setPassword("");
        form.setPasswordConfirm("");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.update("U001", form, bindingResult, new ExtendedModelMap(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/user-view/U001");
        verify(adminUserService).update("U001", form);
        verify(accessChecker).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    // ── delete ────────────────────────────────────────────────────

    @Test
    void delete_ログイン中の自ユーザーを削除しようとした場合_エラーメッセージが設定され検索画面にリダイレクト() {
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

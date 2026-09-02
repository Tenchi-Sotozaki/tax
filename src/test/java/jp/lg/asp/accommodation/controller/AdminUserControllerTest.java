package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

/**
 * AdminUserController 単体テスト
 *
 * <p>ユーザー検索・ユーザー登録編集削除は対象クラスが同じため、1ファイルにまとめる
 * （MRは画面ごとに分割する。ACCOMMODATION_TAX 内で合意済み）。
 * チェックリストがどちらも #1 から始まるため、@DisplayName の番号には画面名を付けて区別する。</p>
 *
 * <ul>
 *   <li>#検索1〜#検索3 … 「ユーザー検索_単体テストチェックリスト.xlsx」</li>
 *   <li>#登録編集削除… … 「ユーザー登録編集削除_単体テストチェックリスト.xlsx」（別MRで追加）</li>
 * </ul>
 *
 * <p>チェックリストはあるべき仕様で書かれている。テストが通るように期待値を実装へ寄せないこと。</p>
 */
@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock AdminUserService adminUserService;
    @Mock ScreenAccessChecker accessChecker;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks AdminUserController controller;

    private static final String JICHITAI_CD = "01100";
    private static final String LIST_VIEW = "admin/userDaicho";
    private static final String FORM_VIEW = "admin/userConfig";
    private static final String USER_ID = "U001";

    @BeforeEach
    void setUp() {
        // delete は jichitaiCd を参照しないため lenient にする
        lenient().when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    private Role role(Long roleId, String name) {
        Role role = new Role();
        role.setJichitaiCd(JICHITAI_CD);
        role.setRoleId(roleId);
        role.setName(name);
        return role;
    }

    private User user(String id) {
        User user = new User();
        user.setJichitaiCd(JICHITAI_CD);
        user.setId(id);
        return user;
    }

    // ==================================================================
    // ユーザー検索：list
    // ==================================================================

    @Test
    @DisplayName("#検索1 list 正常系 searched=falseの場合：検索を実行せず空リストを返す")
    void list_searchedがfalseなら検索しない() {
        when(adminUserService.selectableRoles(JICHITAI_CD, null))
                .thenReturn(List.of(role(1L, "管理者")));

        Model model = new ExtendedModelMap();

        String view = controller.list(new UserSearchForm(), false, model);

        assertThat(view).isEqualTo(LIST_VIEW);
        assertThat(model.asMap()).containsEntry("items", List.of());
        assertThat(model.asMap()).containsEntry("searched", false);
        verify(adminUserService, never()).searchAll(any());
        verify(accessChecker).checkAccess(ScreenManagement.USER_MANAGEMENT);
    }

    @Test
    @DisplayName("#検索2 list 正常系 searched=trueの場合：searchAllを呼び結果をモデルに設定する")
    void list_searchedがtrueなら検索結果をモデルに設定する() {
        User user1 = user("U001");
        UserSearchForm form = new UserSearchForm();
        when(adminUserService.searchAll(form)).thenReturn(List.of(user1));
        when(adminUserService.selectableRoles(JICHITAI_CD, null))
                .thenReturn(List.of(role(1L, "管理者")));

        Model model = new ExtendedModelMap();

        String view = controller.list(form, true, model);

        assertThat(view).isEqualTo(LIST_VIEW);
        assertThat(model.asMap()).containsEntry("items", List.of(user1));
        assertThat(model.asMap()).containsEntry("searched", true);
        verify(adminUserService, times(1)).searchAll(form);
        verify(accessChecker).checkAccess(ScreenManagement.USER_MANAGEMENT);
    }

    @Test
    @DisplayName("#検索3 list 正常系 rolesとroleMapがモデルに設定される")
    void list_rolesとroleMapがモデルに設定される() {
        Role role1 = role(1L, "管理者");
        when(adminUserService.selectableRoles(JICHITAI_CD, null)).thenReturn(List.of(role1));

        Model model = new ExtendedModelMap();

        controller.list(new UserSearchForm(), false, model);

        assertThat(model.asMap()).containsEntry("roles", List.of(role1));
        // Role.roleId は Long のため roleMap のキーも Long になる
        assertThat(model.asMap().get("roleMap")).isEqualTo(java.util.Map.of(1L, "管理者"));
        verify(accessChecker).checkAccess(ScreenManagement.USER_MANAGEMENT);
    }

    // ==================================================================
    // ユーザー登録編集削除
    // ==================================================================

    /** 登録・更新用のフォームを生成する */
    private UserForm userForm(String id, String password, String passwordConfirm) {
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

    private BindingResult bindingResult(UserForm form) {
        return new BeanPropertyBindingResult(form, "userForm");
    }

    @Test
    @DisplayName("#登録編集削除1 showRegistrationForm 正常系 登録画面初期表示：userForm・roles・isEdit=false・isView=false・isDefaultUser=falseがモデルに設定される")
    void showRegistrationForm_登録画面初期表示() {
        Role role1 = role(1L, "管理者");
        when(adminUserService.selectableRoles(JICHITAI_CD, null)).thenReturn(List.of(role1));

        Model model = new ExtendedModelMap();

        String view = controller.showRegistrationForm(model);

        assertThat(view).isEqualTo(FORM_VIEW);
        assertThat(model.asMap().get("userForm")).isNotNull();
        assertThat(model.asMap()).containsEntry("roles", List.of(role1));
        assertThat(model.asMap()).containsEntry("isEdit", false);
        assertThat(model.asMap()).containsEntry("isView", false);
        assertThat(model.asMap()).containsEntry("isDefaultUser", false);
        verify(accessChecker, times(1)).checkWriteAccess(ScreenManagement.USER_CONFIG);
    }

    @Test
    @DisplayName("#登録編集削除2 register 正常系 正常登録：successMessageを積んでユーザー照会画面へリダイレクトする")
    void register_正常登録() {
        UserForm form = userForm(USER_ID, "pass", "pass");
        when(adminUserService.existsActiveUser(USER_ID)).thenReturn(false);
        lenient().when(adminUserService.selectableRoles(eq(JICHITAI_CD), any()))
                .thenReturn(List.of(role(1L, "管理者")));

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.register(form, bindingResult(form), new ExtendedModelMap(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/user-view/" + USER_ID);
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
                .isEqualTo("ユーザーを登録しました。");
        verify(adminUserService, times(1)).register(form);
        verify(accessChecker).checkWriteAccess(ScreenManagement.USER_CONFIG);
    }

    @Test
    @DisplayName("#登録編集削除3 register 異常系 パスワードと確認用パスワードが不一致：bindingResultにエラーが設定され登録画面に戻る")
    void register_パスワード不一致は登録画面に戻る() {
        UserForm form = userForm(USER_ID, "pass1", "pass2");
        when(adminUserService.selectableRoles(eq(JICHITAI_CD), any()))
                .thenReturn(List.of(role(1L, "管理者")));

        BindingResult bindingResult = bindingResult(form);

        String view = controller.register(form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo(FORM_VIEW);
        assertThat(bindingResult.getFieldError("passwordConfirm")).isNotNull();
        verify(adminUserService, never()).register(any());
        verify(accessChecker).checkWriteAccess(ScreenManagement.USER_CONFIG);
    }

    @Test
    @DisplayName("#登録編集削除4 register 異常系 既存アクティブユーザーと重複するIDで登録：bindingResult[\"id\"]にエラーが設定され登録画面に戻る")
    void register_ID重複は登録画面に戻る() {
        UserForm form = userForm(USER_ID, "pass", "pass");
        when(adminUserService.existsActiveUser(USER_ID)).thenReturn(true);
        when(adminUserService.selectableRoles(eq(JICHITAI_CD), any()))
                .thenReturn(List.of(role(1L, "管理者")));

        BindingResult bindingResult = bindingResult(form);

        String view = controller.register(form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo(FORM_VIEW);
        assertThat(bindingResult.getFieldError("id")).isNotNull();
        verify(adminUserService, never()).register(any());
        verify(accessChecker).checkWriteAccess(ScreenManagement.USER_CONFIG);
    }

    @Test
    @DisplayName("#登録編集削除5 showEditForm 正常系 編集画面初期表示：userForm・roles・isEdit=true・isView=false・isDefaultUser=falseがモデルに設定される")
    void showEditForm_編集画面初期表示() {
        Role role1 = role(1L, "管理者");
        when(adminUserService.findById(USER_ID)).thenReturn(user(USER_ID));
        when(adminUserService.selectableRoles(eq(JICHITAI_CD), any())).thenReturn(List.of(role1));

        Model model = new ExtendedModelMap();

        String view = controller.showEditForm(USER_ID, model);

        assertThat(view).isEqualTo(FORM_VIEW);
        assertThat(model.asMap().get("userForm")).isNotNull();
        assertThat(model.asMap()).containsEntry("roles", List.of(role1));
        assertThat(model.asMap()).containsEntry("isEdit", true);
        assertThat(model.asMap()).containsEntry("isView", false);
        assertThat(model.asMap()).containsEntry("isDefaultUser", false);
        verify(accessChecker).checkWriteAccess(ScreenManagement.USER_CONFIG);
    }

    @Test
    @DisplayName("#登録編集削除6 showEditForm 正常系 ADMIN_IDユーザーの編集画面：isDefaultUser=trueがモデルに設定される")
    void showEditForm_ADMIN_IDはisDefaultUserがtrue() {
        when(adminUserService.findById(InitialPasswordController.ADMIN_ID))
                .thenReturn(user(InitialPasswordController.ADMIN_ID));
        when(adminUserService.selectableRoles(eq(JICHITAI_CD), any()))
                .thenReturn(List.of(role(1L, "管理者")));

        Model model = new ExtendedModelMap();

        controller.showEditForm(InitialPasswordController.ADMIN_ID, model);

        assertThat(model.asMap()).containsEntry("isDefaultUser", true);
        verify(accessChecker).checkWriteAccess(ScreenManagement.USER_CONFIG);
    }

    /**
     * ※現行実装は isEdit=true を設定しているため、実装側の修正が必要
     */
    @Test
    @DisplayName("#登録編集削除7 showViewForm 正常系 照会画面表示：userForm・roles・isEdit=false・isView=true・isDefaultUser=false がモデルに設定される")
    void showViewForm_照会画面表示() {
        Role role1 = role(1L, "管理者");
        when(adminUserService.findById(USER_ID)).thenReturn(user(USER_ID));
        when(adminUserService.selectableRoles(eq(JICHITAI_CD), any())).thenReturn(List.of(role1));

        Model model = new ExtendedModelMap();

        String view = controller.showViewForm(USER_ID, model);

        assertThat(view).isEqualTo(FORM_VIEW);
        assertThat(model.asMap().get("userForm")).isNotNull();
        assertThat(model.asMap()).containsEntry("roles", List.of(role1));
        assertThat(model.asMap()).containsEntry("isEdit", false);
        assertThat(model.asMap()).containsEntry("isView", true);
        assertThat(model.asMap()).containsEntry("isDefaultUser", false);
        verify(accessChecker).checkAccess(ScreenManagement.USER_CONFIG);
    }

    @Test
    @DisplayName("#登録編集削除8 update 正常系 正常更新：successMessageを積んでユーザー照会画面へリダイレクトする")
    void update_正常更新() {
        UserForm form = userForm(USER_ID, "", "");
        form.setName("鈴木");
        when(adminUserService.findById(USER_ID)).thenReturn(user(USER_ID));

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.update(USER_ID, form, bindingResult(form), new ExtendedModelMap(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/user-view/" + USER_ID);
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
                .isEqualTo("ユーザー情報を更新しました。");
        verify(adminUserService, times(1)).update(USER_ID, form);
        verify(accessChecker).checkWriteAccess(ScreenManagement.USER_CONFIG);
    }

    @Test
    @DisplayName("#登録編集削除9 update 異常系 パスワードと確認用パスワードが不一致：bindingResultにエラーが設定され編集画面に戻る")
    void update_パスワード不一致は編集画面に戻る() {
        UserForm form = userForm(USER_ID, "pass1", "pass2");
        when(adminUserService.findById(USER_ID)).thenReturn(user(USER_ID));
        when(adminUserService.selectableRoles(eq(JICHITAI_CD), any()))
                .thenReturn(List.of(role(1L, "管理者")));

        BindingResult bindingResult = bindingResult(form);

        String view = controller.update(USER_ID, form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo(FORM_VIEW);
        assertThat(bindingResult.getFieldError("passwordConfirm")).isNotNull();
        verify(adminUserService, never()).update(any(), any());
        verify(accessChecker).checkWriteAccess(ScreenManagement.USER_CONFIG);
    }

    @Test
    @DisplayName("#登録編集削除10 update 正常系 パスワード欄が空のまま更新：パスワードチェックをスキップして正常に更新処理へ流れる")
    void update_パスワード欄が空でも更新される() {
        // form.password は null の場合も同様（実装は null と空文字の両方をスキップ対象にしている）
        UserForm form = userForm(USER_ID, "", "");
        when(adminUserService.findById(USER_ID)).thenReturn(user(USER_ID));

        String view = controller.update(USER_ID, form, bindingResult(form), new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/admin/user-view/" + USER_ID);
        verify(adminUserService, times(1)).update(USER_ID, form);
        verify(accessChecker).checkWriteAccess(ScreenManagement.USER_CONFIG);
    }

    @Test
    @DisplayName("#登録編集削除11 delete 正常系 正常削除：successMessageを積んで検索画面へリダイレクトする")
    void delete_正常削除() {
        when(adminUserService.isLoginUser(USER_ID)).thenReturn(false);

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.delete(USER_ID, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/user-search");
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
                .isEqualTo("ユーザーを削除しました。");
        verify(adminUserService, times(1)).delete(USER_ID);
        verify(accessChecker).checkWriteAccess(ScreenManagement.USER_CONFIG);
    }

    @Test
    @DisplayName("#登録編集削除12 delete 異常系 ログイン中の自ユーザーを削除しようとした場合：errorMessageを積んで検索画面へリダイレクトする")
    void delete_ログイン中ユーザーは削除できない() {
        when(adminUserService.isLoginUser(USER_ID)).thenReturn(true);

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.delete(USER_ID, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/user-search");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("ログイン中のユーザーは削除できません。");
        verify(adminUserService, never()).delete(any());
        verify(accessChecker).checkWriteAccess(ScreenManagement.USER_CONFIG);
    }
}

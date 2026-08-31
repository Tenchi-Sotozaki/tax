package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
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

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
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
}

package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.RoleForm;
import jp.lg.asp.accommodation.entity.Role;
import jp.lg.asp.accommodation.entity.RoleDetail;
import jp.lg.asp.accommodation.entity.Screen;
import jp.lg.asp.accommodation.entity.User;
import jp.lg.asp.accommodation.repository.UserRepository;
import jp.lg.asp.accommodation.service.RoleService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoleControllerTest {

    @Mock RoleService roleService;
    @Mock ScreenAccessChecker accessChecker;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks RoleController controller;

    private static final String JICHITAI_CD = "011002";
    private static final String SCREEN_ID = ScreenManagement.ROLE_MANAGEMENT;

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

    private User user(String id, String name) {
        User user = new User();
        user.setJichitaiCd(JICHITAI_CD);
        user.setId(id);
        user.setName(name);
        return user;
    }

    // ==================================================================
    // roleManagement
    // ==================================================================

    @Test
    @DisplayName("#1 roleManagement 正常系 取得したRoleにDEFAULT_USER_ROLE_IDあり：rolesに1件のみ設定されること")
    void roleManagement_DEFAULT_USER_ROLE_IDを含む場合はフィルタされる() {
        Role defaultRole = role((Long) UserRepository.DEFAULT_USER_ROLE_ID, "デフォルト");
        Role normalRole = role(1L, "一般");
        Map<String, List<Screen>> screenGroups = Map.of("区分A", List.of());
        when(roleService.findAllRoles(JICHITAI_CD)).thenReturn(List.of(defaultRole, normalRole));
        when(roleService.findScreensGroupedByKbn()).thenReturn(screenGroups);

        Model model = new ExtendedModelMap();
        String view = controller.roleManagement(model);

        assertThat(view).isEqualTo("admin/roleManagement");
        assertThat((List<Role>) model.asMap().get("roles")).containsExactly(normalRole);
        assertThat(model.asMap()).containsKey("screenGroups");
        assertThat(model.asMap()).containsEntry("defaultRoleId", (long) UserRepository.DEFAULT_USER_ROLE_ID);
        verify(accessChecker).checkAccess(SCREEN_ID);
    }

    @Test
    @DisplayName("#2 roleManagement 正常系 取得したRoleが全てDEFAULT_USER_ROLE_ID以外：rolesに全件設定されること")
    void roleManagement_DEFAULT_USER_ROLE_IDなしの場合は全件設定される() {
        Role role1 = role(1L, "一般");
        Role role2 = role(2L, "管理者");
        when(roleService.findAllRoles(JICHITAI_CD)).thenReturn(List.of(role1, role2));
        when(roleService.findScreensGroupedByKbn()).thenReturn(Collections.emptyMap());

        Model model = new ExtendedModelMap();
        controller.roleManagement(model);

        assertThat((List<Role>) model.asMap().get("roles")).containsExactly(role1, role2);
        verify(accessChecker).checkAccess(SCREEN_ID);
    }

    @Test
    @DisplayName("#3 roleManagement 異常系 roleService.findAllRolesが例外をスロー：rolesが空リスト・screenGroupsが空マップでmodelに設定されること")
    void roleManagement_例外発生時は空リストと空マップが設定される() {
        when(roleService.findAllRoles(JICHITAI_CD)).thenThrow(new RuntimeException("DB error"));

        Model model = new ExtendedModelMap();
        String view = controller.roleManagement(model);

        assertThat(view).isEqualTo("admin/roleManagement");
        assertThat(model.asMap().get("roles")).isEqualTo(Collections.emptyList());
        assertThat(model.asMap().get("screenGroups")).isEqualTo(Collections.emptyMap());
        verify(accessChecker).checkAccess(SCREEN_ID);
    }

    // ==================================================================
    // saveRole
    // ==================================================================

    @Test
    @DisplayName("#4 saveRole 正常系 権限名あり・新規登録：success:trueが設定されること、roleService.saveRoleが1回呼ばれること")
    void saveRole_正常登録() {
        RoleForm form = new RoleForm();
        form.setName("一般ユーザー");

        Map<String, Object> result = controller.saveRole(form);

        assertThat(result).containsEntry("success", true);
        verify(roleService, times(1)).saveRole(eq(form), eq(JICHITAI_CD), eq("admin"));
        verify(accessChecker).checkWriteAccess(SCREEN_ID);
    }

    @Test
    @DisplayName("#5 saveRole 異常系 デフォルト権限IDを指定：success:falseとmessage「この権限は編集できません」が設定されること")
    void saveRole_デフォルト権限IDは編集不可() {
        RoleForm form = new RoleForm();
        form.setRoleId((Long) UserRepository.DEFAULT_USER_ROLE_ID);
        form.setName("管理者");

        Map<String, Object> result = controller.saveRole(form);

        assertThat(result).containsEntry("success", false);
        assertThat(result).containsEntry("message", "この権限は編集できません");
        verify(accessChecker).checkWriteAccess(SCREEN_ID);
    }

    @Test
    @DisplayName("#6 saveRole 異常系 権限名がnull：success:falseとerrorsに「権限名は必須です」が含まれること")
    void saveRole_権限名がnullはエラー() {
        RoleForm form = new RoleForm();
        form.setName(null);

        Map<String, Object> result = controller.saveRole(form);

        assertThat(result).containsEntry("success", false);
        assertThat((List<String>) result.get("errors")).contains("権限名は必須です");
        verify(accessChecker).checkWriteAccess(SCREEN_ID);
    }

    @Test
    @DisplayName("#7 saveRole 異常系 権限名が空白のみ：success:falseとerrorsに「権限名は必須です」が含まれること")
    void saveRole_権限名が空白のみはエラー() {
        RoleForm form = new RoleForm();
        form.setName("   ");

        Map<String, Object> result = controller.saveRole(form);

        assertThat(result).containsEntry("success", false);
        assertThat((List<String>) result.get("errors")).contains("権限名は必須です");
        verify(accessChecker).checkWriteAccess(SCREEN_ID);
    }

    @Test
    @DisplayName("#8 saveRole 異常系 サービスが例外をスロー：success:falseと例外メッセージがmessageに設定されること")
    void saveRole_サービス例外時はエラーメッセージが設定される() {
        RoleForm form = new RoleForm();
        form.setName("一般ユーザー");
        doThrow(new RuntimeException("保存失敗")).when(roleService).saveRole(any(), any(), any());

        Map<String, Object> result = controller.saveRole(form);

        assertThat(result).containsEntry("success", false);
        assertThat(result).containsEntry("message", "保存失敗");
        verify(accessChecker).checkWriteAccess(SCREEN_ID);
    }

    @Test
    @DisplayName("#9 saveRole 正常系 roleIdがnullかつscreenPermissionsにvalue=0以外が含まれる：success:trueが設定されること、roleService.saveRoleが1回呼ばれること")
    void saveRole_roleIdがnullかつscreenPermissionsにvalue0以外が含まれる場合は正常登録() {
        RoleForm form = new RoleForm();
        form.setName("一般ユーザー");
        form.setScreenPermissions(Map.of("sc001", 1, "sc002", 2));

        Map<String, Object> result = controller.saveRole(form);

        assertThat(result).containsEntry("success", true);
        verify(roleService, times(1)).saveRole(eq(form), eq(JICHITAI_CD), eq("admin"));
        verify(accessChecker).checkWriteAccess(SCREEN_ID);
    }

    // ==================================================================
    // getRoleDetail
    // ==================================================================

    @Test
    @DisplayName("#10 getRoleDetail 正常系 存在する権限IDを指定：戻り値にrole・permissions・editableが設定されること")
    void getRoleDetail_存在する権限IDはroleとpermissionsとeditableが返る() {
        RoleDetail detail = new RoleDetail();
        detail.setScreenId("sc00000001");
        detail.setPermission("1");

        Role role = role(3L, "一般");
        role.setRoleDetails(List.of(detail));
        when(roleService.findById(JICHITAI_CD, 3L)).thenReturn(role);

        Map<String, Object> result = controller.getRoleDetail(3L);

        assertThat(result).containsKey("role");
        assertThat(result).containsKey("permissions");
        assertThat(result).containsEntry("editable", true);
        verify(accessChecker).checkAccess(SCREEN_ID);
    }

    @Test
    @DisplayName("#11 getRoleDetail 正常系 デフォルト権限IDを指定：戻り値にeditable:falseが設定されること")
    void getRoleDetail_デフォルト権限IDはeditableがfalse() {
        long defaultId = UserRepository.DEFAULT_USER_ROLE_ID;
        Role role = role(defaultId, "デフォルト");
        role.setRoleDetails(Collections.emptyList());
        when(roleService.findById(JICHITAI_CD, defaultId)).thenReturn(role);

        Map<String, Object> result = controller.getRoleDetail(defaultId);

        assertThat(result).containsEntry("editable", false);
        verify(accessChecker).checkAccess(SCREEN_ID);
    }

    @Test
    @DisplayName("#12 getRoleDetail 異常系 存在しない権限IDを指定：戻り値に空のマップが設定されること")
    void getRoleDetail_存在しない権限IDは空マップが返る() {
        when(roleService.findById(JICHITAI_CD, 99L)).thenReturn(null);

        Map<String, Object> result = controller.getRoleDetail(99L);

        assertThat(result).isEmpty();
        verify(accessChecker).checkAccess(SCREEN_ID);
    }

    // ==================================================================
    // getAssignedUsers
    // ==================================================================

    @Test
    @DisplayName("#13 getAssignedUsers 正常系 付与ユーザーが存在：roleNameとusers（各id・name含む）が返ること")
    void getAssignedUsers_付与ユーザーが存在する場合はroleNameとusersが返る() {
        Role role = role(3L, "一般");
        User u1 = user("U001", "山田太郎");
        when(roleService.findById(JICHITAI_CD, 3L)).thenReturn(role);
        when(roleService.findAssignedUsers(JICHITAI_CD, 3L)).thenReturn(List.of(u1));

        Map<String, Object> result = controller.getAssignedUsers(3L);

        assertThat(result).containsEntry("roleName", "一般");
        List<?> users = (List<?>) result.get("users");
        assertThat(users).hasSize(1);
        Map<String, Object> userMap = (Map<String, Object>) users.get(0);
        assertThat(userMap).containsEntry("id", "U001");
        assertThat(userMap).containsEntry("name", "山田太郎");
        verify(accessChecker).checkAccess(SCREEN_ID);
    }

    @Test
    @DisplayName("#14 getAssignedUsers 正常系 付与ユーザーが0件：roleNameが設定されること、usersが空リストで返ること")
    void getAssignedUsers_付与ユーザーが0件の場合はusersが空リスト() {
        Role role = role(3L, "一般");
        when(roleService.findById(JICHITAI_CD, 3L)).thenReturn(role);
        when(roleService.findAssignedUsers(JICHITAI_CD, 3L)).thenReturn(Collections.emptyList());

        Map<String, Object> result = controller.getAssignedUsers(3L);

        assertThat(result).containsEntry("roleName", "一般");
        assertThat((List<?>) result.get("users")).isEmpty();
        verify(accessChecker).checkAccess(SCREEN_ID);
    }

    @Test
    @DisplayName("#15 getAssignedUsers 正常系 roleが存在しない：roleNameが空文字で返ること")
    void getAssignedUsers_roleが存在しない場合はroleNameが空文字() {
        when(roleService.findById(JICHITAI_CD, 99L)).thenReturn(null);
        when(roleService.findAssignedUsers(JICHITAI_CD, 99L)).thenReturn(Collections.emptyList());

        Map<String, Object> result = controller.getAssignedUsers(99L);

        assertThat(result).containsEntry("roleName", "");
        verify(accessChecker).checkAccess(SCREEN_ID);
    }

    // ==================================================================
    // deleteRole
    // ==================================================================

    @Test
    @DisplayName("#16 deleteRole 正常系 付与ユーザーなし・削除成功の場合：roleService.deleteRoleが1回呼ばれること、success:trueが返ること")
    void deleteRole_正常削除() {
        when(roleService.findAssignedUsers(JICHITAI_CD, 3L)).thenReturn(Collections.emptyList());

        Map<String, Object> result = controller.deleteRole(3L);

        assertThat(result).containsEntry("success", true);
        verify(roleService, times(1)).deleteRole(JICHITAI_CD, 3L);
    }

    @Test
    @DisplayName("#17 deleteRole 異常系 roleIdがDEFAULT_USER_ROLE_ID：roleService.deleteRoleが呼ばれないこと、success:falseとmessage「デフォルト権限のため削除できません」が返ること")
    void deleteRole_デフォルト権限IDは削除不可() {
        Map<String, Object> result = controller.deleteRole((long) UserRepository.DEFAULT_USER_ROLE_ID);

        assertThat(result).containsEntry("success", false);
        assertThat(result).containsEntry("message", "デフォルト権限のため削除できません");
        verify(roleService, never()).deleteRole(anyString(), anyLong());
    }

    @Test
    @DisplayName("#18 deleteRole 異常系 付与ユーザーが1件以上存在：roleService.deleteRoleが呼ばれないこと、success:falseとmessageに「ユーザーがいるため削除できません」が含まれること")
    void deleteRole_付与ユーザーがいる場合は削除不可() {
        when(roleService.findAssignedUsers(JICHITAI_CD, 3L)).thenReturn(List.of(new User()));

        Map<String, Object> result = controller.deleteRole(3L);

        assertThat(result).containsEntry("success", false);
        assertThat((String) result.get("message")).contains("ユーザーがいるため削除できません");
        verify(roleService, never()).deleteRole(anyString(), anyLong());
    }

    @Test
    @DisplayName("#19 deleteRole 異常系 サービスが例外をスロー：success:falseと例外メッセージがmessageに設定されること")
    void deleteRole_サービス例外時はエラーメッセージが設定される() {
        when(roleService.findAssignedUsers(JICHITAI_CD, 3L)).thenReturn(Collections.emptyList());
        doThrow(new RuntimeException("削除失敗")).when(roleService).deleteRole(JICHITAI_CD, 3L);

        Map<String, Object> result = controller.deleteRole(3L);

        assertThat(result).containsEntry("success", false);
        assertThat(result).containsEntry("message", "削除失敗");
    }
}

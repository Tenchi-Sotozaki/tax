package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
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
import jp.lg.asp.accommodation.dto.RoleForm;
import jp.lg.asp.accommodation.entity.Role;
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

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("011002");
    }

    @Test
    void roleManagement_管理画面を返す() {
        when(roleService.findAllRoles("011002")).thenReturn(List.of());
        when(roleService.findAllScreens()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.roleManagement(model);

        assertThat(view).isEqualTo("admin/roleManagement");
    }

    @Test
    void saveRole_デフォルト権限は編集不可() {
        RoleForm form = new RoleForm();
        form.setRoleId(UserRepository.DEFAULT_USER_ROLE_ID);
        form.setName("管理者");

        Map<String, Object> result = controller.saveRole(form);

        assertThat(result).containsEntry("success", false);
    }

    @Test
    void saveRole_名前空はエラー() {
        RoleForm form = new RoleForm();
        form.setName("");

        Map<String, Object> result = controller.saveRole(form);

        assertThat(result).containsEntry("success", false);
    }

    @Test
    void saveRole_正常保存() {
        RoleForm form = new RoleForm();
        form.setName("一般ユーザー");

        Map<String, Object> result = controller.saveRole(form);

        assertThat(result).containsEntry("success", true);
        verify(roleService).saveRole(eq(form), eq("011002"), eq("admin"));
    }

    @Test
    void getRoleDetail_存在しない場合は空マップ() {
        when(roleService.findById("011002", 99L)).thenReturn(null);

        Map<String, Object> result = controller.getRoleDetail(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void getRoleDetail_存在する場合はロール情報を返す() {
        Role role = new Role();
        role.setRoleId(3L);
        role.setName("一般");
        when(roleService.findById("011002", 3L)).thenReturn(role);

        Map<String, Object> result = controller.getRoleDetail(3L);

        assertThat(result).containsKey("role");
        assertThat(result).containsEntry("editable", true);
    }

    @Test
    void getAssignedUsers_デフォルト権限でも照会できる() {
        Role role = new Role();
        role.setRoleId(UserRepository.DEFAULT_USER_ROLE_ID);
        role.setName("デフォルト権限");
        when(roleService.findById("011002", UserRepository.DEFAULT_USER_ROLE_ID)).thenReturn(role);
        when(roleService.findAssignedUsers("011002", UserRepository.DEFAULT_USER_ROLE_ID))
                .thenReturn(List.of());

        Map<String, Object> result = controller.getAssignedUsers(UserRepository.DEFAULT_USER_ROLE_ID);

        assertThat(result).doesNotContainKey("error");
        assertThat(result).containsEntry("roleName", "デフォルト権限");
    }

    @Test
    void deleteRole_デフォルト権限は削除不可() {
        Map<String, Object> result = controller.deleteRole(UserRepository.DEFAULT_USER_ROLE_ID);

        assertThat(result).containsEntry("success", false);
        verify(roleService, never()).deleteRole(anyString(), anyLong());
    }

    @Test
    void deleteRole_付与ユーザーがいる場合は削除不可() {
        when(roleService.findAssignedUsers("011002", 3L)).thenReturn(List.of(new User()));

        Map<String, Object> result = controller.deleteRole(3L);

        assertThat(result).containsEntry("success", false);
        verify(roleService, never()).deleteRole(anyString(), anyLong());
    }

    @Test
    void deleteRole_正常削除() {
        when(roleService.findAssignedUsers("011002", 3L)).thenReturn(List.of());

        Map<String, Object> result = controller.deleteRole(3L);

        assertThat(result).containsEntry("success", true);
        verify(roleService).deleteRole("011002", 3L);
    }
}

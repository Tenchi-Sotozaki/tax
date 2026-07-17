package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.RoleForm;
import jp.lg.asp.accommodation.entity.Role;
import jp.lg.asp.accommodation.entity.RoleDetail;
import jp.lg.asp.accommodation.entity.RoleId;
import jp.lg.asp.accommodation.entity.Screen;
import jp.lg.asp.accommodation.entity.User;
import jp.lg.asp.accommodation.entity.UserId;
import jp.lg.asp.accommodation.repository.RoleRepository;
import jp.lg.asp.accommodation.repository.ScreenRepository;
import jp.lg.asp.accommodation.repository.UserRepository;
import jp.lg.asp.accommodation.service.impl.RoleServiceImpl;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class RoleServiceImplTest {

    @Mock RoleRepository roleRepository;
    @Mock ScreenRepository screenRepository;
    @Mock UserRepository userRepository;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks RoleServiceImpl service;

    private static final String JICHITAI_CD = "011002";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    @Test
    void findAllRoles_delegatesToRepository() {
        Role role = new Role();
        when(roleRepository.findByJichitaiCdWithDetails(JICHITAI_CD)).thenReturn(List.of(role));

        assertThat(service.findAllRoles(JICHITAI_CD)).hasSize(1);
    }

    @Test
    void findAllScreens_delegatesToRepository() {
        Screen screen = new Screen();
        when(screenRepository.findByJichitaiCdOrderByScreenId(JICHITAI_CD)).thenReturn(List.of(screen));

        assertThat(service.findAllScreens()).hasSize(1);
    }

    @Test
    void findById_found() {
        Role role = new Role();
        when(roleRepository.findByIdWithDetails(JICHITAI_CD, 1L)).thenReturn(Optional.of(role));

        assertThat(service.findById(JICHITAI_CD, 1L)).isNotNull();
    }

    @Test
    void findById_notFound_returnsNull() {
        when(roleRepository.findByIdWithDetails(JICHITAI_CD, 99L)).thenReturn(Optional.empty());

        assertThat(service.findById(JICHITAI_CD, 99L)).isNull();
    }

    @Test
    void saveRole_newRole_assignsNextId() {
        RoleForm form = new RoleForm();
        form.setName("テストロール");
        form.setScreenPermissions(Map.of("SCR001", 1));

        when(roleRepository.findMaxRoleIdByJichitaiCd(JICHITAI_CD)).thenReturn(5L);
        when(roleRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.saveRole(form, JICHITAI_CD, "user01");

        verify(roleRepository).saveAndFlush(argThat(r -> r.getRoleId() == 6L));
    }

    @Test
    void saveRole_existingRole_updatesName() {
        RoleForm form = new RoleForm();
        form.setRoleId(1L);
        form.setName("更新ロール");
        form.setScreenPermissions(Map.of("SCR001", 1));

        Role existing = new Role();
        existing.setRoleId(1L);
        existing.setRoleDetails(new ArrayList<>());
        when(roleRepository.findByIdWithDetails(JICHITAI_CD, 1L)).thenReturn(Optional.of(existing));
        when(roleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.saveRole(form, JICHITAI_CD, "user01");

        assertThat(existing.getName()).isEqualTo("更新ロール");
    }

    @Test
    void saveRole_zeroPermission_notAdded() {
        RoleForm form = new RoleForm();
        form.setRoleId(1L);
        form.setName("ロール");
        form.setScreenPermissions(Map.of("SCR001", 0));

        Role existing = new Role();
        existing.setRoleId(1L);
        existing.setRoleDetails(new ArrayList<>());
        when(roleRepository.findByIdWithDetails(JICHITAI_CD, 1L)).thenReturn(Optional.of(existing));
        when(roleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.saveRole(form, JICHITAI_CD, "user01");

        assertThat(existing.getRoleDetails()).isEmpty();
    }

    @Test
    void updateUserRole_resetsOldUsersAndAssignsNew() {
        User oldUser = new User();
        oldUser.setRoleId(BigDecimal.valueOf(1));
        when(userRepository.findByJichitaiCdAndRoleId(JICHITAI_CD, BigDecimal.valueOf(1L)))
                .thenReturn(List.of(oldUser));

        UserId uid = new UserId();
        uid.setJichitaiCd(JICHITAI_CD);
        uid.setId("user02");
        User newUser = new User();
        when(userRepository.findById(any())).thenReturn(Optional.of(newUser));
        when(userRepository.saveAll(any())).thenReturn(List.of());
        when(userRepository.save(any())).thenReturn(newUser);

        service.updateUserRole(JICHITAI_CD, 1L, List.of("user02"), "admin");

        assertThat(oldUser.getRoleId()).isEqualTo(BigDecimal.ZERO);
        assertThat(newUser.getRoleId()).isEqualTo(BigDecimal.valueOf(1L));
    }

    @Test
    void resetUsersToDefaultRole_setsRoleIdToTwo() {
        User user = new User();
        when(userRepository.findByJichitaiCdAndRoleId(JICHITAI_CD, BigDecimal.valueOf(1L)))
                .thenReturn(List.of(user));
        when(userRepository.saveAll(any())).thenReturn(List.of());

        service.resetUsersToDefaultRole(JICHITAI_CD, 1L, "admin");

        assertThat(user.getRoleId()).isEqualTo(BigDecimal.TWO);
    }

    @Test
    void deleteRole_callsRepositoryDeleteById() {
        service.deleteRole(JICHITAI_CD, 1L);

        verify(roleRepository).deleteById(new RoleId(JICHITAI_CD, 1L));
    }
}

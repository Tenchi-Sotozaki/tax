package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.RoleForm;
import jp.lg.asp.accommodation.entity.Role;
import jp.lg.asp.accommodation.entity.RoleDetail;
import jp.lg.asp.accommodation.entity.RoleId;
import jp.lg.asp.accommodation.entity.Screen;
import jp.lg.asp.accommodation.entity.User;
import jp.lg.asp.accommodation.repository.RoleRepository;
import jp.lg.asp.accommodation.repository.ScreenRepository;
import jp.lg.asp.accommodation.repository.UserRepository;
import jp.lg.asp.accommodation.service.impl.RoleServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

    private Screen screen(String screenId, String kbn, int dspOdr) {
        Screen s = new Screen();
        s.setScreenId(screenId);
        s.setKbn(kbn);
        s.setDspOdr(dspOdr);
        return s;
    }

    private Role role(Long roleId) {
        Role r = new Role();
        r.setJichitaiCd(JICHITAI_CD);
        r.setRoleId(roleId);
        r.setRoleDetails(new ArrayList<>());
        return r;
    }

    // ==================================================================
    // findAllRoles
    // ==================================================================

    @Test
    @DisplayName("#1 findAllRoles 正常系 Roleが複数件存在する：findByJichitaiCdWithDetailsが呼ばれ結果がそのまま返ること")
    void findAllRoles_複数件存在する場合は結果がそのまま返る() {
        Role r1 = role(1L);
        Role r2 = role(2L);
        when(roleRepository.findByJichitaiCdWithDetails(JICHITAI_CD)).thenReturn(List.of(r1, r2));

        List<Role> result = service.findAllRoles(JICHITAI_CD);

        verify(roleRepository).findByJichitaiCdWithDetails(JICHITAI_CD);
        assertThat(result).containsExactly(r1, r2);
    }

    @Test
    @DisplayName("#2 findAllRoles 正常系 Roleが0件：空リストが返ること")
    void findAllRoles_0件の場合は空リストが返る() {
        when(roleRepository.findByJichitaiCdWithDetails(JICHITAI_CD)).thenReturn(List.of());

        assertThat(service.findAllRoles(JICHITAI_CD)).isEmpty();
    }

    // ==================================================================
    // findAllScreens
    // ==================================================================

    @Test
    @DisplayName("#3 findAllScreens 正常系 Screenが複数件存在する：findAllByOrderByScreenIdAscが呼ばれ結果が返ること")
    void findAllScreens_複数件存在する場合は結果が返る() {
        Screen s1 = screen("sc001", "区分A", 1);
        Screen s2 = screen("sc002", "区分B", 2);
        when(screenRepository.findAllByOrderByScreenIdAsc()).thenReturn(List.of(s1, s2));

        List<Screen> result = service.findAllScreens();

        verify(screenRepository).findAllByOrderByScreenIdAsc();
        assertThat(result).containsExactly(s1, s2);
    }

    @Test
    @DisplayName("#4 findAllScreens 正常系 Screenが0件：空リストが返ること")
    void findAllScreens_0件の場合は空リストが返る() {
        when(screenRepository.findAllByOrderByScreenIdAsc()).thenReturn(List.of());

        assertThat(service.findAllScreens()).isEmpty();
    }

    // ==================================================================
    // findScreensGroupedByKbn
    // ==================================================================

    @Test
    @DisplayName("#5 findScreensGroupedByKbn 正常系 複数区分の画面が存在する：区分ごとにグループ化されたマップが返ること、表示順が維持されること")
    void findScreensGroupedByKbn_複数区分が区分ごとにグループ化される() {
        Screen s1 = screen("sc001", "区分A", 1);
        Screen s2 = screen("sc002", "区分A", 2);
        Screen s3 = screen("sc003", "区分B", 3);
        when(screenRepository.findAllByOrderByDspOdrAsc()).thenReturn(List.of(s1, s2, s3));

        Map<String, List<Screen>> result = service.findScreensGroupedByKbn();

        assertThat(result).containsKey("区分A");
        assertThat(result).containsKey("区分B");
        assertThat(result.get("区分A")).containsExactly(s1, s2);
        assertThat(result.get("区分B")).containsExactly(s3);
        // LinkedHashMapなので挿入順（表示順）が維持される
        assertThat(result.keySet()).containsExactly("区分A", "区分B");
    }

    @Test
    @DisplayName("#6 findScreensGroupedByKbn 正常系 Screenが0件の場合：空マップが返ること")
    void findScreensGroupedByKbn_0件の場合は空マップが返る() {
        when(screenRepository.findAllByOrderByDspOdrAsc()).thenReturn(List.of());

        assertThat(service.findScreensGroupedByKbn()).isEmpty();
    }

    // ==================================================================
    // findById
    // ==================================================================

    @Test
    @DisplayName("#7 findById 正常系 存在する権限IDを指定：findByIdWithDetailsが呼ばれ該当のRoleが返ること")
    void findById_存在する権限IDは該当Roleが返る() {
        Role r = role(1L);
        when(roleRepository.findByIdWithDetails(JICHITAI_CD, 1L)).thenReturn(Optional.of(r));

        Role result = service.findById(JICHITAI_CD, 1L);

        verify(roleRepository).findByIdWithDetails(JICHITAI_CD, 1L);
        assertThat(result).isSameAs(r);
    }

    @Test
    @DisplayName("#8 findById 異常系 存在しない権限IDを指定：nullが返ること")
    void findById_存在しない権限IDはnullが返る() {
        when(roleRepository.findByIdWithDetails(JICHITAI_CD, 99L)).thenReturn(Optional.empty());

        assertThat(service.findById(JICHITAI_CD, 99L)).isNull();
    }

    // ==================================================================
    // saveRole
    // ==================================================================

    @Test
    @DisplayName("#9 saveRole 正常系 roleIdがnull（新規登録）：saveAndFlushとsaveが呼ばれること、RoleIdが採番されること")
    void saveRole_新規登録でRoleIdが採番される() {
        RoleForm form = new RoleForm();
        form.setName("新規ロール");
        when(roleRepository.findMaxRoleIdByJichitaiCd(JICHITAI_CD)).thenReturn(5L);
        when(roleRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.saveRole(form, JICHITAI_CD, "admin");

        verify(roleRepository).saveAndFlush(argThat(r -> r.getRoleId() == 6L));
        verify(roleRepository).save(any());
    }

    @Test
    @DisplayName("#10 saveRole 正常系 roleIdあり（更新）：既存Roleの名前が更新されること、RoleDetailがクリアされ新しい内容で保存されること")
    void saveRole_更新時に名前が更新されRoleDetailが再設定される() {
        RoleDetail existingDetail = new RoleDetail();
        existingDetail.setScreenId("sc001");
        existingDetail.setPermission("1");

        Role existing = role(1L);
        existing.getRoleDetails().add(existingDetail);
        when(roleRepository.findByIdWithDetails(JICHITAI_CD, 1L)).thenReturn(Optional.of(existing));
        when(roleRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RoleForm form = new RoleForm();
        form.setRoleId(1L);
        form.setName("更新ロール");
        form.setScreenPermissions(Map.of("sc002", 2));

        service.saveRole(form, JICHITAI_CD, "admin");

        assertThat(existing.getName()).isEqualTo("更新ロール");
        verify(roleRepository).saveAndFlush(existing);
        verify(roleRepository).save(existing);
        assertThat(existing.getRoleDetails()).hasSize(1);
        assertThat(existing.getRoleDetails().get(0).getScreenId()).isEqualTo("sc002");
    }

    @Test
    @DisplayName("#11 saveRole 正常系 screenPermissionsにvalue=0以外が含まれる：RoleDetailに追加されること")
    void saveRole_value0以外のエントリはRoleDetailに追加される() {
        Role existing = role(1L);
        when(roleRepository.findByIdWithDetails(JICHITAI_CD, 1L)).thenReturn(Optional.of(existing));
        when(roleRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RoleForm form = new RoleForm();
        form.setRoleId(1L);
        form.setName("ロール");
        form.setScreenPermissions(Map.of("sc001", 1, "sc002", 2));

        service.saveRole(form, JICHITAI_CD, "admin");

        assertThat(existing.getRoleDetails()).hasSize(2);
    }

    @Test
    @DisplayName("#12 saveRole 正常系 screenPermissionsにvalue=0が含まれる：value=0のエントリはRoleDetailに追加されないこと")
    void saveRole_value0のエントリはRoleDetailに追加されない() {
        Role existing = role(1L);
        when(roleRepository.findByIdWithDetails(JICHITAI_CD, 1L)).thenReturn(Optional.of(existing));
        when(roleRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RoleForm form = new RoleForm();
        form.setRoleId(1L);
        form.setName("ロール");
        form.setScreenPermissions(Map.of("sc001", 0, "sc002", 1));

        service.saveRole(form, JICHITAI_CD, "admin");

        assertThat(existing.getRoleDetails()).hasSize(1);
        assertThat(existing.getRoleDetails().get(0).getScreenId()).isEqualTo("sc002");
    }

    @Test
    @DisplayName("#13 saveRole 正常系 screenPermissionsがnull：RoleDetailが空のまま保存されること")
    void saveRole_screenPermissionsがnullの場合はRoleDetailが空のまま保存される() {
        Role existing = role(1L);
        when(roleRepository.findByIdWithDetails(JICHITAI_CD, 1L)).thenReturn(Optional.of(existing));
        when(roleRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RoleForm form = new RoleForm();
        form.setRoleId(1L);
        form.setName("ロール");
        form.setScreenPermissions(null);

        service.saveRole(form, JICHITAI_CD, "admin");

        assertThat(existing.getRoleDetails()).isEmpty();
        verify(roleRepository).save(existing);
    }

    @Test
    @DisplayName("#14 saveRole 異常系 更新時に対象Roleが存在しない：NoSuchElementExceptionがスローされること")
    void saveRole_更新対象が存在しない場合はNoSuchElementExceptionがスローされる() {
        when(roleRepository.findByIdWithDetails(JICHITAI_CD, 99L)).thenReturn(Optional.empty());

        RoleForm form = new RoleForm();
        form.setRoleId(99L);
        form.setName("存在しないロール");

        assertThatThrownBy(() -> service.saveRole(form, JICHITAI_CD, "admin"))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ==================================================================
    // findAssignedUsers
    // ==================================================================

    @Test
    @DisplayName("#15 findAssignedUsers 正常系 付与ユーザーが複数：findAssignedUsersが呼ばれユーザーリストが返ること")
    void findAssignedUsers_複数ユーザーが返る() {
        User u1 = new User();
        User u2 = new User();
        when(userRepository.findAssignedUsers(JICHITAI_CD, BigDecimal.valueOf(1L)))
                .thenReturn(List.of(u1, u2));

        List<User> result = service.findAssignedUsers(JICHITAI_CD, 1L);

        verify(userRepository).findAssignedUsers(JICHITAI_CD, BigDecimal.valueOf(1L));
        assertThat(result).containsExactly(u1, u2);
    }

    @Test
    @DisplayName("#16 findAssignedUsers 正常系 付与ユーザーが0件：空リストが返ること")
    void findAssignedUsers_0件の場合は空リストが返る() {
        when(userRepository.findAssignedUsers(JICHITAI_CD, BigDecimal.valueOf(1L)))
                .thenReturn(List.of());

        assertThat(service.findAssignedUsers(JICHITAI_CD, 1L)).isEmpty();
    }

    // ==================================================================
    // deleteRole
    // ==================================================================

    @Test
    @DisplayName("#17 deleteRole 正常系 正常に削除：roleRepository.deleteByIdがRoleId(jichitaiCd, roleId)で1回呼ばれること")
    void deleteRole_deleteByIdがRoleIdで1回呼ばれる() {
        service.deleteRole(JICHITAI_CD, 1L);

        verify(roleRepository, times(1)).deleteById(new RoleId(JICHITAI_CD, 1L));
    }
}

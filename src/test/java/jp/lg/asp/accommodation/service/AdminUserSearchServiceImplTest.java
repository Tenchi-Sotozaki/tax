package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.controller.InitialPasswordController;
import jp.lg.asp.accommodation.dto.UserForm;
import jp.lg.asp.accommodation.dto.UserSearchForm;
import jp.lg.asp.accommodation.entity.Role;
import jp.lg.asp.accommodation.entity.User;
import jp.lg.asp.accommodation.entity.UserId;
import jp.lg.asp.accommodation.repository.RoleRepository;
import jp.lg.asp.accommodation.repository.UserRepository;
import jp.lg.asp.accommodation.service.impl.AdminUserServiceImpl;

@ExtendWith(MockitoExtension.class)
class AdminUserSearchServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks AdminUserServiceImpl service;

    private static final String JICHITAI_CD = "01100";

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User user(String id, String delFlg) {
        User u = new User();
        u.setJichitaiCd(JICHITAI_CD);
        u.setId(id);
        u.setName("テスト");
        u.setNameKana("テスト");
        u.setBusho("総務課");
        u.setRoleId(BigDecimal.ONE);
        u.setDelFlg(delFlg);
        return u;
    }

    private UserId userId(String id) {
        UserId pk = new UserId();
        pk.setJichitaiCd(JICHITAI_CD);
        pk.setId(id);
        return pk;
    }

    private void setSecurityContext(String name) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(name, null, List.of()));
    }

    // ── searchAll ─────────────────────────────────────────────────

    @Test
    void searchAll_全条件指定_jichitaiCdとLIKEパターンを正しくリポジトリに渡す() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(userRepository.searchAll(any(), any(), any(), any(), any(), any())).thenReturn(List.of(user("U001", "0")));
        UserSearchForm form = new UserSearchForm();
        form.setId("U001");
        form.setName("山田");
        form.setNameMatchType("partial");
        form.setBusho("総務");
        form.setBushoMatchType("prefix");

        service.searchAll(form);

        verify(userRepository).searchAll(JICHITAI_CD, "U001", "%山田%", null, "総務%", null);
    }

    @Test
    void searchAll_条件が空白の場合_nullに変換してリポジトリに渡す() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(userRepository.searchAll(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        UserSearchForm form = new UserSearchForm();
        form.setId("");
        form.setName(" ");
        form.setNameKana("");

        service.searchAll(form);

        verify(userRepository).searchAll(JICHITAI_CD, null, null, null, null, null);
    }

    @Test
    void searchAll_nameMatchType_prefixの場合_前方一致パターンで渡す() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(userRepository.searchAll(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        UserSearchForm form = new UserSearchForm();
        form.setName("山田");
        form.setNameMatchType("prefix");

        service.searchAll(form);

        verify(userRepository).searchAll(eq(JICHITAI_CD), any(), eq("山田%"), any(), any(), any());
    }

    @Test
    void searchAll_nameMatchType_exactの場合_完全一致パターンで渡す() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(userRepository.searchAll(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        UserSearchForm form = new UserSearchForm();
        form.setName("山田");
        form.setNameMatchType("exact");

        service.searchAll(form);

        verify(userRepository).searchAll(eq(JICHITAI_CD), any(), eq("山田"), any(), any(), any());
    }

    // ── selectableRoles ───────────────────────────────────────────

    @Test
    void selectableRoles_DEFAULT_USER_ROLE_IDのロールは除外される() {
        Role defaultRole = new Role();
        defaultRole.setRoleId(UserRepository.DEFAULT_USER_ROLE_ID);
        Role normalRole = new Role();
        normalRole.setRoleId(2L);
        when(roleRepository.findByJichitaiCdOrderByRoleId(JICHITAI_CD)).thenReturn(List.of(defaultRole, normalRole));

        List<Role> result = service.selectableRoles(JICHITAI_CD, null);

        assertThat(result).doesNotContain(defaultRole);
        assertThat(result).contains(normalRole);
    }

    @Test
    void selectableRoles_currentRoleIdがDEFAULT_USER_ROLE_IDと一致する場合は除外しない() {
        Role defaultRole = new Role();
        defaultRole.setRoleId(UserRepository.DEFAULT_USER_ROLE_ID);
        when(roleRepository.findByJichitaiCdOrderByRoleId(JICHITAI_CD)).thenReturn(List.of(defaultRole));

        List<Role> result = service.selectableRoles(JICHITAI_CD, BigDecimal.valueOf(UserRepository.DEFAULT_USER_ROLE_ID));

        assertThat(result).contains(defaultRole);
    }

    // ── search ────────────────────────────────────────────────────

    @Test
    void search_ページング条件付き検索_PageRequestが正しく生成されてsearchPageに渡される() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        Page<User> emptyPage = new PageImpl<>(List.of());
        when(userRepository.searchPage(any(), any(), any(), any(), any(), any(), any())).thenReturn(emptyPage);
        UserSearchForm form = new UserSearchForm();
        form.setPage(2);
        form.setPageSize(10);

        service.search(form);

        verify(userRepository).searchPage(
                eq(JICHITAI_CD), any(), any(), any(), any(), any(),
                eq(PageRequest.of(2, 10)));
    }

    // ── findById ──────────────────────────────────────────────────

    @Test
    void findById_存在するIDの場合_該当Userエンティティが返される() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        User u = user("U001", "0");
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(u));

        User result = service.findById("U001");

        assertThat(result).isEqualTo(u);
    }

    @Test
    void findById_存在しないIDの場合_RuntimeExceptionがスローされる() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById("U999"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ユーザーが見つかりません: U999");
    }

    // ── existsActiveUser ──────────────────────────────────────────

    @Test
    void existsActiveUser_delFlg_0のユーザーが存在する場合_trueを返す() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(user("U001", "0")));

        assertThat(service.existsActiveUser("U001")).isTrue();
    }

    @Test
    void existsActiveUser_delFlg_1の場合_falseを返す() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(user("U001", "1")));

        assertThat(service.existsActiveUser("U001")).isFalse();
    }

    @Test
    void existsActiveUser_ユーザーが存在しない場合_falseを返す() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.empty());

        assertThat(service.existsActiveUser("U999")).isFalse();
    }

    // ── register ──────────────────────────────────────────────────

    @Test
    void register_新規ユーザー登録_エンティティが生成されパスワード暗号化とinitialPasswordFlg設定後saveが呼ばれる() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        UserForm form = new UserForm();
        form.setId("U001");
        form.setPassword("pass");
        form.setName("山田太郎");
        form.setNameKana("ヤマダタロウ");
        form.setBusho("総務課");
        form.setRoleId(BigDecimal.ONE);

        service.register(form);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getJichitaiCd()).isEqualTo(JICHITAI_CD);
        assertThat(saved.getPassword()).isEqualTo("encoded");
        assertThat(saved.getInitialPasswordFlg()).isEqualTo("1");
        assertThat(saved.getName()).isEqualTo("山田太郎");
    }

    @Test
    void register_削除済みユーザーの再登録_delFlgが0に更新されsaveが呼ばれる() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        User existing = user("U001", "1");
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        UserForm form = new UserForm();
        form.setId("U001");
        form.setPassword("pass");
        form.setName("山田太郎");
        form.setNameKana("ヤマダタロウ");
        form.setBusho("総務課");
        form.setRoleId(BigDecimal.ONE);

        service.register(form);

        verify(userRepository, times(1)).save(existing);
        assertThat(existing.getDelFlg()).isEqualTo("0");
        assertThat(existing.getInitialPasswordFlg()).isEqualTo("1");
        assertThat(existing.getPassword()).isEqualTo("encoded");
    }

    // ── update ────────────────────────────────────────────────────

    @Test
    void update_一般ユーザーの更新_氏名カナ部署ロールパスワードが更新されsaveが呼ばれる() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        User u = user("U001", "0");
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(u));
        when(passwordEncoder.encode("newpass")).thenReturn("encoded");
        setSecurityContext("OTHER");
        UserForm form = new UserForm();
        form.setName("鈴木");
        form.setNameKana("スズキ");
        form.setBusho("企画課");
        form.setRoleId(BigDecimal.valueOf(2));
        form.setPassword("newpass");

        service.update("U001", form);

        verify(userRepository, times(1)).save(u);
        assertThat(u.getName()).isEqualTo("鈴木");
        assertThat(u.getRoleId()).isEqualTo(BigDecimal.valueOf(2));
        assertThat(u.getPassword()).isEqualTo("encoded");
    }

    @Test
    void update_ADMIN_IDユーザーの更新_roleIdは変更されない() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        User u = user(InitialPasswordController.ADMIN_ID, "0");
        u.setRoleId(BigDecimal.ONE);
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(u));
        setSecurityContext("OTHER");
        UserForm form = new UserForm();
        form.setName("管理者");
        form.setNameKana("カンリシャ");
        form.setBusho("総務課");
        form.setRoleId(BigDecimal.valueOf(99));
        form.setPassword(null);

        service.update(InitialPasswordController.ADMIN_ID, form);

        verify(userRepository, times(1)).save(u);
        assertThat(u.getRoleId()).isEqualTo(BigDecimal.ONE);
    }

    @Test
    void update_パスワード未入力の場合_passwordEncoderが呼ばれない() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        User u = user("U001", "0");
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(u));
        setSecurityContext("OTHER");
        UserForm form = new UserForm();
        form.setName("山田太郎");
        form.setNameKana("ヤマダタロウ");
        form.setBusho("総務課");
        form.setRoleId(BigDecimal.ONE);
        form.setPassword("");

        service.update("U001", form);

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, times(1)).save(u);
    }

    // ── delete ────────────────────────────────────────────────────

    @Test
    void delete_論理削除_delFlgが1に更新されsaveが呼ばれる() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        User u = user("U001", "0");
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(u));

        service.delete("U001");

        verify(userRepository, times(1)).save(u);
        assertThat(u.getDelFlg()).isEqualTo("1");
    }

    // ── isLoginUser ───────────────────────────────────────────────

    @Test
    void isLoginUser_ログイン中ユーザーIDと一致する場合_trueを返す() {
        setSecurityContext("U001");

        assertThat(service.isLoginUser("U001")).isTrue();
    }

    @Test
    void isLoginUser_ログイン中ユーザーIDと一致しない場合_falseを返す() {
        setSecurityContext("U001");

        assertThat(service.isLoginUser("U002")).isFalse();
    }
}

package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
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

/**
 * AdminUserServiceImpl 単体テスト
 *
 * <p>ユーザー検索・ユーザー登録編集削除は対象クラスが同じため、1ファイルにまとめる
 * （MRは画面ごとに分割する）。
 * チェックリストがどちらも #1 から始まるため、@DisplayName の番号には画面名を付けて区別する。</p>
 *
 * <ul>
 *   <li>#検索4〜#検索13 … 「ユーザー検索_単体テストチェックリスト.xlsx」</li>
 *   <li>#登録編集削除… … 「ユーザー登録編集削除_単体テストチェックリスト.xlsx」（別MRで追加）</li>
 * </ul>
 *
 * <p>チェックリストはあるべき仕様で書かれている。テストが通るように期待値を実装へ寄せないこと。</p>
 */
@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks AdminUserServiceImpl service;

    private static final String JICHITAI_CD = "01100";

    @BeforeEach
    void setUp() {
        lenient().when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    // ------------------------------------------------------------------
    // テストデータ生成
    // ------------------------------------------------------------------

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

    /** searchAll の戻り値をスタブする */
    private void stubSearchAll(List<User> result) {
        when(userRepository.searchAll(eq(JICHITAI_CD), any(), any(), any(), any(), any()))
                .thenReturn(result);
    }

    // ==================================================================
    // ユーザー検索：searchAll
    // ==================================================================

    @Test
    @DisplayName("#検索4 searchAll 正常系 全条件指定：jichitaiCdとLIKEパターンを正しくリポジトリに渡す")
    void searchAll_全条件指定でLIKEパターンが渡される() {
        stubSearchAll(List.of(user("U001")));

        UserSearchForm form = new UserSearchForm();
        form.setId("U001");
        form.setName("山田");
        form.setNameMatchType("partial");
        form.setBusho("総務");
        form.setBushoMatchType("prefix");

        service.searchAll(form);

        verify(userRepository, times(1))
                .searchAll(JICHITAI_CD, "U001", "%山田%", null, "総務%", null);
    }

    @Test
    @DisplayName("#検索5 searchAll 正常系 条件が空白の場合：nullに変換してリポジトリに渡す")
    void searchAll_空白条件はnullに変換される() {
        stubSearchAll(List.of());

        UserSearchForm form = new UserSearchForm();
        form.setId("");
        form.setName(" ");
        form.setNameKana("");
        form.setBusho("");

        service.searchAll(form);

        verify(userRepository, times(1))
                .searchAll(JICHITAI_CD, null, null, null, null, null);
    }

    @Test
    @DisplayName("#検索6 searchAll 正常系 nameMatchType=prefixの場合：前方一致パターン（値+%）で渡す")
    void searchAll_氏名prefixは前方一致パターン() {
        stubSearchAll(List.of());

        UserSearchForm form = new UserSearchForm();
        form.setName("山田");
        form.setNameMatchType("prefix");

        service.searchAll(form);

        verify(userRepository, times(1))
                .searchAll(JICHITAI_CD, null, "山田%", null, null, null);
    }

    @Test
    @DisplayName("#検索7 searchAll 正常系 nameMatchType=exactの場合：完全一致パターン（値そのまま）で渡す")
    void searchAll_氏名exactは値そのまま() {
        stubSearchAll(List.of());

        UserSearchForm form = new UserSearchForm();
        form.setName("山田");
        form.setNameMatchType("exact");

        service.searchAll(form);

        verify(userRepository, times(1))
                .searchAll(JICHITAI_CD, null, "山田", null, null, null);
    }

    @Test
    @DisplayName("#検索8 searchAll 正常系 nameKanaMatchType=prefixの場合：前方一致パターン（値+%）で渡す")
    void searchAll_カナprefixは前方一致パターン() {
        stubSearchAll(List.of());

        UserSearchForm form = new UserSearchForm();
        form.setNameKana("ヤマダ");
        form.setNameKanaMatchType("prefix");

        service.searchAll(form);

        verify(userRepository, times(1))
                .searchAll(JICHITAI_CD, null, null, "ヤマダ%", null, null);
    }

    @Test
    @DisplayName("#検索9 searchAll 正常系 nameKanaMatchType=exactの場合：完全一致パターン（値そのまま）で渡す")
    void searchAll_カナexactは値そのまま() {
        stubSearchAll(List.of());

        UserSearchForm form = new UserSearchForm();
        form.setNameKana("ヤマダ");
        form.setNameKanaMatchType("exact");

        service.searchAll(form);

        verify(userRepository, times(1))
                .searchAll(JICHITAI_CD, null, null, "ヤマダ", null, null);
    }

    @Test
    @DisplayName("#検索10 searchAll 正常系 bushoMatchType=prefixの場合：前方一致パターン（値+%）で渡す")
    void searchAll_部署prefixは前方一致パターン() {
        stubSearchAll(List.of());

        UserSearchForm form = new UserSearchForm();
        form.setBusho("総務");
        form.setBushoMatchType("prefix");

        service.searchAll(form);

        verify(userRepository, times(1))
                .searchAll(JICHITAI_CD, null, null, null, "総務%", null);
    }

    @Test
    @DisplayName("#検索11 searchAll 正常系 bushoMatchType=exactの場合：完全一致パターン（値そのまま）で渡す")
    void searchAll_部署exactは値そのまま() {
        stubSearchAll(List.of());

        UserSearchForm form = new UserSearchForm();
        form.setBusho("総務");
        form.setBushoMatchType("exact");

        service.searchAll(form);

        verify(userRepository, times(1))
                .searchAll(JICHITAI_CD, null, null, null, "総務", null);
    }

    // ==================================================================
    // ユーザー検索：selectableRoles
    // ==================================================================

    @Test
    @DisplayName("#検索12 selectableRoles 正常系 DEFAULT_USER_ROLE_IDのロールは除外される")
    void selectableRoles_既定ロールは除外される() {
        Role defaultRole = role(UserRepository.DEFAULT_USER_ROLE_ID, "既定ユーザー");
        Role role2 = role(2L, "管理者");
        when(roleRepository.findByJichitaiCdOrderByRoleId(JICHITAI_CD))
                .thenReturn(List.of(defaultRole, role2));

        List<Role> result = service.selectableRoles(JICHITAI_CD, null);

        assertThat(result).containsExactly(role2);
        assertThat(result).doesNotContain(defaultRole);
    }

    @Test
    @DisplayName("#検索13 selectableRoles 正常系 currentRoleIdがDEFAULT_USER_ROLE_IDと一致する場合は除外しない")
    void selectableRoles_現在のロールが既定ロールなら除外しない() {
        Role defaultRole = role(UserRepository.DEFAULT_USER_ROLE_ID, "既定ユーザー");
        when(roleRepository.findByJichitaiCdOrderByRoleId(JICHITAI_CD))
                .thenReturn(List.of(defaultRole));

        List<Role> result = service.selectableRoles(
                JICHITAI_CD, BigDecimal.valueOf(UserRepository.DEFAULT_USER_ROLE_ID));

        assertThat(result).containsExactly(defaultRole);
    }

    // ==================================================================
    // ユーザー登録編集削除
    // ==================================================================

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** ログイン中ユーザーをSecurityContextに設定する */
    private void setLoginUser(String id) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(id, "password"));
        SecurityContextHolder.setContext(context);
    }

    private User user(String id, String delFlg) {
        User user = user(id);
        user.setDelFlg(delFlg);
        return user;
    }

    private UserForm userForm(String id, String password) {
        UserForm form = new UserForm();
        form.setId(id);
        form.setName("山田太郎");
        form.setNameKana("ヤマダタロウ");
        form.setBusho("総務課");
        form.setRoleId(BigDecimal.ONE);
        form.setPassword(password);
        return form;
    }

    /** save() に渡された User を取得する */
    private User captureSavedUser() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("#登録編集削除13 findById 正常系 存在するIDの場合：該当Userエンティティが返される")
    void findById_存在するIDはユーザーが返る() {
        User user = user("U001");
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(user));

        assertThat(service.findById("U001")).isSameAs(user);
    }

    @Test
    @DisplayName("#登録編集削除14 findById 異常系 存在しないIDの場合：RuntimeExceptionがスローされる")
    void findById_存在しないIDは例外() {
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById("U999"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("ユーザーが見つかりません: U999");
    }

    @Test
    @DisplayName("#登録編集削除15 existsActiveUser 正常系 delFlg=\"0\"のユーザーが存在する場合：trueを返す")
    void existsActiveUser_有効ユーザーはtrue() {
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(user("U001", "0")));

        assertThat(service.existsActiveUser("U001")).isTrue();
    }

    @Test
    @DisplayName("#登録編集削除16 existsActiveUser 正常系 delFlg=\"1\"（削除済み）の場合：falseを返す")
    void existsActiveUser_削除済みはfalse() {
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(user("U001", "1")));

        assertThat(service.existsActiveUser("U001")).isFalse();
    }

    @Test
    @DisplayName("#登録編集削除17 existsActiveUser 正常系 ユーザーが存在しない場合：falseを返す")
    void existsActiveUser_存在しないユーザーはfalse() {
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.empty());

        assertThat(service.existsActiveUser("U999")).isFalse();
    }

    @Test
    @DisplayName("#登録編集削除18 register 正常系 新規ユーザー登録：新しいエンティティが生成されパスワード暗号化・initialPasswordFlg=\"1\"・jichitaiCd=\"01100\"が設定されsave()が1回呼ばれる")
    void register_新規登録() {
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass")).thenReturn("encoded");

        service.register(userForm("U001", "pass"));

        User saved = captureSavedUser();
        assertThat(saved.getJichitaiCd()).isEqualTo(JICHITAI_CD);
        assertThat(saved.getId()).isEqualTo("U001");
        assertThat(saved.getPassword()).isEqualTo("encoded");
        assertThat(saved.getInitialPasswordFlg()).isEqualTo("1");
        assertThat(saved.getName()).isEqualTo("山田太郎");
    }

    @Test
    @DisplayName("#登録編集削除19 register 正常系 削除済みユーザーの再登録：既存エンティティのdelFlgが\"0\"に更新されsave()が1回呼ばれる")
    void register_削除済みユーザーの再登録() {
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(user("U001", "1")));
        when(passwordEncoder.encode("pass")).thenReturn("encoded");

        service.register(userForm("U001", "pass"));

        User saved = captureSavedUser();
        assertThat(saved.getDelFlg()).isEqualTo("0");
        assertThat(saved.getInitialPasswordFlg()).isEqualTo("1");
        assertThat(saved.getPassword()).isEqualTo("encoded");
    }

    @Test
    @DisplayName("#登録編集削除20 update 正常系 一般ユーザーの更新：氏名・カナ・部署・ロールが更新されsave()が1回呼ばれる")
    void update_一般ユーザーの更新() {
        setLoginUser("OTHER");
        User user = user("U001");
        user.setRoleId(BigDecimal.ONE);
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(user));

        UserForm form = userForm("U001", null);
        form.setName("鈴木");
        form.setNameKana("スズキ");
        form.setBusho("企画課");
        form.setRoleId(BigDecimal.valueOf(2));

        service.update("U001", form);

        User saved = captureSavedUser();
        assertThat(saved.getName()).isEqualTo("鈴木");
        assertThat(saved.getNameKana()).isEqualTo("スズキ");
        assertThat(saved.getBusho()).isEqualTo("企画課");
        assertThat(saved.getRoleId()).isEqualByComparingTo(BigDecimal.valueOf(2));
    }

    @Test
    @DisplayName("#登録編集削除21 update 正常系 パスワード入力ありの更新：パスワードが暗号化されinitialPasswordFlg=\"1\"が設定される")
    void update_パスワード入力ありの更新() {
        setLoginUser("OTHER");
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(user("U001")));
        when(passwordEncoder.encode("newpass")).thenReturn("encoded");

        service.update("U001", userForm("U001", "newpass"));

        User saved = captureSavedUser();
        assertThat(saved.getPassword()).isEqualTo("encoded");
        assertThat(saved.getInitialPasswordFlg()).isEqualTo("1");
        verify(passwordEncoder, times(1)).encode("newpass");
    }

    @Test
    @DisplayName("#登録編集削除22 update 正常系 パスワード未入力の更新：passwordEncoder.encode()が呼ばれない")
    void update_パスワード未入力の更新() {
        setLoginUser("OTHER");
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(user("U001")));

        service.update("U001", userForm("U001", null));

        verify(passwordEncoder, never()).encode(any());
        captureSavedUser();
    }

    @Test
    @DisplayName("#登録編集削除23 update 正常系 ADMIN_IDユーザーの更新：roleIdが変更されない")
    void update_ADMIN_IDはロールが変更されない() {
        setLoginUser("OTHER");
        User user = user(InitialPasswordController.ADMIN_ID);
        user.setRoleId(BigDecimal.ONE);
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(user));

        UserForm form = userForm(InitialPasswordController.ADMIN_ID, null);
        form.setRoleId(BigDecimal.valueOf(99));

        service.update(InitialPasswordController.ADMIN_ID, form);

        User saved = captureSavedUser();
        assertThat(saved.getRoleId()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("#登録編集削除24 delete 正常系 論理削除：delFlg=\"1\"に更新されsave()が1回呼ばれる")
    void delete_論理削除() {
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(user("U001", "0")));

        service.delete("U001");

        assertThat(captureSavedUser().getDelFlg()).isEqualTo("1");
    }

    @Test
    @DisplayName("#登録編集削除25 isLoginUser 正常系 ログイン中ユーザーIDと一致する場合：trueを返す")
    void isLoginUser_一致する場合はtrue() {
        setLoginUser("U001");

        assertThat(service.isLoginUser("U001")).isTrue();
    }

    @Test
    @DisplayName("#登録編集削除26 isLoginUser 正常系 ログイン中ユーザーIDと一致しない場合：falseを返す")
    void isLoginUser_一致しない場合はfalse() {
        setLoginUser("U001");

        assertThat(service.isLoginUser("U002")).isFalse();
    }

    @Test
    @DisplayName("#登録編集削除27 update 正常系 ログイン中のユーザーを更新した場合、SecurityContext上のユーザー情報も更新される")
    void update_ログイン中ユーザー更新でSecurityContextも更新される() {
        User existing = user("U001");
        existing.setName("旧名前");
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(existing));

        jp.lg.asp.accommodation.config.AppUserDetails principal =
                new jp.lg.asp.accommodation.config.AppUserDetails(
                        "U001", "password", List.of(), false);
        principal.setDisplayName("旧名前");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "password", List.of()));

        UserForm form = userForm("U001", null);
        form.setName("新しい名前");

        service.update("U001", form);

        verify(userRepository, times(1)).save(any());

        Object updatedPrincipal = SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        assertThat(updatedPrincipal).isInstanceOf(jp.lg.asp.accommodation.config.AppUserDetails.class);
        assertThat(((jp.lg.asp.accommodation.config.AppUserDetails) updatedPrincipal).getDisplayName())
                .isEqualTo("新しい名前");
    }
}

package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.UserSearchForm;
import jp.lg.asp.accommodation.entity.Role;
import jp.lg.asp.accommodation.entity.User;
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
}

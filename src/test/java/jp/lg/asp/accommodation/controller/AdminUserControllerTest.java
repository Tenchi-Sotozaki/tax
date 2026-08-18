package jp.lg.asp.accommodation.controller;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.repository.RoleRepository;
import jp.lg.asp.accommodation.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminUserControllerTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock ScreenAccessChecker accessChecker;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks AdminUserController controller;
/*
    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("011002");
        when(userRepository.searchPage(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(roleRepository.findByJichitaiCdOrderByRoleId("011002")).thenReturn(List.of());

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("other_user");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @Test
    void list_一覧画面を返す() {
        Model model = new ExtendedModelMap();

        String view = controller.list(new UserSearchForm(), 0, 10, model);

        assertThat(view).isEqualTo("admin/userDaicho");
        assertThat(model.asMap()).containsKey("items");
        assertThat(model.asMap().get("items")).isInstanceOf(Page.class);
    }

    @Test
    void showRegistrationForm_登録画面を返す() {
        Model model = new ExtendedModelMap();

        String view = controller.showRegistrationForm(model);

        assertThat(view).isEqualTo("admin/userConfig");
        assertThat(model.asMap()).containsEntry("isEdit", false);
    }

    @Test
    void register_パスワード不一致はエラー() {
        UserForm form = new UserForm();
        form.setId("user01");
        form.setPassword("pass1");
        form.setPasswordConfirm("pass2");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");
        Model model = new ExtendedModelMap();

        String view = controller.register(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/userConfig");
    }

    @Test
    void register_正常登録() {
        UserForm form = new UserForm();
        form.setId("user01");
        form.setPassword("pass1");
        form.setPasswordConfirm("pass1");
        form.setName("テストユーザー");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass1")).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Model model = new ExtendedModelMap();

        String view = controller.register(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/admin/user-search");
    }

    @Test
    void delete_ログイン中ユーザーは削除不可() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user01");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);

        String view = controller.delete("user01", new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/admin/user-search");
        verify(userRepository, never()).save(any());
    }

    @Test
    void delete_他ユーザーは論理削除される() {
        User user = new User();
        user.setId("other_user2");
        user.setDelFlg("0");
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String view = controller.delete("other_user2", new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/admin/user-search");
        assertThat(user.getDelFlg()).isEqualTo("1");
        verify(userRepository).save(any(User.class));
        verify(userRepository, never()).deleteById(any());
    }
*/
}

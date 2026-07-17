package jp.lg.asp.accommodation.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import jp.lg.asp.accommodation.entity.User;
import jp.lg.asp.accommodation.entity.UserId;
import jp.lg.asp.accommodation.exception.AccessDeniedException;
import jp.lg.asp.accommodation.repository.RoleRepository;
import jp.lg.asp.accommodation.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScreenAccessCheckerTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private JichitaiContext jichitaiContext;
    @Mock private Authentication authentication;
    @Mock private SecurityContext securityContext;

    @InjectMocks
    private ScreenAccessChecker screenAccessChecker;

    private static final String JICHITAI_CD = "011002";
    private static final String USER_ID = "testUser";
    private static final String SCREEN_ID = "SCR001";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(USER_ID);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User createUser(BigDecimal roleId) {
        User user = new User();
        UserId pk = new UserId();
        pk.setJichitaiCd(JICHITAI_CD);
        pk.setId(USER_ID);
        user.setRoleId(roleId);
        return user;
    }

    // --- checkAccess ---

    @Test
    void checkAccess_ユーザーが存在しない場合はスキップ() {
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.empty());
        assertThatNoException().isThrownBy(() -> screenAccessChecker.checkAccess(SCREEN_ID));
    }

    @Test
    void checkAccess_roleIdがnullの場合はスキップ() {
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(createUser(null)));
        assertThatNoException().isThrownBy(() -> screenAccessChecker.checkAccess(SCREEN_ID));
    }

    @Test
    void checkAccess_アクセス権限あり() {
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(createUser(BigDecimal.ONE)));
        when(roleRepository.countAccessibleScreen(JICHITAI_CD, 1L, SCREEN_ID)).thenReturn(1L);
        assertThatNoException().isThrownBy(() -> screenAccessChecker.checkAccess(SCREEN_ID));
    }

    @Test
    void checkAccess_アクセス権限なしはAccessDeniedExceptionをスロー() {
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(createUser(BigDecimal.ONE)));
        when(roleRepository.countAccessibleScreen(JICHITAI_CD, 1L, SCREEN_ID)).thenReturn(0L);
        assertThatThrownBy(() -> screenAccessChecker.checkAccess(SCREEN_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- checkWriteAccess ---

    @Test
    void checkWriteAccess_ユーザーが存在しない場合はスキップ() {
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.empty());
        assertThatNoException().isThrownBy(() -> screenAccessChecker.checkWriteAccess(SCREEN_ID));
    }

    @Test
    void checkWriteAccess_roleIdがnullの場合はスキップ() {
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(createUser(null)));
        assertThatNoException().isThrownBy(() -> screenAccessChecker.checkWriteAccess(SCREEN_ID));
    }

    @Test
    void checkWriteAccess_更新権限あり() {
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(createUser(BigDecimal.ONE)));
        when(roleRepository.countWritableScreen(JICHITAI_CD, 1L, SCREEN_ID)).thenReturn(1L);
        assertThatNoException().isThrownBy(() -> screenAccessChecker.checkWriteAccess(SCREEN_ID));
    }

    @Test
    void checkWriteAccess_更新権限なしはAccessDeniedExceptionをスロー() {
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(createUser(BigDecimal.ONE)));
        when(roleRepository.countWritableScreen(JICHITAI_CD, 1L, SCREEN_ID)).thenReturn(0L);
        assertThatThrownBy(() -> screenAccessChecker.checkWriteAccess(SCREEN_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void checkAccess_AccessDeniedExceptionにscreenIdとuserIdが含まれる() {
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(createUser(BigDecimal.ONE)));
        when(roleRepository.countAccessibleScreen(JICHITAI_CD, 1L, SCREEN_ID)).thenReturn(0L);
        assertThatThrownBy(() -> screenAccessChecker.checkAccess(SCREEN_ID))
                .isInstanceOf(AccessDeniedException.class)
                .satisfies(e -> {
                    AccessDeniedException ex = (AccessDeniedException) e;
                    assertThat(ex.getScreenId()).isEqualTo(SCREEN_ID);
                    assertThat(ex.getUserId()).isEqualTo(USER_ID);
                });
    }
}

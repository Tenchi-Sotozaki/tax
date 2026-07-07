package jp.lg.asp.accommodation.config;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import jp.lg.asp.accommodation.entity.User;
import jp.lg.asp.accommodation.entity.UserId;
import jp.lg.asp.accommodation.repository.RoleRepository;
import jp.lg.asp.accommodation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
public class DbUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	// layout.html の sec:authorize="hasRole('ADMIN')" と対応する管理系画面ID
	private static final Set<String> ADMIN_SCREENS = Set.of(
			ScreenManagement.USER_MANAGEMENT.strip(),
			ScreenManagement.ROLE_MANAGEMENT.strip(),
			ScreenManagement.NOZEI_SHUKI.strip(),
			ScreenManagement.NOZEI_SHUKI_CONFIG.strip(),
			ScreenManagement.ZEIRITSU_CONFIG.strip(),
			ScreenManagement.KOFU_RITSU_CONFIG.strip(),
			ScreenManagement.NOKIGEN_CONFIG.strip(),
			ScreenManagement.NOKIGEN.strip(),
			ScreenManagement.SHITEI_GASSAN_CONFIG.strip(),
			ScreenManagement.SHITEI_GASSAN.strip());

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		UserId pk = new UserId();
		pk.setJichitaiCd(jichitaiCd);
		pk.setId(username);

		User user = userRepository.findById(pk)
				.orElseThrow(() -> {
					log.warn("ユーザー見つからず: username={}, jichitaiCd={}", username, jichitaiCd);
					return new UsernameNotFoundException("ユーザーが見つかりません: " + username);
				});

		String password = user.getPassword() != null ? user.getPassword().trim() : "";

		/**
		 * m_role_dtl に管理系画面（USER_MANAGEMENT / ROLE_MANAGEMENT / NOZEI_SHUKI）への
		 *アクセス権（permission >= 1）が1件でもあれば ROLE_ADMIN、なければ ROLE_USER
		**/
		boolean isAdmin = user.getRoleId() != null && roleRepository
				.findByIdWithDetails(jichitaiCd, user.getRoleId().longValue())
				.map(role -> role.getRoleDetails() != null && role.getRoleDetails().stream()
						.anyMatch(rd -> rd.getPermission() != null && rd.getPermission().compareTo("1") >= 0
								&& ADMIN_SCREENS.contains(rd.getScreenId().strip())))
				.orElse(false);
		String role = isAdmin ? "ROLE_ADMIN" : "ROLE_USER";

		boolean mustChangePassword = "1".equals(user.getInitialPasswordFlg());

	    return new AppUserDetails(
	            user.getId(),
	            password,
	            List.of(new SimpleGrantedAuthority(role)),
	            mustChangePassword);
	}
}

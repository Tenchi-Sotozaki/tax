package jp.lg.asp.accommodation.config;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import jp.lg.asp.accommodation.entity.User;
import jp.lg.asp.accommodation.entity.UserId;
import jp.lg.asp.accommodation.repository.RoleRepository;
import jp.lg.asp.accommodation.repository.UserRepository;
import lombok.RequiredArgsConstructor;

/**
 * 本番用 UserDetailsService（DB連携）
 * 差し替え時は SecurityConfig#userDetailsService の @Bean を削除し、
 * このクラスの @Component コメントを外すこと。
 * 現在は @Component を無効化しているため Spring に登録されない。
 */
// @Component
@RequiredArgsConstructor
public class DbUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	// layout.html の sec:authorize="hasRole('ADMIN')" と対応する管理系画面ID
	private static final Set<String> ADMIN_SCREENS = Set.of(
			ScreenId.USER_MANAGEMENT.strip(), ScreenId.ROLE_MANAGEMENT.strip(),
			ScreenId.NOZEI_SHUKI.strip());

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		UserId pk = new UserId();
		pk.setJichitaiCd(jichitaiCd);
		pk.setId(username);

		User user = userRepository.findById(pk)
				.orElseThrow(() -> new UsernameNotFoundException("ユーザーが見つかりません: " + username));

		/**
		 * m_role_dtl に管理系画面（USER_MANAGEMENT / ROLE_MANAGEMENT / NOZEI_SHUKI）への
		 *アクセス権（permission >= 1）が1件でもあれば ROLE_ADMIN、なければ ROLE_USER
		**/
		boolean isAdmin = user.getRoleId() != null && roleRepository
				.findByIdWithDetails(jichitaiCd, user.getRoleId().longValue())
				.map(role -> role.getRoleDetails() != null && role.getRoleDetails().stream()
						.anyMatch(rd -> rd.getPermission() != null && rd.getPermission() >= 1
								&& ADMIN_SCREENS.contains(rd.getScreenId().strip())))
				.orElse(false);

		String role = isAdmin ? "ROLE_ADMIN" : "ROLE_USER";

		return new org.springframework.security.core.userdetails.User(
				user.getId(),
				user.getPassword(),
				List.of(new SimpleGrantedAuthority(role)));
	}
}

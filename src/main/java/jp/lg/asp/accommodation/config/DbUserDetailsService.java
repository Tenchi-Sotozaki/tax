package jp.lg.asp.accommodation.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import jp.lg.asp.accommodation.entity.User;
import jp.lg.asp.accommodation.entity.UserId;
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

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		UserId pk = new UserId();
		pk.setJichitaiCd(jichitaiCd);
		pk.setId(username);

		User user = userRepository.findById(pk)
				.orElseThrow(() -> new UsernameNotFoundException("ユーザーが見つかりません: " + username));

		// role_id=1 をデフォルト（ROLE_USER）、それ以外は ROLE_ADMIN とする想定
		// 本番要件に合わせて権限マッピングを修正
		String role = user.getRoleId() != null && user.getRoleId().longValue() == 1
				? "ROLE_USER"
				: "ROLE_ADMIN";

		return new org.springframework.security.core.userdetails.User(
				user.getId(),
				user.getPassword(),
				List.of(new SimpleGrantedAuthority(role)));
	}
}

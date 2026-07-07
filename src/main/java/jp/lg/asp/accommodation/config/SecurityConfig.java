package jp.lg.asp.accommodation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(auth -> auth
						// 静的リソースとログイン画面は誰でもアクセス可
						.requestMatchers("/css/**", "/js/**", "/fonts/**", "/images/**",
								"/login", "/error", "/*.html")
						.permitAll()
						// 初期セットアップ用: ユーザー登録画面は認証なしで許可
						// ※コントローラ側で「初期セットアップ中以外は権限チェック」を必ず入れること
						.requestMatchers("/admin/user-registration").permitAll()
						// /admin/** は ADMIN のみ
						.requestMatchers("/admin/**").hasRole("ADMIN")
						// 業務画面は USER・ADMIN 両方アクセス可
						.requestMatchers("/tokugimu/**", "/declaration/**", "/atena/**", "/reports/**")
						.hasAnyRole("USER", "ADMIN")
						// その他は認証済みであればアクセス可
						.anyRequest().authenticated())
				.formLogin(form -> form
						.loginPage("/login")
						.defaultSuccessUrl("/tokugimu/list", true)
						.permitAll())
				.logout(logout -> logout
						.logoutSuccessUrl("/login?logout")
						.permitAll());
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
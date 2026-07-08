package jp.lg.asp.accommodation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http,
			PasswordChangeRequiredFilter passwordChangeRequiredFilter,
			InitialAdminPasswordFilter initialAdminPasswordFilter) throws Exception { // ★引数を追加
		http
				.authorizeHttpRequests(auth -> auth
						// 静的リソースとログイン画面は誰でもアクセス可
						.requestMatchers("/css/**", "/js/**", "/fonts/**", "/images/**",
								"/login", "/error", "/*.html")
						.permitAll()
						// ★追加：初回パスワード設定画面は未認証でもアクセス可
						.requestMatchers("/admin/password-change").permitAll()
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
						.permitAll())
				// ★追加：ログイン前に、初期パスワードのままなら誘導するフィルター
				.addFilterBefore(initialAdminPasswordFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterAfter(passwordChangeRequiredFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
package jp.lg.asp.accommodation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jp.lg.asp.accommodation.controller.InitialPasswordController;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http,
			PasswordChangeRequiredFilter passwordChangeRequiredFilter) throws Exception {
	    http
	            .authorizeHttpRequests(auth -> auth
						// 静的リソースとログイン画面は誰でもアクセス可
						.requestMatchers("/css/**", "/js/**", "/fonts/**", "/images/**",
								"/login", "/error", "/*.html")
						.permitAll()
						// 初回パスワード設定画面はログイン済みユーザーのみアクセス可
						.requestMatchers("/admin/password-change").authenticated()
						// /admin/** は ADMIN のみ
						.requestMatchers("/admin/**").hasRole("ADMIN")
						// 業務画面は USER・ADMIN 両方アクセス可
						.requestMatchers("/tokugimu/**", "/declaration/**", "/atena/**", "/reports/**")
						.hasAnyRole("USER", "ADMIN")
						// その他は認証済みであればアクセス可
						.anyRequest().authenticated())
	            .formLogin(form -> form
	                    .loginPage("/login")
	                    // 初期ユーザーはユーザー検索画面へ、それ以外は業務トップへ
	                    .successHandler((request, response, authentication) -> {
	                        String target = InitialPasswordController.ADMIN_ID.equals(authentication.getName())
	                                ? "/admin/user-search"
	                                : "/tokugimu/list";
	                        response.sendRedirect(request.getContextPath() + target);
	                    })
	                    .permitAll())
				.logout(logout -> logout
						.logoutSuccessUrl("/login?logout")
						.permitAll())
				// 初期パスワードのままのユーザーを初回パスワード設定画面へ強制誘導
	            .addFilterAfter(passwordChangeRequiredFilter, UsernamePasswordAuthenticationFilter.class);
	    return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}

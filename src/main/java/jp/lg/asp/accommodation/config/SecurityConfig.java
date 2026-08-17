package jp.lg.asp.accommodation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.InvalidSessionStrategy;

import jp.lg.asp.accommodation.controller.InitialPasswordController;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http,
			PasswordChangeRequiredFilter passwordChangeRequiredFilter,
			JichitaiCodeFilter jichitaiCodeFilter) throws Exception {
	    http
	            .authorizeHttpRequests(auth -> auth
						// 静的リソースとログイン画面は誰でもアクセス可
						.requestMatchers("/css/**", "/js/**", "/fonts/**", "/images/**",
								"/login", "/error", "/*.html")
						.permitAll()
						// 初回パスワード設定・パスワード変更画面はログイン済みユーザーのみアクセス可
						.requestMatchers("/admin/password-change", "/admin/user-password-change").authenticated()
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
	                                : "/top";
	                        response.sendRedirect(request.getContextPath() + target);
	                    })
	                    .permitAll())
				.logout(logout -> logout
						// セッション破棄前に自治体コードを退避する（破棄後の再ログインを可能にするため）
						.addLogoutHandler((req, res, auth) -> {
							var session = req.getSession(false);
							if (session != null) {
								req.setAttribute("jichitaiCd", session.getAttribute("jichitaiCd"));
							}
						})
						// 退避した自治体コードを新しいセッションに引き継いでからログイン画面へ
						.logoutSuccessHandler((req, res, auth) -> {
							Object jichitaiCd = req.getAttribute("jichitaiCd");
							if (jichitaiCd != null) {
								req.getSession(true).setAttribute("jichitaiCd", jichitaiCd.toString());
							}
							res.sendRedirect(req.getContextPath() + "/login?logout");
						})
						.permitAll())
				// クエリパラメータの自治体コードをセッションに保存するフィルター
				.addFilterBefore(jichitaiCodeFilter, UsernamePasswordAuthenticationFilter.class)
				// 初期パスワードのままのユーザーを初回パスワード設定画面へ強制誘導
				.addFilterAfter(passwordChangeRequiredFilter, UsernamePasswordAuthenticationFilter.class)
				.sessionManagement(session -> session
						.invalidSessionStrategy(invalidSessionStrategy()));
		return http.build();
	}

	@Bean
	public InvalidSessionStrategy invalidSessionStrategy() {
		return (request, response) -> {
			String jichitaiCd = null;
			if (request.getCookies() != null) {
				for (var cookie : request.getCookies()) {
					if (JichitaiCodeFilter.COOKIE_NAME.equals(cookie.getName())) {
						jichitaiCd = cookie.getValue();
						break;
					}
				}
			}
			if (jichitaiCd != null) {
				request.getSession(true).setAttribute("jichitaiCd", jichitaiCd);
			}
			response.sendRedirect(request.getContextPath() + "/login?expired");
		};
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}

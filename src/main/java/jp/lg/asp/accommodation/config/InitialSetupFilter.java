package jp.lg.asp.accommodation.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jp.lg.asp.accommodation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialSetupFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Value("${app.jichitai.code}")
    private String jichitaiCd;

    private static final String LOGIN_PATH         = "/login";
    private static final String USER_REGISTER_PATH = "/admin/user-registration";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        boolean isLoginGet = "GET".equalsIgnoreCase(request.getMethod())
        		&& (request.getContextPath() + LOGIN_PATH).equals(request.getRequestURI().replaceAll("/$", ""));

        if (isLoginGet && userRepository.countByJichitaiCd(jichitaiCd) == 0) {
            log.info("ユーザー未登録のため、初期セットアップ画面へリダイレクト: jichitaiCd={}", jichitaiCd);
            response.sendRedirect(request.getContextPath() + USER_REGISTER_PATH);
            return;
        }

        filterChain.doFilter(request, response);
    }
}

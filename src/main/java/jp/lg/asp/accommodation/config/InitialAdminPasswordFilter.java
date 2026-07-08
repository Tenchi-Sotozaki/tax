package jp.lg.asp.accommodation.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jp.lg.asp.accommodation.entity.UserId;
import jp.lg.asp.accommodation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialAdminPasswordFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Value("${app.jichitai.code}")
    private String jichitaiCd;

    private static final String ADMIN_ID = "管理者";
    private static final String LOGIN_PATH = "/login";
    private static final String PASSWORD_CHANGE_PATH = "/admin/password-change";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        String path = uri.startsWith(contextPath) ? uri.substring(contextPath.length()) : uri;

        boolean isLoginGet = "GET".equalsIgnoreCase(request.getMethod()) && LOGIN_PATH.equals(path);

        if (isLoginGet) {
            UserId pk = new UserId();
            pk.setJichitaiCd(jichitaiCd);
            pk.setId(ADMIN_ID);
            boolean stillInitial = userRepository.findById(pk)
                    .map(u -> "1".equals(u.getInitialPasswordFlg()))
                    .orElse(false);
            if (stillInitial) {
                log.info("管理者アカウントが初期パスワードのままのため、初回パスワード設定画面へ誘導: jichitaiCd={}", jichitaiCd);
                response.sendRedirect(contextPath + PASSWORD_CHANGE_PATH);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
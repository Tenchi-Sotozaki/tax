package jp.lg.asp.accommodation.config;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    // 強制リダイレクトの対象外にするパス（無限リダイレクト防止）
    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/admin/password-change", "/login", "/logout", "/error"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        // ↓コンテキストパスを取り除いた、アプリ内での相対パスに直す
        String path = uri.startsWith(contextPath) ? uri.substring(contextPath.length()) : uri;

        boolean isStatic = path.startsWith("/css/") || path.startsWith("/js/")
                || path.startsWith("/images/") || path.startsWith("/fonts/") || path.startsWith("/webjars/");

        if (!isStatic && !ALLOWED_PATHS.contains(path)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof AppUserDetails userDetails
                    && userDetails.isMustChangePassword()) {
                response.sendRedirect(contextPath + "/admin/password-change");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
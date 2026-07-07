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

        String uri = request.getRequestURI();
        boolean isStatic = uri.startsWith("/css/") || uri.startsWith("/js/")
        	    || uri.startsWith("/images/") || uri.startsWith("/fonts/") || uri.startsWith("/webjars/");

        if (!isStatic && !ALLOWED_PATHS.contains(uri)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof AppUserDetails userDetails
                    && userDetails.isMustChangePassword()) {
                response.sendRedirect(request.getContextPath() + "/admin/password-change");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
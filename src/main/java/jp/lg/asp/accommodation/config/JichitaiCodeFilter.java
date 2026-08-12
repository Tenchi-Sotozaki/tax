package jp.lg.asp.accommodation.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JichitaiCodeFilter extends OncePerRequestFilter {

    private static final String PARAM_NAME = "jichitaiCd";
    private static final String SESSION_KEY = "jichitaiCd";
    public static final String COOKIE_NAME = "jichitaiCd";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String jichitaiCd = request.getParameter(PARAM_NAME);

        if (jichitaiCd != null && !jichitaiCd.isBlank()) {
            request.getSession().setAttribute(SESSION_KEY, jichitaiCd);
            Cookie cookie = new Cookie(COOKIE_NAME, jichitaiCd);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            response.addCookie(cookie);
            
			if (request.getUserPrincipal() == null) {
				response.sendRedirect(request.getContextPath() + "/login");
				return;
			}
        }

        filterChain.doFilter(request, response);
    }
}
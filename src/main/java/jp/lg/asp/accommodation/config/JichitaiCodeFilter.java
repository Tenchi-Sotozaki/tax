package jp.lg.asp.accommodation.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JichitaiCodeFilter extends OncePerRequestFilter {

    private static final String PARAM_NAME = "jichitaiCd";
    private static final String SESSION_KEY = "jichitaiCd";

    private final AdminSeedService adminSeedService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String jichitaiCd = request.getParameter(PARAM_NAME);

        if (jichitaiCd != null && !jichitaiCd.isBlank()) {
            request.getSession().setAttribute(SESSION_KEY, jichitaiCd);
            adminSeedService.seedIfNeeded(jichitaiCd); // ★追加：初回アクセス時にAdminアカウントを作成
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
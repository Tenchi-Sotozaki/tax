package jp.lg.asp.accommodation.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JichitaiCodeFilter extends OncePerRequestFilter {

    private static final String PARAM_NAME = "jichitaiCd";
    private static final String SESSION_KEY = "jichitaiCd";
    public static final String COOKIE_NAME = "jichitaiCd";

    private final JichitaiRepository jichitaiRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String param = request.getParameter(PARAM_NAME);

        if (param != null && !param.isBlank()) {
            String jichitaiCd = toJichitaiCd(param);
            if (jichitaiCd != null) {
                request.getSession().setAttribute(SESSION_KEY, jichitaiCd);
                Cookie cookie = new Cookie(COOKIE_NAME, jichitaiCd);
                cookie.setPath("/");
                cookie.setHttpOnly(true);
                response.addCookie(cookie);
            } else {
                log.warn("該当する自治体が存在しません: param={}", param);
            }
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * クエリパラメータ文字列を自治体コードに変換する。
     *
     * @param param クエリパラメータで受け取った文字列（m_jichitai.param）
     * @return 自治体コード。該当する自治体が存在しない場合は null
     */
    private String toJichitaiCd(String param) {
        return jichitaiRepository.findFirstByParam(param)
                .map(Jichitai::getJichitaiCd)
                .map(String::strip)
                .orElse(null);
    }
}

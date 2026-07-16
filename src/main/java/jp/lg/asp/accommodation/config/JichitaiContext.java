package jp.lg.asp.accommodation.config;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

@Component
public class JichitaiContext {

    private final HttpServletRequest request;

    public JichitaiContext(HttpServletRequest request) {
        this.request = request;
    }

    public String getJichitaiCd() {
        Object value = request.getSession().getAttribute("jichitaiCd");
        return value != null ? value.toString() : null;
    }
}
package jp.lg.asp.accommodation.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;

/**
 * セッション情報の取得・保存を管理するユーティリティクラス
 */
public class SessionHelper {

    public static final String SHITEI_GASSAN_KEY = "selectedShiteiGassan";

    private SessionHelper() {}

    public static void saveShiteiGassan(HttpSession session, ShiteiGassanSearchDto dto) {
        session.setAttribute(SHITEI_GASSAN_KEY, dto);
    }

    public static ShiteiGassanSearchDto getShiteiGassan(HttpSession session) {
        if (session == null) return null;
        return (ShiteiGassanSearchDto) session.getAttribute(SHITEI_GASSAN_KEY);
    }

    public static ShiteiGassanSearchDto getShiteiGassan(HttpServletRequest request) {
        return getShiteiGassan(request.getSession(false));
    }
}

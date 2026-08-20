package jp.lg.asp.accommodation.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;

/**
 * セッション情報の取得・保存を管理するユーティリティクラス
 */
public class SessionHelper {

	public static final String SHITEI_GASSAN_KEY = "selectedShiteiGassan";

	private SessionHelper() {
	}

	public static void saveShiteiGassan(HttpSession session, ShiteiGassanSearchDto dto) {
		session.setAttribute(SHITEI_GASSAN_KEY, dto);
	}

	public static ShiteiGassanSearchDto getShiteiGassan(HttpSession session) {
		if (session == null)
			return null;
		return (ShiteiGassanSearchDto) session.getAttribute(SHITEI_GASSAN_KEY);
	}

	public static ShiteiGassanSearchDto getShiteiGassan(HttpServletRequest request) {
		return getShiteiGassan(request.getSession(false));
	}

	/** セッションから指定番号を取得する。未設定の場合は null を返す。 */
	public static String getShiteiNo(HttpSession session) {
		ShiteiGassanSearchDto dto = getShiteiGassan(session);
		return (dto != null && dto.getShiteiNo() != null && !dto.getShiteiNo().isEmpty())
				? dto.getShiteiNo()
				: null;
	}

	/** セッションから合算指定番号を取得する。未設定の場合は null を返す。 */
	public static String getGassanShiteiNo(HttpSession session) {
		ShiteiGassanSearchDto dto = getShiteiGassan(session);
		return (dto != null && dto.getGassanShiteiNo() != null && !dto.getGassanShiteiNo().isEmpty())
				? dto.getGassanShiteiNo()
				: null;
	}

	/** 現在のリクエストのセッションから指定番号または合算指定番号を取得する。リクエストが存在しない場合は null を返す。 */
	public static String getShiteiNoOrGassanShiteiNoFromCurrentRequest() {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attrs == null)
			return null;
		String shiteiNo = getShiteiNo(attrs.getRequest().getSession(false));
		return shiteiNo == null ? getGassanShiteiNo(attrs.getRequest().getSession(false)) : shiteiNo;
	}
}

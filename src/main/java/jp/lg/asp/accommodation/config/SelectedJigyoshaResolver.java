package jp.lg.asp.accommodation.config;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpSession;
import jp.lg.asp.accommodation.controller.ShiteiGassanSearchApiController;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;

/**
 * 事業者（特別徴収義務者／合算申請事業者）の選択状態を解決する。
 *
 * 事業者の指定が必要な画面では、セッションに選択情報を保持していない場合、
 * または保持している事業者の種別が画面の要求と異なる場合に指定モーダルで選択させる。
 */
@Component
public class SelectedJigyoshaResolver {

	/** 画面が要求する事業者の種別 */
	public enum Kind {
		/** 特別徴収義務者 */
		TOKUGIMU,
		/** 合算申請事業者 */
		GASSAN,
		/** どちらでも可 */
		ANY
	}

	/**
	 * セッションに保持している選択情報を取得する。
	 */
	public ShiteiGassanSearchDto getSelected(HttpSession session) {
		if (session == null) {
			return null;
		}
		return (ShiteiGassanSearchDto) session.getAttribute(ShiteiGassanSearchApiController.SESSION_KEY);
	}

	/**
	 * 画面が要求する種別を満たしているかを判定する。
	 *
	 * 合算指定番号を保持していれば合算申請事業者、保持していなければ特別徴収義務者として扱う。
	 */
	public boolean matches(HttpSession session, Kind kind) {
		ShiteiGassanSearchDto selected = getSelected(session);
		if (selected == null || !StringUtils.hasText(selected.getShiteiNo())) {
			return false;
		}
		boolean isGassan = StringUtils.hasText(selected.getGassanShiteiNo());
		return switch (kind) {
			case TOKUGIMU -> !isGassan;
			case GASSAN -> isGassan;
			case ANY -> true;
		};
	}

	/**
	 * 画面が要求する種別を満たしている場合のみ指定番号を返す。満たしていない場合はnull。
	 */
	public String resolveShiteiNo(HttpSession session, Kind kind) {
		return matches(session, kind) ? getSelected(session).getShiteiNo() : null;
	}

	/**
	 * 画面が要求する種別を満たしている場合のみ合算指定番号を返す。満たしていない場合はnull。
	 */
	public String resolveGassanShiteiNo(HttpSession session) {
		return matches(session, Kind.GASSAN) ? getSelected(session).getGassanShiteiNo() : null;
	}
}

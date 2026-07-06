package jp.lg.asp.accommodation.service;

import java.util.List;

import jp.lg.asp.accommodation.dto.OpeLogViewDto;
import jp.lg.asp.accommodation.entity.Screen;

public interface OpeLogViewService {

	/**
	 * 検索条件に合致する操作ログ一覧を返す。
	 */
	List<OpeLogViewDto> search(OpeLogViewDto form);

	/**
	 * 自治体に紐づく画面マスタ一覧を返す（プルダウン用）。
	 */
	List<Screen> findAllScreens();
}

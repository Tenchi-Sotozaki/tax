package jp.lg.asp.accommodation.service;

import java.util.List;

import jp.lg.asp.accommodation.dto.RptLogViewDto;
import jp.lg.asp.accommodation.entity.Reports;

public interface RptLogViewService {

	/**
	 * 検索条件に合致する帳票ログ一覧を返す。
	 */
	List<RptLogViewDto> search(RptLogViewDto form);

	/**
	 * 自治体に紐づく帳票マスタ一覧を返す（プルダウン用）。
	 */
	List<Reports> findAllReports();
}

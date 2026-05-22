package jp.lg.asp.accommodation.service;

import java.util.List;

import jp.lg.asp.accommodation.dto.ShoreikinDto;

/**
 * 特別徴収事務交付金 Service インターフェース。
 */
public interface ShoreikinService {

	/** 検索条件に合致する一覧を返す */
	List<ShoreikinDto> search(ShoreikinDto form);

	/** 指定年度の交付金を一括算出する */
	int bulkCalculate(String nendo);
}

package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.ShoreikinBulkDto;

/**
 * 特別徴収事務交付金一括算出 Service インターフェース
 */
public interface ShoreikinBulkService {

	/**
	 * 指定年度の交付金を一括算出する
	 * @param dto 一括算出条件
	 * @return 処理結果を含むDTO
	 */
	ShoreikinBulkDto executeBulkSanshutsu(ShoreikinBulkDto dto);

	/**
	 * 指定年度の算出対象件数を取得する
	 * @param nendo 対象年度
	 * @return 対象件数
	 */
	int getTargetCount(String nendo);
}
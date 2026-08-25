package jp.lg.asp.accommodation.service;

import java.math.BigDecimal;
import java.util.List;

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
	 * 交付率取得
	 */
	List<BigDecimal> findKofuRitsuList(String jichitaiCd, int year);
}
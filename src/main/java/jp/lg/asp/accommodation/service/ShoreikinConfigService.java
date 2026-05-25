package jp.lg.asp.accommodation.service;

import java.util.List;

import jp.lg.asp.accommodation.dto.ShoreikinConfigDto;

/**
 * 特別徴収事務交付金照会／登録／編集 Service インターフェース
 * 仕様書：特別徴収事務交付金照会・登録・編集.csv に基づく実装
 */
public interface ShoreikinConfigService {

	/**
	 * 指定番号と年度で交付金情報を取得
	 * @param shiteiNo 指定番号
	 * @param nendo 年度
	 * @return 交付金情報
	 */
	ShoreikinConfigDto getShoreikin(String shiteiNo, String nendo);

	/**
	 * 交付金情報を登録
	 * @param dto 交付金情報
	 * @return 登録結果
	 */
	ShoreikinConfigDto createShoreikin(ShoreikinConfigDto dto);

	/**
	 * 交付金情報を更新
	 * @param dto 交付金情報
	 * @return 更新結果
	 */
	ShoreikinConfigDto updateShoreikin(ShoreikinConfigDto dto);

	/**
	 * 交付金を算出（納入税額、交付率、交付額を自動計算）
	 * @param dto 交付金情報
	 * @return 算出結果を含むDTO
	 */
	ShoreikinConfigDto calculateShoreikin(ShoreikinConfigDto dto);

	/**
	 * 指定年度の賦課情報から納入税額を算出
	 * @param shiteiNo 指定番号
	 * @param nendo 年度
	 * @return 納入税額
	 */
	Long calculateKofuZeigaku(String shiteiNo, String nendo);
}
package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.FurikomiKozaDto;

/**
 * 振込先口座照会／登録／編集 Service インターフェース
 * 仕様書：振込先口座照会・登録・編集.csv に基づく実装
 */
public interface FurikomiKozaService {

	/**
	 * 指定番号で振込先口座情報を取得
	 * @param shiteiNo 指定番号
	 * @return 振込先口座情報
	 */
	FurikomiKozaDto getFurikomiKoza(String shiteiNo);

	/**
	 * 振込先口座情報を登録
	 * @param dto 振込先口座情報
	 * @return 登録結果
	 */
	FurikomiKozaDto createFurikomiKoza(FurikomiKozaDto dto);

	/**
	 * 振込先口座情報を更新
	 * @param dto 振込先口座情報
	 * @return 更新結果
	 */
	FurikomiKozaDto updateFurikomiKoza(FurikomiKozaDto dto);
}
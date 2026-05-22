package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.FurikomiKozaDto;

public interface FurikomiKozaService {

	/**
	 * 振込先口座情報を取得する
	 * 
	 * @param shiteiNo 指定番号
	 * @return 振込先口座情報DTO
	 */
	FurikomiKozaDto getFurikomiKoza(String shiteiNo);

	/**
	 * 振込先口座情報を登録する
	 * 
	 * @param dto 振込先口座情報DTO
	 */
	void registerFurikomiKoza(FurikomiKozaDto dto);

	/**
	 * 振込先口座情報を更新する
	 * 
	 * @param dto 振込先口座情報DTO
	 */
	void updateFurikomiKoza(FurikomiKozaDto dto);
}
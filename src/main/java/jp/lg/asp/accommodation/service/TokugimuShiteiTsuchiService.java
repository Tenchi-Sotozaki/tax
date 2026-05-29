package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.TokugimuShiteiTsuchiDto;

/**
 * 特別徴収義務者指定通知 Service インターフェース
 */
public interface TokugimuShiteiTsuchiService {

	/**
	 * 指定番号に基づいて特別徴収義務者情報を取得
	 * 
	 * @param shiteiNo 指定番号
	 * @return 特別徴収義務者指定通知DTO
	 */
	TokugimuShiteiTsuchiDto getTokugimuInfo(String shiteiNo);
}
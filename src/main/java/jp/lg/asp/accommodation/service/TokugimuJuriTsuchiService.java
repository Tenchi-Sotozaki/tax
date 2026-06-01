package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.TokugimuJuriTsuchiDto;

/**
 * 特別徴収義務者申請受理通知 Service インターフェース
 */
public interface TokugimuJuriTsuchiService {

	/**
	 * 指定番号に基づいて特別徴収義務者情報を取得
	 * 
	 * @param shiteiNo 指定番号
	 * @return 特別徴収義務者申請受理通知DTO
	 */
	TokugimuJuriTsuchiDto getTokugimuInfo(String shiteiNo);
}
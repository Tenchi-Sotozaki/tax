package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.GassanNonyuTsuchiDto;

/**
 * 合算申告納入承認通知書 Service インターフェース
 */
public interface GassanNonyuTsuchiService {

	/**
	 * 指定番号に基づいて合算申告納入承認通知書の情報を取得
	 * 
	 * @param shiteiNo 指定番号
	 * @return 合算申告納入承認通知書DTO
	 */
	GassanNonyuTsuchiDto getGassanNonyuTsuchiInfo(String shiteiNo);
}

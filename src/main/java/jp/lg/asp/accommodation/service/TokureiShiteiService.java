package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.TokureiShiteiDto;

/**
 * 納入申告書の提出期限等の特例適用者指定通知 Service インターフェース
 */
public interface TokureiShiteiService {

	/**
	 * 指定番号に基づいて特別徴収義務者情報を取得
	 * 
	 * @param shiteiNo 指定番号
	 * @return 特例適用者指定通知DTO
	 */
	TokureiShiteiDto getTokugimuInfo(String shiteiNo);
}

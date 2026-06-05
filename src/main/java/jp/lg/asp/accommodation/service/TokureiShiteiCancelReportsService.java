package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.TokureiShiteiCancelDto;

/**
 * 納入申告書の提出期限等の特例適用者指定取消通知帳票 Service インターフェース
 */
public interface TokureiShiteiCancelReportsService {

	/**
	 * 特例適用者指定取消通知書をPDF形式で生成
	 * 
	 * @param dto 特例適用者指定取消通知DTO
	 * @return PDF バイト配列
	 */
	byte[] generateTsuchiPdf(TokureiShiteiCancelDto dto);
}

package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.TokugimuShiteiTsuchiDto;

/**
 * 特別徴収義務者指定通知帳票 Service インターフェース
 */
public interface TokugimuShiteiTsuchiReportsService {

	/**
	 * 特別徴収義務者指定通知書をPDF形式で生成
	 * 
	 * @param dto 特別徴収義務者指定通知DTO
	 * @return PDF バイト配列
	 */
	byte[] generateTsuchiPdf(TokugimuShiteiTsuchiDto dto);
}
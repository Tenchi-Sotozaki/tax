package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.TokugimuJuriTsuchiDto;

/**
 * 特別徴収義務者申請受理通知帳票 Service インターフェース
 */
public interface TokugimuJuriTsuchiReportsService {

	/**
	 * 特別徴収義務者申請受理通知書をPDF形式で生成
	 * 
	 * @param dto 特別徴収義務者申請受理通知DTO
	 * @return PDF バイト配列
	 */
	byte[] generateTsuchiPdf(TokugimuJuriTsuchiDto dto);
}
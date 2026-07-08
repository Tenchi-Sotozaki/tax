package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.GassanNonyuTsuchiDto;

/**
 * 合算申告納入承認通知書帳票 Service インターフェース
 */
public interface GassanNonyuTsuchiReportsService {

	/**
	 * 合算申告納入承認通知書をPDF形式で生成
	 * 
	 * @param dto 合算申告納入承認通知書DTO
	 * @return PDF バイト配列
	 */
	byte[] generateTsuchiPdf(GassanNonyuTsuchiDto dto);
}

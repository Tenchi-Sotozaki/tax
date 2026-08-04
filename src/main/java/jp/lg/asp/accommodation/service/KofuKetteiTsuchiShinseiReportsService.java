package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.KofuKetteiTsuchiShinseiDto;

/**
 * 宿泊税特別徴収事務交付金決定通知書・交付申請書 ReportsService インターフェース
 */
public interface KofuKetteiTsuchiShinseiReportsService {
	
	/**
     * 決定通知書・交付申請書を生成
     * @param dto 帳票DTO
     * @return PDFデータ
     */
    byte[] generatekofuKetteiTsuchiShinseiPdf(KofuKetteiTsuchiShinseiDto dto);
}
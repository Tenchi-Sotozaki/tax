package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.KofuKetteiTsuchiDto;

/**
 * 宿泊税特別徴収事務交付金交付決定通知書 ReportsService インターフェース
 */
public interface KofuKetteiTsuchiReportsService {

    /**
     * 交付決定通知書PDFを生成
     * @param dto 帳票DTO
     * @return PDFデータ
     */
    byte[] generateKofuKetteiTsuchiPdf(KofuKetteiTsuchiDto dto);
}
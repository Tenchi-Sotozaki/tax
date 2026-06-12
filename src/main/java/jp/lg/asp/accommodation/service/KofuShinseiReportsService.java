package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.KofuShinseiDto;

/**
 * 宿泊税特別徴収事務交付金交付申請書 ReportsService インターフェース
 */
public interface KofuShinseiReportsService {

    /**
     * 交付申請書PDFを生成
     * @param dto 帳票DTO
     * @return PDFデータ
     */
    byte[] generateKofuShinseiPdf(KofuShinseiDto dto);
}
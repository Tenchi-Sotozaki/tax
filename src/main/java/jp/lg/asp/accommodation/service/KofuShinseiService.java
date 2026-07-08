package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.KofuShinseiDto;

/**
 * 宿泊税特別徴収事務交付金交付申請書 Service インターフェース
 */
public interface KofuShinseiService {

    /**
     * 帳票データを取得
     * @param shiteiNo 指定番号
     * @return 帳票DTO
     */
    KofuShinseiDto getReportData(String shiteiNo);
    
    /**
     * 帳票データを取得（年度指定）
     * @param shiteiNo 指定番号
     * @param nendo 年度
     * @return 帳票DTO
     */
    KofuShinseiDto getReportData(String shiteiNo, String nendo);
}
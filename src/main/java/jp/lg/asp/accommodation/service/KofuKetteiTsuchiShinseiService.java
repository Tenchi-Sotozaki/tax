package jp.lg.asp.accommodation.service;

import java.util.List;

import jp.lg.asp.accommodation.dto.KofuKetteiTsuchiShinseiDto;

/**
 * 宿泊税特別徴収事務交付金交付申請書 Service インターフェース
 */
public interface KofuKetteiTsuchiShinseiService {

    /**
     * 帳票データを取得
     * @param shiteiNo 指定番号
     * @return 帳票DTO
     */
    KofuKetteiTsuchiShinseiDto getReportData(String shiteiNo);
    
    /**
     * 帳票データを取得（年度指定）
     * @param shiteiNo 指定番号
     * @param nendo 年度
     * @return 帳票DTO
     */
    KofuKetteiTsuchiShinseiDto getReportData(String shiteiNo, String nendo);

    /**
     * 全特別徴収義務者の帳票データを取得（一括発行用）
     * @param nendo 年度
     * @return 帳票DTOリスト
     */
    List<KofuKetteiTsuchiShinseiDto> getAllReportData(String nendo);
}
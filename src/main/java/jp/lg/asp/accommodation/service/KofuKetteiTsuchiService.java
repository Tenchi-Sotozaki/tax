package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.KofuKetteiTsuchiDto;

/**
 * 宿泊税特別徴収事務交付金交付決定通知書 Service インターフェース
 */
public interface KofuKetteiTsuchiService {

    /**
     * 帳票データを取得
     * @param shiteiNo 指定番号
     * @return 帳票DTO
     */
    KofuKetteiTsuchiDto getReportData(String shiteiNo);
}
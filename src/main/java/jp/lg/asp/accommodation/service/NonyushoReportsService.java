package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.NonyushoDto;
import jp.lg.asp.accommodation.dto.NonyushoDataResponse;

/**
 * 納入書レポート Service
 */
public interface NonyushoReportsService {

    /**
     * 納入書PDF生成
     * @param dto 納入書データ
     * @return PDFバイト配列
     */
    byte[] generateNonyushoPdf(NonyushoDto dto);
    
    /**
     * 納入書動的データ取得
     * @param shiteiNo 指定番号
     * @param nendo 年度
     * @return 動的データ
     */
    NonyushoDataResponse getNonyushoData(String shiteiNo, String nendo);
}
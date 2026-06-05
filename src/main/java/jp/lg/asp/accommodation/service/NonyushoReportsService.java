package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.NonyushoDto;

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
}
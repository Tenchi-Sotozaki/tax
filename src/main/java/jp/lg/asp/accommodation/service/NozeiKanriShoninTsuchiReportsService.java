package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.NozeiKanriShoninTsuchiDto;

/**
 * 納税管理人承認(不承認)通知書PDF生成 Service
 */
public interface NozeiKanriShoninTsuchiReportsService {

    /**
     * 納税管理人承認(不承認)通知書PDFを生成
     * @param dto 通知書データ
     * @return PDFバイト配列
     */
    byte[] generateTsuchiPdf(NozeiKanriShoninTsuchiDto dto);
}
package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.NozeiKanrininNinteiDto;

/**
 * 納税管理人選任免除認定（不認定）通知書PDF生成 Service
 */
public interface NozeiKanrininNinteiReportsService {

    /**
     * 通知書PDFを生成
     * @param dto 通知書DTO
     * @return PDFバイト配列
     */
    byte[] generatePdf(NozeiKanrininNinteiDto dto);
}

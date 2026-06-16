package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.NozeiKanrininNinteiDto;

/**
 * 納税管理人選任免除認定（不認定）通知書 Service
 */
public interface NozeiKanrininNinteiService {

    /**
     * 指定番号から通知書の情報を取得
     * @param shiteiNo 指定番号
     * @return DTO
     */
    NozeiKanrininNinteiDto getNinteiInfo(String shiteiNo);
}

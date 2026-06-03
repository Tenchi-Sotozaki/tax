package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.NozeiKanriShoninTsuchiDto;

/**
 * 納税管理人承認(不承認)通知書 Service
 */
public interface NozeiKanriShoninTsuchiService {

    /**
     * 指定番号から納税管理人承認通知書の情報を取得
     * @param shiteiNo 指定番号
     * @return 納税管理人承認通知書DTO
     */
    NozeiKanriShoninTsuchiDto getNozeiKanriInfo(String shiteiNo);
}
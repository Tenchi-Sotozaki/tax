package jp.lg.asp.accommodation.service;

import java.util.List;

import jp.lg.asp.accommodation.dto.SofusakiCsvDto;

public interface SofusakiCsvService {

    /** 印刷済み帳票ログに紐づく送付先一覧を返す */
    List<SofusakiCsvDto> findAll();

    /** 指定したDTOリストをCSV文字列に変換する */
    String toCsvString(List<SofusakiCsvDto> items);
}

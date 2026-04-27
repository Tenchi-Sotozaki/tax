package jp.lg.asp.accommodation.service;

import java.util.List;

import jp.lg.asp.accommodation.dto.NozeiShukiDto;

public interface NozeiShukiService {

    /** 自治体コードに紐づく有効な納税周期マスタを取得する */
    List<NozeiShukiDto> findAll();
}

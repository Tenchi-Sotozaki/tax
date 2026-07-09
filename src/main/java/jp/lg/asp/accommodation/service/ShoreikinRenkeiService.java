package jp.lg.asp.accommodation.service;

import java.util.List;

import jp.lg.asp.accommodation.dto.ShoreikinRenkeiDto;

public interface ShoreikinRenkeiService {

    List<ShoreikinRenkeiDto> search(String jichitaiCd, String nendo, String shiteiNo, String name, String nameMatchType);

    List<ShoreikinRenkeiDto> findByKeys(String jichitaiCd, List<ShoreikinRenkeiDto.Key> keys);
}
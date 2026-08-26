package jp.lg.asp.accommodation.service;

import java.util.List;

import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;

public interface ShiteiGassanSearchApiService {

    List<ShiteiGassanSearchDto> searchByShiteiNo(String jichitaiCd, String shiteiNo);

    List<ShiteiGassanSearchDto> searchByGassanShiteiNo(String jichitaiCd, String gassanShiteiNo);

    List<ShiteiGassanSearchDto> searchByName(String jichitaiCd, String name, String matchType);

    List<ShiteiGassanSearchDto> searchByShisetsuName(String jichitaiCd, String shisetsuName, String matchType);

    List<ShiteiGassanSearchDto> searchByKojinNo(String jichitaiCd, String kojinNo);

    List<ShiteiGassanSearchDto> searchByHojinNo(String jichitaiCd, String hojinNo);
}

package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.ShiteiGassanConfigDto;
import jp.lg.asp.accommodation.entity.Jichitai;

public interface ShiteiGassanConfigService {

    Jichitai findById(String jichitaiCd);

    void save(String jichitaiCd, ShiteiGassanConfigDto dto);
}

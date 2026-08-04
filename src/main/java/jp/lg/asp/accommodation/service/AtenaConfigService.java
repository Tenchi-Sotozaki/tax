package jp.lg.asp.accommodation.service;

import java.math.BigDecimal;

import jp.lg.asp.accommodation.entity.Atena;

public interface AtenaConfigService {

    Atena findByAtenaNo(String jichitaiCd, BigDecimal atenaNo);

    Atena register(Atena atena, String jichitaiCd);

    Atena update(Atena atena, String jichitaiCd);
}

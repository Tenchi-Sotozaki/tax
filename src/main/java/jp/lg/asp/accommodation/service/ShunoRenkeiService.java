package jp.lg.asp.accommodation.service;

import java.time.LocalDate;
import java.util.List;

import jp.lg.asp.accommodation.dto.ShunoDto;

public interface ShunoRenkeiService {

    List<ShunoDto> search(String jichitaiCd, LocalDate shinkokuFrom, LocalDate shinkokuTo,
            String taishoMonth, String shiteiNo, String name);

    List<ShunoDto> findByKeys(String jichitaiCd, List<ShunoDto.Key> keys);

}

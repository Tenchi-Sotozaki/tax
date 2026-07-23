package jp.lg.asp.accommodation.service;

import java.util.List;

import jp.lg.asp.accommodation.dto.HolidayConfigForm;

public interface HolidayConfigService {

    HolidayConfigForm findByNendo(String nendo);

    void save(HolidayConfigForm form);

    List<String> findNendoList();
}

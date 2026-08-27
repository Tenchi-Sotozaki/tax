package jp.lg.asp.accommodation.service;
import java.util.List;

import jp.lg.asp.accommodation.dto.NozeiShukiDto;
import jp.lg.asp.accommodation.dto.TekiyoNozeiShukiForm;

public interface TekiyoNozeiShukiService {

    List<NozeiShukiDto> getNozeiShukiOptions();

    TekiyoNozeiShukiForm getByShiteiNo(String shiteiNo);

    void save(String shiteiNo, TekiyoNozeiShukiForm form);

    void delete(String shiteiNo);
}

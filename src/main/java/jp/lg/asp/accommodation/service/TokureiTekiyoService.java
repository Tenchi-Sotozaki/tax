package jp.lg.asp.accommodation.service;

import java.util.List;

import jp.lg.asp.accommodation.dto.TokureiTekiyoForm;
import jp.lg.asp.accommodation.dto.TokureiTekiyoHistoryDto;

public interface TokureiTekiyoService {

    List<TokureiTekiyoHistoryDto> getHistories(String shiteiNo);

    TokureiTekiyoForm getForView(String shiteiNo, Integer rno);

    TokureiTekiyoForm getForRegister(String shiteiNo);

    void save(String shiteiNo, TokureiTekiyoForm form);

    void update(String shiteiNo, Integer rno, TokureiTekiyoForm form);

    void delete(String shiteiNo, Integer rno);
}

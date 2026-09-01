package jp.lg.asp.accommodation.service;

import java.util.List;

import jp.lg.asp.accommodation.dto.TokureiTekiyoForm;
import jp.lg.asp.accommodation.dto.TokureiTekiyoHistoryDto;

public interface TokureiTekiyoService {

    List<TokureiTekiyoHistoryDto> getHistories();

    TokureiTekiyoForm getForView(Integer rno);

    TokureiTekiyoForm getForRegister();

    void save(TokureiTekiyoForm form);

    void update(Integer rno, TokureiTekiyoForm form);

    void delete(Integer rno);
}

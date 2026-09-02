package jp.lg.asp.accommodation.service;

import java.util.List;

import jp.lg.asp.accommodation.dto.JichitaiListSearchForm;
import jp.lg.asp.accommodation.entity.Jichitai;

public interface JichitaiListService {

    List<Jichitai> search(JichitaiListSearchForm form);
}

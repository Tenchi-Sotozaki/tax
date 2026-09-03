package jp.lg.asp.accommodation.service;

import java.util.List;

import jp.lg.asp.accommodation.dto.AtenaDaichoItem;
import jp.lg.asp.accommodation.dto.AtenaSearchForm;

public interface AtenaService {
	
	List<AtenaDaichoItem> searchDaicho(String jichitaiCd, AtenaSearchForm searchForm, boolean searched);
}

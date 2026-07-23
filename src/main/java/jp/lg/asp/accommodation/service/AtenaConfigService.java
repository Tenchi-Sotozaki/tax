package jp.lg.asp.accommodation.service;

import java.math.BigDecimal;

import jp.lg.asp.accommodation.dto.AtenaConfigForm;

public interface AtenaConfigService {

	AtenaConfigForm findByAtenaNo(BigDecimal atenaNo);

	void register(AtenaConfigForm form);

	void update(BigDecimal atenaNo, AtenaConfigForm form);
}

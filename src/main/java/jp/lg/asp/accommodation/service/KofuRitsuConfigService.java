package jp.lg.asp.accommodation.service;

import java.math.BigDecimal;
import java.util.List;

import jp.lg.asp.accommodation.dto.KofuRitsuConfigDto;
import jp.lg.asp.accommodation.entity.KofuRitsu;

public interface KofuRitsuConfigService {

	KofuRitsu findCurrent();

	List<KofuRitsu> findAll();

	KofuRitsu findByRno(BigDecimal rno);

	void register(KofuRitsuConfigDto dto);

	void update(BigDecimal rno, KofuRitsuConfigDto dto);
}

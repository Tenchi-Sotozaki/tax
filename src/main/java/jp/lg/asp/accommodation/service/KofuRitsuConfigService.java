package jp.lg.asp.accommodation.service;

import java.util.List;

import jp.lg.asp.accommodation.dto.KofuRitsuConfigDto;
import jp.lg.asp.accommodation.entity.KofuRitsu;

public interface KofuRitsuConfigService {

	KofuRitsu findCurrent();

	List<KofuRitsu> findAll();

	void register(KofuRitsuConfigDto dto);
}

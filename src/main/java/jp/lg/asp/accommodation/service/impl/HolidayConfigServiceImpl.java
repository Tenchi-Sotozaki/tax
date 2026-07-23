package jp.lg.asp.accommodation.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.HolidayConfigForm;
import jp.lg.asp.accommodation.entity.Holiday;
import jp.lg.asp.accommodation.repository.HolidayRepository;
import jp.lg.asp.accommodation.service.HolidayConfigService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HolidayConfigServiceImpl implements HolidayConfigService {

	private final HolidayRepository holidayRepository;
	private final JichitaiContext jichitaiContext;

	@Override
	@Transactional(readOnly = true)
	public HolidayConfigForm findByNendo(String nendo) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		//List<Holiday> holidays = holidayRepository.findByJichitaiCdAndNendoOrderByHolidayDt(jichitaiCd, nendo);
		List<Holiday> holidays = new ArrayList<>();
		HolidayConfigForm form = new HolidayConfigForm();
		form.setNendo(nendo);
		form.setHolidayDts(holidays.stream().map(Holiday::getHolidayDt).toList());
		return form;
	}

	@Override
	@Transactional
	public void save(HolidayConfigForm form) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		holidayRepository.deleteByJichitaiCdAndNendo(jichitaiCd, form.getNendo());
		if (form.getHolidayDts() == null)
			return;
		for (String dt : form.getHolidayDts()) {
			Holiday h = new Holiday();
			h.setJichitaiCd(jichitaiCd);
			h.setNendo(form.getNendo());
			h.setHolidayDt(dt);
			holidayRepository.save(h);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<String> findNendoList() {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		return List.of("2027");
		//return holidayRepository.findAll().stream()
		//			.filter(h -> h.getJichitaiCd().equals(jichitaiCd))
		//		.map(Holiday::getNendo)
		//	.distinct()
		//.sorted()
		//.toList();
	}
}

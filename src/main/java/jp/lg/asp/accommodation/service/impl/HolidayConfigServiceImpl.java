package jp.lg.asp.accommodation.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.HolidayConfigForm;
import jp.lg.asp.accommodation.entity.Kyugyobi;
import jp.lg.asp.accommodation.repository.HolidayRepository;
import jp.lg.asp.accommodation.service.HolidayConfigService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HolidayConfigServiceImpl implements HolidayConfigService {

	private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final HolidayRepository holidayRepository;
	private final JichitaiContext jichitaiContext;

	@Override
	@Transactional(readOnly = true)
	public HolidayConfigForm findByNendo(String nendo) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		List<Kyugyobi> list = holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi(jichitaiCd, nendo);
		HolidayConfigForm form = new HolidayConfigForm();
		form.setNendo(nendo);
		form.setHolidayDts(list.stream().map(k -> k.getKyugyobi().format(FMT)).toList());
		return form;
	}

	@Override
	@Transactional
	public void save(HolidayConfigForm form) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		holidayRepository.deleteByJichitaiCdAndNen(jichitaiCd, form.getNendo());
		if (form.getHolidayDts() == null)
			return;
		for (String dt : form.getHolidayDts()) {
			Kyugyobi k = new Kyugyobi();
			k.setJichitaiCd(jichitaiCd);
			k.setNen(form.getNendo());
			k.setKyugyobi(LocalDate.parse(dt, FMT));
			holidayRepository.save(k);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<String> findNendoList() {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		return holidayRepository.findDistinctNenByJichitaiCd(jichitaiCd);
	}

	@Override
	@Transactional(readOnly = true)
	public List<String> getInitialHolidays(String nen) {
		List<Kyugyobi> template = holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi("99999", nen);
		return template.stream().map(k -> k.getKyugyobi().format(FMT)).toList();
	}
}

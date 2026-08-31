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

	private static final String SENTINEL_CD = "99999";
	private static final LocalDate SENTINEL_DATE = LocalDate.of(1, 1, 1);

	@Override
	@Transactional(readOnly = true)
	public HolidayConfigForm findByNendo(String nendo) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		boolean registered = holidayRepository.existsByJichitaiCdAndNen(jichitaiCd, nendo);
		List<Kyugyobi> list;
		if (registered) {
			list = holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi(jichitaiCd, nendo);
		} else {
			list = holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi(SENTINEL_CD, nendo);
		}
		HolidayConfigForm form = new HolidayConfigForm();
		form.setNendo(nendo);
		form.setHolidayDts(list.stream()
				.filter(k -> !SENTINEL_DATE.equals(k.getKyugyobi()))
				.map(k -> k.getKyugyobi().format(FMT)).toList());
		return form;
	}

	@Override
	@Transactional
	public void save(HolidayConfigForm form) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		holidayRepository.deleteByJichitaiCdAndNen(jichitaiCd, form.getNendo());
		if (form.getHolidayDts() == null || form.getHolidayDts().isEmpty()) {
			// 空登録時は番兵レコードを挿入して「登録済み」を記録
			Kyugyobi sentinel = new Kyugyobi();
			sentinel.setJichitaiCd(jichitaiCd);
			sentinel.setNen(form.getNendo());
			sentinel.setKyugyobi(SENTINEL_DATE);
			holidayRepository.save(sentinel);
			return;
		}
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
		List<String> jichitaiList = holidayRepository.findDistinctNenByJichitaiCd(jichitaiCd);
		List<String> sentinelList = holidayRepository.findDistinctNenByJichitaiCd(SENTINEL_CD);
		return java.util.stream.Stream.concat(jichitaiList.stream(), sentinelList.stream())
				.distinct()
				.sorted()
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<String> getInitialHolidays(String nen) {
		List<Kyugyobi> template = holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi("99999", nen);
		return template.stream().map(k -> k.getKyugyobi().format(FMT)).toList();
	}
}

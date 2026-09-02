package jp.lg.asp.accommodation.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.RptStatusListItem;
import jp.lg.asp.accommodation.dto.RptStatusSearchForm;
import jp.lg.asp.accommodation.entity.Reports;
import jp.lg.asp.accommodation.entity.RptStatus;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.ReportsRepository;
import jp.lg.asp.accommodation.repository.RptStatusRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.RptStatusService;
import jp.lg.asp.accommodation.util.HashUtil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RptStatusServiceImpl implements RptStatusService {

	private final JichitaiContext jichitaiContext;
	private final ReportsRepository reportsRepository;
	private final RptStatusRepository rptStatusRepository;
	private final TokugimuRepository tokugimuRepository;
	private final HashUtil hashUtil;

	@Override
	public List<Reports> findAllReports() {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		return reportsRepository.findAll().stream()
				.filter(r -> jichitaiCd.equals(r.getJichitaiCd()))
				.collect(Collectors.toList());
	}

	@Override
	public List<RptStatusListItem> search(RptStatusSearchForm form) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();

		String searchKojinNo = org.springframework.util.StringUtils.hasText(form.getKojinNo())
				? hashUtil.sha256(form.getKojinNo()) : form.getKojinNo();

		List<Tokugimu> tokugimuList = tokugimuRepository.findBySearchConditions(
				jichitaiCd,
				form.getShiteiNo(),
				form.getName(),
				toLikePattern(form.getName(), form.getNameMatchType()),
				form.getShisetsuName(),
				toLikePattern(form.getShisetsuName(), form.getShisetsuNameMatchType()),
				"999",
				searchKojinNo,
				form.getHojinNo());

		List<RptStatus> rptStatusList = rptStatusRepository.findByJichitaiCd(jichitaiCd);

		Map<String, Map<String, LocalDateTime>> statusMap = new LinkedHashMap<>();
		for (RptStatus s : rptStatusList) {
			statusMap.computeIfAbsent(s.getShiteiNo(), k -> new LinkedHashMap<>())
					.put(s.getRptId(), s.getCreateDt());
		}

		// 検索条件に合致した特別徴収義務者はすべて返す。
		// 帳票の発行実績が無い場合は空のマップを持たせ、画面側で「-」を表示する。
		List<RptStatusListItem> result = new ArrayList<>();
		for (Tokugimu t : tokugimuList) {
			RptStatusListItem item = new RptStatusListItem();
			item.setShiteiNo(t.getShiteiNo());
			item.setName(t.getAtena() != null ? t.getAtena().getName() : "");
			item.setShisetsuName(t.getShisetsuName());
			item.setRptStatusMap(statusMap.getOrDefault(t.getShiteiNo(), Map.of()));
			result.add(item);
		}
		return result;
	}

	private String toLikePattern(String value, String matchType) {
		if (value == null || value.isBlank())
			return null;
		return switch (matchType) {
		case "prefix" -> value + "%";
		case "exact" -> value;
		default -> "%" + value + "%"; // partial
		};
	}
}

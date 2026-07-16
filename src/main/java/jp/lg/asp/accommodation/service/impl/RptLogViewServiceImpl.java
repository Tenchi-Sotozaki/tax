package jp.lg.asp.accommodation.service.impl;

import java.util.ArrayList;
import java.util.List;

import jp.lg.asp.accommodation.config.JichitaiContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.RptLogViewDto;
import jp.lg.asp.accommodation.entity.Reports;
import jp.lg.asp.accommodation.entity.ReportsLog;
import jp.lg.asp.accommodation.repository.ReportsLogRepository;
import jp.lg.asp.accommodation.repository.ReportsRepository;
import jp.lg.asp.accommodation.service.RptLogViewService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RptLogViewServiceImpl implements RptLogViewService {

	private final ReportsRepository reportsRepository;
	private final ReportsLogRepository reportsLogRepository;

	private final JichitaiContext jichitaiContext;

	@Override
	@Transactional(readOnly = true)
	public List<RptLogViewDto> search(RptLogViewDto form) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		List<ReportsLog> logList = reportsLogRepository.findByConditions(
				jichitaiCd,
				form.getRptId(),
				form.getSousa(),
				form.getOpeUser(),
				form.getOpeDtFrom(),
				form.getOpeDtTo(),
				form.getShiteiNo());

		List<Reports> reportsList = reportsRepository.findAll();

		List<RptLogViewDto> results = new ArrayList<>();
		for (ReportsLog log : logList) {
			RptLogViewDto dto = new RptLogViewDto();
			dto.setSeq(log.getSeq());
			dto.setRptId(log.getRptId());
			dto.setRptName(resolveRptName(reportsList, log.getRptId()));
			dto.setSousa(log.getSousa());
			dto.setSousaName(ReportsConstants.resolveSousaName(log.getSousa()));
			dto.setOpeUser(log.getOpeUser());
			dto.setOpeDt(log.getOpeDt());
			dto.setShiteiNo(log.getShiteiNo());
			results.add(dto);
		}
		return results;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Reports> findAllReports() {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		return reportsRepository.findAll().stream()
				.filter(r -> jichitaiCd.equals(r.getJichitaiCd()))
				.toList();
	}

	private String resolveRptName(List<Reports> reportsList, String rptId) {
		if (rptId == null)
			return "";
		return reportsList.stream()
				.filter(r -> rptId.strip().equals(r.getRptId().strip()))
				.map(Reports::getRptName)
				.findFirst()
				.orElse(rptId);
	}
}

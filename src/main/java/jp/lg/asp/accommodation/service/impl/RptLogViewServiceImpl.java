package jp.lg.asp.accommodation.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

	private static final String SOUSA_PDF = "1";
	private static final String SOUSA_PREVIEW = "2";
	private static final String SOUSA_PRINT = "3";

	private final ReportsRepository reportsRepository;
	private final ReportsLogRepository reportsLogRepository;

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	@Override
	@Transactional(readOnly = true)
	public List<RptLogViewDto> search(RptLogViewDto form) {
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
			dto.setSousaName(resolveSousaName(log.getSousa()));
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
		return reportsRepository.findAll().stream()
				.filter(r -> jichitaiCd.equals(r.getJichitaiCd()))
				.toList();
	}

	private String resolveRptName(List<Reports> reportsList, String rptId) {
		if (rptId == null) return "";
		return reportsList.stream()
				.filter(r -> rptId.strip().equals(r.getRptId().strip()))
				.map(Reports::getRptName)
				.findFirst()
				.orElse(rptId);
	}

	private String resolveSousaName(String sousa) {
		if (sousa == null) return "";
		return switch (sousa.strip()) {
			case SOUSA_PDF -> "PDF";
			case SOUSA_PREVIEW -> "プレビュー";
			case SOUSA_PRINT -> "印刷";
			default -> sousa;
		};
	}
}

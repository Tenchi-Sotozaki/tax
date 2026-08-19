package jp.lg.asp.accommodation.service.impl;
import jp.lg.asp.accommodation.config.JichitaiContext;

import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.ReportsDef;
import jp.lg.asp.accommodation.entity.ReportsDefId;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.ReportsDefRepository;
import jp.lg.asp.accommodation.service.ReportsCommonService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportsCommonServiceImpl implements ReportsCommonService {

	private final JichitaiRepository jichitaiRepository;
	private final ReportsDefRepository reportsDefRepository;

	private final JichitaiContext jichitaiContext;

	@Override
	public Jichitai getJichitaiInfo() {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		return jichitaiRepository.findById(jichitaiCd).orElse(null);
	}

	@Override
	public String getReportsDefText(String Id) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		if (Id.isEmpty()) {
			return "";
		}
		ReportsDefId id = new ReportsDefId();
		id.setJichitaiCd(jichitaiCd);
		id.setId(Id);

		ReportsDef entity = reportsDefRepository.findById(id).orElse(null);
		if (entity == null || !ReportsConstants.KBN_TEXT.equals(entity.getKbn())
				|| entity.getDefText() == null || entity.getDefText().isEmpty()) {
			return "";
		}
		return entity.getDefText();
	}

	@Override
	public byte[] getReportsDefData(String Id) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		if (Id.isEmpty()) {
			return new byte[0];
		}
		ReportsDefId id = new ReportsDefId();
		id.setJichitaiCd(jichitaiCd);
		id.setId(Id);

		ReportsDef entity = reportsDefRepository.findById(id).orElse(null);
		if (entity == null || !entity.getKbn().equals(ReportsConstants.KBN_DATA) || entity.getDefData() == null) {
			return new byte[0];
		}
		return entity.getDefData();
	}

}
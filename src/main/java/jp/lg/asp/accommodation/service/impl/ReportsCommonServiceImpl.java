package jp.lg.asp.accommodation.service.impl;

import org.springframework.beans.factory.annotation.Value;
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

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	@Override
	public Jichitai getJichitaiInfo() {
		return jichitaiRepository.findById(jichitaiCd).orElse(null);
	}

	@Override
	public String getReportsDefText(String Id) {
		if (Id.isEmpty()) {
			return "";
		}
		ReportsDefId id = new ReportsDefId();
		id.setJichitaiCd(jichitaiCd);
		id.setId(Id);

		ReportsDef entity = reportsDefRepository.findById(id).orElse(null);
		if (entity == null || !entity.getKbn().equals(ReportsConstants.KBN_TEXT) || entity.getDefText().isEmpty()) {
			return "";
		}
		return entity.getDefText();
	}

	@Override
	public byte[] getReportsDefData(String Id) {
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
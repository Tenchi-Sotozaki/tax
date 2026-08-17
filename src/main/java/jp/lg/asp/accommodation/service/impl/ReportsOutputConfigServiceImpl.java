package jp.lg.asp.accommodation.service.impl;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.constant.ReportsConstants.reportsOutputFiled;
import jp.lg.asp.accommodation.entity.ReportsDef;
import jp.lg.asp.accommodation.entity.ReportsDefId;
import jp.lg.asp.accommodation.repository.ReportsDefRepository;
import jp.lg.asp.accommodation.service.ReportsOutputConfigService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportsOutputConfigServiceImpl implements ReportsOutputConfigService {

	private final ReportsDefRepository reportsDefRepository;

	@Override
	public Map<reportsOutputFiled, String> getDefTextMap(String jichitaiCd) {
		Map<reportsOutputFiled, String> map = new LinkedHashMap<>();
		for (reportsOutputFiled field : reportsOutputFiled.values()) {
			ReportsDefId id = new ReportsDefId();
			id.setJichitaiCd(jichitaiCd);
			id.setId(field.getId());
			reportsDefRepository.findById(id).ifPresentOrElse(
				def -> map.put(field, def.getDefText()),
				() -> map.put(field, "")
			);
		}
		return map;
	}

	@Override
	@Transactional
	public void saveDefText(String jichitaiCd, String userId, Map<String, String> defTextMap) {
		LocalDateTime now = LocalDateTime.now();
		for (reportsOutputFiled field : reportsOutputFiled.values()) {
			String defText = defTextMap.getOrDefault(field.getId(), "");
			ReportsDefId id = new ReportsDefId();
			id.setJichitaiCd(jichitaiCd);
			id.setId(field.getId());
			ReportsDef def = reportsDefRepository.findById(id).orElseGet(() -> {
				ReportsDef newDef = new ReportsDef();
				newDef.setJichitaiCd(jichitaiCd);
				newDef.setId(field.getId());
				newDef.setAddDt(now);
				newDef.setAddUser(userId);
				return newDef;
			});
			// 既存行の区分が誤っていると帳票側で読み出せないため、毎回設定し直す
			def.setKbn(ReportsConstants.KBN_TEXT);
			def.setDefText(defText);
			def.setUpdDt(now);
			def.setUpdUser(userId);
			reportsDefRepository.save(def);
		}
	}
}

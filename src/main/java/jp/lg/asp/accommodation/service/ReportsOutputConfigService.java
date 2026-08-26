package jp.lg.asp.accommodation.service;

import java.util.Map;

import jp.lg.asp.accommodation.constant.ReportsConstants.ReportsOutputField;

public interface ReportsOutputConfigService {

	Map<ReportsOutputField, String> getDefTextMap(String jichitaiCd);

	void saveDefText(String jichitaiCd, String userId, Map<String, String> defTextMap);
}

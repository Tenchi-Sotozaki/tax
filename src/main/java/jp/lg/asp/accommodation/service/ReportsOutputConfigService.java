package jp.lg.asp.accommodation.service;

import java.util.Map;

import jp.lg.asp.accommodation.constant.ReportsConstants.reportsOutputFiled;

public interface ReportsOutputConfigService {

	Map<reportsOutputFiled, String> getDefTextMap(String jichitaiCd);

	void saveDefText(String jichitaiCd, String userId, Map<String, String> defTextMap);
}

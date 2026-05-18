package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.constant.FukaConstants;

public interface FukaCommonService {

	/**
	 * t_fuka / t_fuka_uchi を保存する。
	 *
	 * @param shiteiNo   指定番号
	 * @param taishoYm   行為年月（yyyyMM形式）
	 * @param teishutsuYmd 提出年月日文字列
	 * @param fukaKbn    賦課区分（{@link FukaConstants}）
	 * @param dataRow    eLTAX CSVデータ行
	 * @param yoshikiMap 様式定義マップ（No.→項目名称）
	 */
	void saveFuka(String shiteiNo, String taishoYm, String teishutsuYmd,
			FukaConstants fukaKbn, String[] dataRow, java.util.Map<Integer, String> yoshikiMap,
			String taishoYmPrefix);

	/**
	 * 都道府県税額を取得する
	 * 
	 * @param shukuhakuRyokin 宿泊料金
	 * @param taishoYm 対象年月
	 * @return 都道府県税額
	 */
	public long getKenZeigaku(Long shukuhakuRyokin, String taishoYm);
}

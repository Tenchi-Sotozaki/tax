package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.entity.Jichitai;

public interface ReportsCommonService {

	/**
	 * 自治体情報を取得
	 * 
	 * @return 自治体情報
	 */
	Jichitai getJichitaiInfo();

	/** 
	 * 帳票定義テキストを取得
	 * 
	 * @param Id 帳票ID
	 * @return 帳票定義テキスト
	 */
	String getReportsDefText(String Id);

	/** 
	 * 帳票定義データを取得
	 * 
	 * @param Id 帳票ID
	 * @return 帳票定義データ
	 */
	byte[] getReportsDefData(String Id);
}

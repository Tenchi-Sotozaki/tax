package jp.lg.asp.accommodation.dto;

import lombok.Data;

/**
 * 宿泊税特別徴収事務交付金交付申請書DTO
 */
@Data
public class KofuShinseiDto {

	/** 指定番号 */
	private String shiteiNo;

	/** 年度 */
	private String nendo;

	/** 発行様式 */
	private String hakkoYoshiki;

	/** 市区町村名 */
	private String cityName;

	/** 条令 */
	private String jorei;

	/** 施設住所 */
	private String shisetsuJusho;

	/** 施設名称 */
	private String shisetsuName;

	/** 申告納入金額 */
	private String nonyugaku;

	/** 交付申請額 */
	private String kofugaku;

	/** 交付条件文 */
	private String kofuJoken;

	/** 特別徴収義務者名 */
	private String tokuName;

	/** 公印 */
	private byte[] koin;

	public void setHakkoYmd(String hakkoYmd) {
		// TODO 自動生成されたメソッド・スタブ

	}
}
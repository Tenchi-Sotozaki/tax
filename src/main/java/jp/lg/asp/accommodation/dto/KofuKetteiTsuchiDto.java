package jp.lg.asp.accommodation.dto;

import lombok.Data;

/**
 * 宿泊税特別徴収事務交付金交付決定通知書DTO
 */
@Data
public class KofuKetteiTsuchiDto {

	/** 発行様式 */
	private String hakkoYoshiki;

	/** 特別徴収義務者名 */
	private String tokugimuName;

	/** 発行年月日 */
	private String hakkoYmd;

	/** 市区町村名 */
	private String cityName;

	/** 条令 */
	private String hakkoJorei;

	/** 施設住所 */
	private String shisetsuJusho;

	/** 施設名称 */
	private String shisetsuName;

	/** 指定番号 */
	private String shiteiNo;

	/** 交付決定額 */
	private String kofugaku;

	/** 交付年月日 */
	private String kofuYmd;
}
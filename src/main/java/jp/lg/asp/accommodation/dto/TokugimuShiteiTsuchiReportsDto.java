package jp.lg.asp.accommodation.dto;

import lombok.Data;

/**
 * 特別徴収義務者指定通知帳票DTO
 */
@Data
public class TokugimuShiteiTsuchiReportsDto {

	/** 指定番号 */
	private String shiteiNo;

	/** 発行日 */
	private String hakkoYmd;

	/** 指定の理由 */
	private String riyu;

	/** 特別徴収義務者名 */
	private String tokuName;

	/** 特別徴収義務者住所 */
	private String tokuJusho;

	/** 施設名称 */
	private String shisetsuName;

	/** 施設所在地 */
	private String shisetsuJusho;

	/** 市区町村名 */
	private String cityName;

	/** 条令 */
	private String jorei;

	/** 市区町村 */
	private String city;
	
	/** 公印画像ファイル */
	private byte[] koin;
}
package jp.lg.asp.accommodation.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

/**
 * 特別徴収義務者申請受理通知DTO
 */
@Data
public class TokugimuJuriTsuchiDto {

	/** 指定番号 */
	private String shiteiNo;

	/** 発行日 */
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate hakkoYmd;

	/** 特別徴収義務者名 */
	private String tokuName;

	/** 特別徴収義務者住所 */
	private String tokuJusho;

	/** 特別徴収義務者住所（郵便番号なし） */
	private String tokuJushoWithoutYubin;

	/** 施設名称 */
	private String shisetsuName;

	/** 施設所在地 */
	private String shisetsuJusho;

	/** 市区町村名 */
	private String cityName;

	/** 条例 */
	private String jorei;

	/** 備考 */
	private String biko;
	
	/** 公印 */
	private byte[] koin;
}
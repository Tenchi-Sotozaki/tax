package jp.lg.asp.accommodation.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

/**
 * 特別徴収義務者指定通知DTO
 */
@Data
public class TokugimuShiteiTsuchiDto {

	/** 指定番号 */
	private String shiteiNo;

	/** 発行日 */
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate hakkoYmd;

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
}
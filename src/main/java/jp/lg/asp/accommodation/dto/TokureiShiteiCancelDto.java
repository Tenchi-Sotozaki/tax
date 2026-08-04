package jp.lg.asp.accommodation.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

/**
 * 納入申告書の提出期限等の特例適用者指定取消通知DTO
 */
@Data
public class TokureiShiteiCancelDto {

	/** 指定番号 */
	private String shiteiNo;

	/** 発行日 */
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate hakkoYmd;

	/** 適用年月日 */
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate tekiyoYmd;

	/** 取消理由 */
	private String riyu;

	/** 特別徴収義務者名 */
	private String tokuName;

	/** 特別徴収義務者住所 */
	private String tokuJusho;

	/** 施設名称 */
	private String shisetsuName;

	/** 施設所在地 */
	private String shisetsuJusho;

	/** 市区町村 */
	private String city;

	/** 条令 */
	private String jorei;
	
	/** 備考 */
	private String biko;
	
	/** 公印 */
	private byte[] koin;
}

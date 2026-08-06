package jp.lg.asp.accommodation.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

/**
 * 合算申告納入承認通知書DTO
 */
@Data
public class GassanNonyuTsuchiDto {

	/** 指定番号 */
	private String shiteiNo;

	/** 合算指定番号 */
	private String gassanShiteiNo;

	/** 特別徴収義務者名 */
	private String tokuName;

	/** 特別徴収義務者住所 */
	private String tokuJusho;

	/** 適用開始年月日 */
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate tekiyoStYmd;

	/** 発行日 */
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate hakkoYmd;

	/** 備考 */
	private String biko;

	/** 市区町村 */
	private String city;

	/** 条令 */
	private String jorei;
	
	/** 公印 */
	private byte[] koin;

	/** 納入期限 */
	private String nonyuKigen;
}

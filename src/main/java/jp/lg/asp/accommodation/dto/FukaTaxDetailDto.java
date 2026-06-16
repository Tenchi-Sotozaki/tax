package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class FukaTaxDetailDto {
	private String label;
	private BigDecimal zeiritsuSeq;
	private BigDecimal taxRate;
	private BigDecimal taxKenRate;

	private Long ryokinSogaku;
	private Long hakusu;
	private Long ryokin;
	private Long zeigaku;
	private Long cityZeigaku;
	private Long kenZeigaku;

	//	private List<Long> taxCategoryAmounts; // 区分ごとの課税対象料金リスト

}
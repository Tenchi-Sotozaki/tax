package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class FukaTaxDetailDto {
	private String label;
	private BigDecimal taxRate;
	private Integer kazeiRyokin;

	private Long stayCount;
	private Long taxAmount;

	private BigDecimal zeiritsuSeq;

	private List<Long> taxCategoryAmounts; // 区分ごとの課税対象料金リスト
	private Long menjoRyokin; // 課税対象外(免除)の宿泊料金

}
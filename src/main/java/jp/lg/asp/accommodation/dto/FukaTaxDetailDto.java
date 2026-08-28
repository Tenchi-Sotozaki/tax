package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import lombok.Data;

@Data
public class FukaTaxDetailDto {
	private String label;
	private BigDecimal zeiritsuSeq;
	private BigDecimal taxRate;
	private BigDecimal taxKenRate;

	private Long ryokinSogaku;
	@Digits(integer = 8, fraction = 0, message = "8桁以内で入力してください")
	private Long hakusu;
	private Long ryokin;
	@Digits(integer = 13, fraction = 0, message = "13桁以内で入力してください")
	private Long zeigaku;
	private Long cityZeigaku;
	private Long kenZeigaku;
}
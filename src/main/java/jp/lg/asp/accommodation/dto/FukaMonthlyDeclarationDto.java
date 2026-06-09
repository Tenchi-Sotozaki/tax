package jp.lg.asp.accommodation.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import lombok.Data;

/**
 * 月ごとの申告情報を保持するDTO
 */
@Data
public class FukaMonthlyDeclarationDto {

	@NotBlank(message = "※納入年月を選択してください")
	private String paymentYearMonth;

	// リストの中身まで検証を Cascade（連鎖：親の検証時に子も自動的に検証すること）させる
	@Valid
	private List<FukaTaxDetailDto> taxDetails = new ArrayList<>();

	private Long exemptStayCount;

	private Long totalStayCount;

	private Long totalPaymentAmount;

	// ==========================================
	// ✨ 【新規追加】定率制用のフィールド
	// ==========================================

	// 課税対象宿泊料金
	private Long kazeiRyokin;

	// 税額（定率計算後）
	private Long teiritsuZeigaku;

	// 課税対象外宿泊料金
	private Long menjoRyokin;

	// 総宿泊料金
	private Long totalRyokin;

	// 総宿泊数（統計用）
	private Long totalHakusu;

	// 納入金額（1ヶ月分の最終的な納付額）
	private Long nonyuKingaku;
	
}
package jp.lg.asp.accommodation.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;

import lombok.Data;

/**
 * 月ごとの申告情報を保持するDTO
 */
@Data
public class FukaMonthlyDeclarationDto {

	private String paymentYearMonth;

	// リストの中身まで検証を Cascade（連鎖：親の検証時に子も自動的に検証すること）させる
	@Valid
	private List<FukaTaxDetailDto> taxDetails = new ArrayList<>();

	// 免除料金
	private Long exemptRyokin;

	// 免除宿泊数
	private Long exemptStayCount;

	// 宿泊数合計
	private Long totalStayCount;

	// 宿泊料金合計
	private Long kazeiRyokin;

	// 宿泊税額合計
	private Long totalPaymentAmount;

	// 市区町村税額合計
	private Long totalCityZeigaku;

	// 都道府県税額合計
	private Long totalKenZeigaku;

	// ==========================================
	// ✨ 【新規追加】定率制用のフィールド
	// ==========================================

	// 総宿泊数（統計用）
	private Long totalHakusu;

	// 納入金額（1ヶ月分の最終的な納付額）
	private Long nonyuKingaku;

}
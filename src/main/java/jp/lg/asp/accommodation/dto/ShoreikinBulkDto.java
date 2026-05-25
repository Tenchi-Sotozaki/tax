package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 特別徴収事務交付金一括算出 DTO
 * 一括算出処理の入力値保持および表示用クラス
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShoreikinBulkDto {

	// ========== 対象条件エリア ==========

	/** No.1 交付金年度 */
	@NotBlank(message = "交付金年度は必須入力です")
	@Pattern(regexp = "^[0-9]{4}$", message = "交付金年度は4桁の数字で入力してください")
	private String nendo;

	/** 交付率 */
	@NotNull(message = "交付率は必須入力です")
	@DecimalMin(value = "0.00", message = "交付率は0.00以上で入力してください")
	@Digits(integer = 5, fraction = 2, message = "交付率は整数部5桁、小数部2桁以内で入力してください")
	private BigDecimal kofuRitsu;

	/** 算出済みを含む */
	private boolean includeCalculated;

	// ========== 処理結果表示用 ==========

	/** 処理対象件数 */
	private int targetCount;

	/** 処理成功件数 */
	private int successCount;

	/** 処理失敗件数 */
	private int failureCount;

	/** スキップ件数 */
	private int skipCount;

	/** 処理結果メッセージ */
	private String resultMessage;

	/** 処理実行フラグ */
	private boolean executed = false;
}
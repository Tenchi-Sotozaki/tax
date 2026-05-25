package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;

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
	private String nendo;

	/** 交付率 */
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
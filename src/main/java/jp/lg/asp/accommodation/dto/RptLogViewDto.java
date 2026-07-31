package jp.lg.asp.accommodation.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 帳票ログ照会 検索フォーム兼表示用DTO
 */
@Data
public class RptLogViewDto {

	// ===== 検索条件 =====

	/** 帳票ID（セレクトボックス選択値） */
	private String rptId;

	/** 操作（1:PDF、2:プレビュー、3:印刷） */
	private String sousa;

	/** 印刷者（前方一致） */
	private String opeUser;

	/** 印刷日時 FROM */
	@NotBlank(message = "印刷日時（FROM）は必須です")
	private String opeDtFrom;

	/** 印刷日時 TO */
	@NotBlank(message = "印刷日時（TO）は必須です")
	private String opeDtTo;

	/** 指定番号（完全一致） */
	private String shiteiNo;

	// ===== 一覧表示用 =====

	/** 管理番号 */
	private Long seq;

	/** 帳票名 */
	private String rptName;

	/** 操作名称（PDF/プレビュー/印刷） */
	private String sousaName;

	/** 印刷日時 */
	private LocalDateTime opeDt;
}

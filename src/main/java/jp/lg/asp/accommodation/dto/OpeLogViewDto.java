package jp.lg.asp.accommodation.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 操作ログ照会 検索フォーム兼表示用DTO
 */
@Data
public class OpeLogViewDto {

	// ===== 検索条件 =====

	/** 画面ID（セレクトボックス選択値） */
	private String screenId;

	/** 操作（部分一致） */
	private String sousa;

	/** 操作者（前方一致） */
	private String opeUser;

	/** 操作日時 FROM */
	private String opeDtFrom;

	/** 操作日時 TO */
	private String opeDtTo;

	/** 任意項目／パラメータ（部分一致） */
	private String param;

	// ===== 一覧表示用 =====

	/** 管理番号 */
	private Long seq;

	/** 画面名 */
	private String screenName;

	/** 操作日時 */
	private LocalDateTime opeDt;
}

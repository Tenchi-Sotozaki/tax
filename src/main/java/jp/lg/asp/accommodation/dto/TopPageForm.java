package jp.lg.asp.accommodation.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

/**
 * トップページ編集の入力内容
 */
@Data
public class TopPageForm {

	/** 掲載内容（タグ付きテキスト） */
	private String contents;

	/** 掲載開始日 */
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate keisaiStYmd;

	/** 掲載終了日。未入力の場合は終了日なしとして扱う */
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate keisaiEdYmd;
}

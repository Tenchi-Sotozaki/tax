package jp.lg.asp.accommodation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GassanDaichoSearchForm {

	// 合算指定番号
	private String gassanShiteiNo;

	// 代表指定番号  
	private String shiteiNo;

	// 氏名/名称
	private String name;
	private String nameMatchType = "partial";

	// 現在のページ
	private int page = 0;

	// 1ページあたりの表示件数
	private int pageSize = 10;
}
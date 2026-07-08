package jp.lg.asp.accommodation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ZeiritsuSearchForm {

	/** 適用時期FROM (yyyyMM) */
	private String tekiyoYmFrom;

	/** 適用時期TO (yyyyMM) */
	private String tekiyoYmTo;

	/** 賦課方式 (空=すべて, 1=定額, 2=定率) */
	private String fukaKbn;

	/** 対象区分 (空=すべて, 1=市区町村, 2=都道府県) */
	private String taishoKbn;
}

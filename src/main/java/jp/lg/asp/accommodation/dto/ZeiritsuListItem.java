package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ZeiritsuListItem {

	private BigDecimal seq;
	private String fukaKbn;
	private String fukaKbnName;
	private String tekiyoStYm;
	private String tekiyoEdYm;
	private String taishoKbn;
	private String taishoKbnName;
}

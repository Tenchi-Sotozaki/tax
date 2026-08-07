package jp.lg.asp.accommodation.dto;

import lombok.Data;

@Data
public class RptStatusSearchForm {
    private String shiteiNo;
    private String name;
	private String nameMatchType = "partial";
    private String shisetsuName;
	private String shisetsuNameMatchType = "partial";
    private String kojinNo;
    private String hojinNo;
}

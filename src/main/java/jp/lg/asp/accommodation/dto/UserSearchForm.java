package jp.lg.asp.accommodation.dto;

import lombok.Data;

@Data
public class UserSearchForm {
    private String id;
    private String name;
    private String nameMatchType = "partial";
    private String nameKana;
    private String nameKanaMatchType = "partial";
    private String busho;
    private String bushoMatchType = "partial";
    private int page = 0;
    private int pageSize = 10;
}

package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;

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
    /** 権限（未選択の場合は絞り込まない） */
    private BigDecimal roleId;
}

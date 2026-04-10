package jp.lg.asp.accommodation.dto;

import lombok.Data;

@Data
public class CollectorSearchForm {

    /** 登録番号（部分一致） */
    private String registrationNo;

    /** 氏名/名称（部分一致） */
    private String obligorName;

    /** 施設名（部分一致） */
    private String facilityName;

    /** 営業種別（完全一致: hotel / ryokan / simple / minshuku / pension） */
    private String businessType;

    /** 合算対象（完全一致: target / non-target） */
    private String consolidationTarget;
}

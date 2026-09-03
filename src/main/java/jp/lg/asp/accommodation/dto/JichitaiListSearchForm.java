package jp.lg.asp.accommodation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JichitaiListSearchForm {

    private String jichitaiCd;

    private String name;

    private String nameMatchType = "partial";

    private String kbnName;

    private String kbnNameMatchType = "partial";
}

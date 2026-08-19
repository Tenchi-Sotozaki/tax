package jp.lg.asp.accommodation.dto;

import lombok.Data;

@Data
public class AtenaSearchForm {
    private String atenaNo;
    private String name;
    private String nameMatchType = "partial"; // "prefix"=前方一致, "partial"=部分一致, "exact"=完全一致
    private String nameKana;
    private String nameKanaMatchType = "partial";
    private String yubinNo;
    private String jusho;
    private String jushoMatchType = "partial"; // "prefix"=前方一致, "partial"=部分一致, "exact"=完全一致
    private String tel;
    private String kojinNo;
    private String hojinNo;
}

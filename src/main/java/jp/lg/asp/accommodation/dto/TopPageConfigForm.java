package jp.lg.asp.accommodation.dto;

import lombok.Data;

@Data
public class TopPageConfigForm {

    /** "0"=全自治体共有, "1"=自治体カスタマイズ */
    private String kbn = "0";

    /** カスタマイズ選択時の自治体コード */
    private String jichitaiCd;

    private String htmlContent;
}

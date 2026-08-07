package jp.lg.asp.accommodation.dto;

import lombok.Data;

@Data
public class TopPageConfigForm {

    private String seq = "0";

    private String jichitaiCd;
    
    private String title;

    private String htmlContent;    
    
}

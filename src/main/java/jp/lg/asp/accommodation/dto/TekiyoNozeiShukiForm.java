package jp.lg.asp.accommodation.dto;

import java.util.List;

import lombok.Data;

@Data
public class TekiyoNozeiShukiForm {

    private String shiteiNo;
    private String obligorName;
    private String facilityName;
    private boolean edit;

    private String tekiyoStMonth;
    private String tekiyoEdMonth;

    private List<TekiyoNozeiShukiHistoryDto> histories;
}

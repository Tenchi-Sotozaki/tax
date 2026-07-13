package jp.lg.asp.accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TekiyoNozeiShukiHistoryDto {
    private Integer idx;
    private String shukiLabel;
    private String tekiyoStMonth;
    private String tekiyoEdMonth;
}

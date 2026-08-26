package jp.lg.asp.accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokureiTekiyoHistoryDto {
    private Integer idx;
    private String tekiyoStMonth;
    private String tekiyoEdMonth;
}

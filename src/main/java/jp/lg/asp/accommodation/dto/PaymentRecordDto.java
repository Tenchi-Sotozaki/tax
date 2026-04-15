package jp.lg.asp.accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.YearMonth;

@Data
@AllArgsConstructor
public class PaymentRecordDto {
    private String id;
    private YearMonth targetYearMonth;
    private Integer amount;
    private String status; // 済 / 未
}

package jp.lg.asp.accommodation.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.YearMonth;

@Data
public class PaymentLedgerSearchForm {

    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth targetYear;

    private String status; // "" / 済 / 未
}

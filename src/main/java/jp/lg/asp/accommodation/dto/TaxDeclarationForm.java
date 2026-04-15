package jp.lg.asp.accommodation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.YearMonth;

@Data
public class TaxDeclarationForm {

    private String obligorId;

    @NotNull(message = "申告年月は必須です")
    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth targetYearMonth;

    @NotNull(message = "課税対象宿泊数は必須です")
    @Min(value = 0, message = "0以上の値を入力してください")
    private Integer taxableGuestCount;

    @NotNull(message = "設定税額は必須です")
    @Min(value = 0, message = "0以上の値を入力してください")
    private Integer taxRate;

    /** 自動計算値（宿泊数 × 税額）。POSTで受け取るが再計算もする */
    private Integer totalTaxAmount;

    private Boolean isCorrection;

    private String correctionReason;
}

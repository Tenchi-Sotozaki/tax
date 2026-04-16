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

    // ===== 段階1（例: 〜7,000円未満） =====
    @Min(value = 0, message = "0以上の値を入力してください")
    private Integer guestCountTier1 = 0;

    @Min(value = 0, message = "0以上の値を入力してください")
    private Integer taxAmountTier1 = 200;

    // ===== 段階2（例: 7,000円〜15,000円未満） =====
    @Min(value = 0, message = "0以上の値を入力してください")
    private Integer guestCountTier2 = 0;

    @Min(value = 0, message = "0以上の値を入力してください")
    private Integer taxAmountTier2 = 500;

    // ===== 段階3（例: 15,000円以上） =====
    @Min(value = 0, message = "0以上の値を入力してください")
    private Integer guestCountTier3 = 0;

    @Min(value = 0, message = "0以上の値を入力してください")
    private Integer taxAmountTier3 = 1000;

    // ===== 合計（自動計算） =====
    private Integer totalGuestCount = 0;
    private Integer totalTaxAmount  = 0;

    // ===== 後方互換（旧フィールド、削除予定） =====
    @NotNull(message = "課税対象宿泊数は必須です")
    @Min(value = 0, message = "0以上の値を入力してください")
    private Integer taxableGuestCount = 0;

    @NotNull(message = "設定税額は必須です")
    @Min(value = 0, message = "0以上の値を入力してください")
    private Integer taxRate = 200;

    private Boolean isCorrection;
    private String  correctionReason;
}

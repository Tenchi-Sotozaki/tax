package jp.lg.asp.accommodation.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

/**
 * 月ごとの申告情報を保持するDTO
 */
@Data
public class FukaMonthlyDeclarationDto {

    @NotBlank(message = "納入年月を選択してください。")
    private String paymentYearMonth;

    // リストの中身まで検証を Cascade（連鎖：親の検証時に子も自動的に検証すること）させる
    @Valid
    private List<FukaTaxDetailDto> taxDetails = new ArrayList<>();

    private Integer exemptStayCount; 

    // 💡 修正：必須チェック（NotNull）に加え、1以上（Min(1)）を強制する
    @NotNull(message = "総宿泊数を入力してください。")
    @Min(value = 0, message = "総宿泊数は0以上で入力してください。")
    private Integer totalStayCount;

    @NotNull(message = "合計税額を入力してください。")
    @Min(value = 0, message = "合計税額は0以上で入力してください。")
    private Long totalPaymentAmount;
}
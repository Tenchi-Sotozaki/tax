package jp.lg.asp.accommodation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
public class DeclarationRequest {

    @NotBlank(message = "特別徴収義務者IDは必須です")
    private String collectorId;

    @NotBlank(message = "宿泊施設IDは必須です")
    private String facilityId;

    @NotBlank(message = "納入年月は必須です")
    @Pattern(regexp = "^\\d{6}$", message = "納入年月はYYYYMM形式で入力してください")
    private String paymentYearMonth;

    @NotNull(message = "総宿泊数は必須です")
    @Min(value = 0, message = "総宿泊数は0以上で入力してください")
    private Integer totalNights;

    @NotNull(message = "課税対象外宿泊数は必須です")
    @Min(value = 0, message = "課税対象外宿泊数は0以上で入力してください")
    private Integer exemptNights;

    @NotEmpty(message = "税区分明細は1件以上必要です")
    @Valid
    private List<DeclarationDetailRequest> details;
}

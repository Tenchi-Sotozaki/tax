package jp.lg.asp.accommodation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DeclarationDetailRequest {

    @NotBlank(message = "税区分コードは必須です")
    private String taxCategoryCode;

    @NotNull(message = "課税対象宿泊数は必須です")
    @Min(value = 0, message = "課税対象宿泊数は0以上で入力してください")
    private Integer taxableNights;
}

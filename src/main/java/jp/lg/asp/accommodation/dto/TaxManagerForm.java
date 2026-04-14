package jp.lg.asp.accommodation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaxManagerForm {

    // 遷移元から引き継ぐ情報（readonly表示用）
    private Long collectorId;
    private String obligorName;
    private String facilityName;

    // 納税管理人情報
    @NotNull(message = "登録日は必須です")
    private LocalDate registrationDate;

    @NotBlank(message = "住所（所在地）は必須です")
    private String managerAddress;

    @NotBlank(message = "氏名は必須です")
    private String managerName;

    @NotBlank(message = "ふりがなは必須です")
    private String managerNameKana;

    @Pattern(regexp = "^[0-9-]{10,15}$", message = "有効な電話番号を入力してください（例：03-1234-5678）")
    @NotBlank(message = "電話番号は必須です")
    private String managerPhone;

    private boolean exemptionFlag;
    private String exemptionReason;
}

package jp.lg.asp.accommodation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CollectorForm {

    // 編集時のID保持用（新規登録時は null）
    private Long id;

    // ===== 特別徴収義務者情報 =====

    @NotNull(message = "登録日は必須です")
    private LocalDate registrationDate;

    @NotBlank(message = "氏名または名称は必須です")
    @Size(max = 50, message = "50文字以内で入力してください")
    private String obligorName;

    @NotBlank(message = "住所は必須です")
    private String obligorAddress;

    @Pattern(regexp = "^[0-9-]{10,15}$", message = "有効な電話番号を入力してください（例：03-1234-5678）")
    @NotBlank(message = "電話番号は必須です")
    private String obligorPhone;

    private String personalCorporateNumber;

    // ===== 宿泊施設情報 =====

    @NotBlank(message = "所在地は必須です")
    private String facilityAddress;

    @NotBlank(message = "ふりがな名称は必須です")
    private String facilityNameKana;

    @Pattern(regexp = "^[0-9-]{10,15}$", message = "有効な電話番号を入力してください（例：03-1234-5678）")
    @NotBlank(message = "施設の電話番号は必須です")
    private String facilityPhone;

    private Double floorArea;
    private String floors;

    @NotNull(message = "客室数は必須です")
    private Integer roomCount;

    @NotNull(message = "収容人数は必須です")
    private Integer capacity;

    @NotNull(message = "営業開始予定日は必須です")
    private LocalDate businessStartDate;

    // ===== 営業許可等情報 =====

    private String licenseAddress;
    private String licenseNameKana;
    private String licensePhone;

    @NotBlank(message = "営業種別は必須です")
    private String businessType;

    @NotBlank(message = "許可番号は必須です")
    private String licenseNumber;

    // ===== 書類送付先情報 =====

    @NotBlank(message = "書類送付先の住所は必須です")
    private String mailAddress;

    @NotBlank(message = "書類送付先のふりがな氏名は必須です")
    private String mailNameKana;

    @Pattern(regexp = "^[0-9-]{10,15}$", message = "有効な電話番号を入力してください（例：03-1234-5678）")
    @NotBlank(message = "書類送付先の電話番号は必須です")
    private String mailPhone;

    // ===== その他の情報 =====

    private String eltaxApplication;
    private String taxCycle;
    private String remarks;
}

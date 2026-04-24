package jp.lg.asp.accommodation.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class TokugimuForm {

	// 編集時のID保持用（新規登録時は null）
	private Long id;

	// ===== 特別徴収義務者情報 =====

	@NotNull(message = "登録日は必須です")
	private LocalDate registrationDate;

	@NotBlank(message = "氏名または名称は必須です")
	@Size(max = 50, message = "50文字以内で入力してください")
	private String tokugimuName;

	@NotBlank(message = "住所は必須です")
	private String tokugimuAddress;

	@Pattern(regexp = "^[0-9-]{10,15}$", message = "有効な電話番号を入力してください（例：03-1234-5678）")
	@NotBlank(message = "電話番号は必須です")
	private String tokugimuPhone;

	private String personalNumber;
	private String corporateNumber;

	// ===== 宿泊施設情報 =====

	@NotBlank(message = "所在地は必須です")
	private String facilityAddress;

	@NotBlank(message = "ふりがなは必須です")
	private String facilityNameKana;

	@NotBlank(message = "名称は必須です")
	private String facilityName;

	@Pattern(regexp = "^[0-9-]{10,15}$", message = "有効な電話番号を入力してください（例：03-1234-5678）")
	@NotBlank(message = "施設の電話番号は必須です")
	private String facilityPhone;

	@NotNull(message = "延床面積は必須です")
	private Double floorArea;

	@NotBlank(message = "地上階は必須です")
	private String aboveGroundFloor;

	@NotBlank(message = "地下階は必須です")
	private String basementFloor;

	@NotNull(message = "客室数は必須です")
	private Integer roomCount;

	@NotNull(message = "収容人数は必須です")
	private Integer capacity;

	@NotNull(message = "営業開始(予定)日は必須です")
	private LocalDate businessStartDate;

	// ===== 営業許可等情報 =====

	@NotBlank(message = "営業許可の郵便番号は必須です")
	private String licenseAddressNo;

	@NotBlank(message = "営業許可の住所は必須です")
	private String licenseAddress;

	@NotBlank(message = "営業許可のふりがなは必須です")
	private String licenseNameKana;

	@NotBlank(message = "営業許可の氏名は必須です")
	private String licenseName;

	@Pattern(regexp = "^[0-9-]{10,15}$", message = "有効な電話番号を入力してください（例：03-1234-5678）")
	@NotBlank(message = "営業許可の電話番号は必須です")
	private String licensePhone;

	@NotBlank(message = "営業種別は必須です")
	private String businessType;

	@NotBlank(message = "許可番号は必須です")
	private String licenseNumber;

	// ===== 施設所有者情報 =====

	@NotBlank(message = "所有者の郵便番号は必須です")
	private String ownerAddressNo;

	@NotBlank(message = "所有者の住所は必須です")
	private String ownerAddress;

	@NotBlank(message = "所有者のふりがなは必須です")
	private String ownerNameKana;

	@NotBlank(message = "所有者の氏名は必須です")
	private String ownerName;

	@Pattern(regexp = "^[0-9-]{10,15}$", message = "有効な電話番号を入力してください（例：03-1234-5678）")
	@NotBlank(message = "所有者の電話番号は必須です")
	private String ownerPhone;

	// ===== 書類送付先情報 =====

	@NotBlank(message = "書類送付先の郵便番号は必須です")
	private String mailAddressNo;

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

	// ===== 施設営業休止/再開/廃止情報（編集時のみ使用） =====

	private String declarationCategory; // 休止 / 再開 / 廃止
	private LocalDate suspensionStartDate;
	private LocalDate suspensionEndDate;
	private boolean suspensionEndDateUndecided;
	private LocalDate resumptionOrAbolitionDate;
	private String suspensionOrAbolitionReason;
}

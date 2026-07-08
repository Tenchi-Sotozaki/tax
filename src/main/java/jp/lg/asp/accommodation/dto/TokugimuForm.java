package jp.lg.asp.accommodation.dto;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Size;

import org.springframework.util.StringUtils;

import lombok.Data;

@Data
@TokugimuForm.TokugimuValid
public class TokugimuForm {

	private Long id;
	private Long atenaNo;

	// ===== 特別徴収義務者情報 =====
	private LocalDate registrationDate;
	@Size(max = 10)
	private String tokugimuAddressNo;
	private String tokugimuAddress;
	private String name;
	@Size(max = 200)
	private String nameKana;
	private String personalNumber;
	private String corporateNumber;
	private String tokugimuPhone;

	// ===== 宿泊施設情報 =====
	@Size(max = 10)
	private String facilityAddressNo;
	@Size(max = 200)
	private String facilityAddress;
	@Size(max = 200)
	private String facilityName;
	@Size(max = 200)
	private String facilityNameKana;
	@Size(max = 20)
	private String facilityPhone;
	private BigDecimal floorArea;
	private String aboveGroundFloor;
	private String basementFloor;
	private Integer roomCount;
	private Integer capacity;
	private LocalDate businessStartDate;

	// ===== 営業許可等情報 =====
	@Size(max = 10)
	private String licenseAddressNo;
	@Size(max = 200)
	private String licenseAddress;
	@Size(max = 200)
	private String licenseName;
	@Size(max = 200)
	private String licenseNameKana;
	@Size(max = 20)
	private String licensePhone;
	private String businessType;
	@Size(max = 200)
	private String licenseNumber;

	// ===== 施設所有者情報 =====
	@Size(max = 10)
	private String ownerAddressNo;
	@Size(max = 200)
	private String ownerAddress;
	@Size(max = 200)
	private String ownerName;
	@Size(max = 200)
	private String ownerNameKana;
	@Size(max = 20)
	private String ownerPhone;

	// ===== 書類送付先情報 =====
	@Size(max = 10)
	private String mailAddressNo;
	@Size(max = 200)
	private String mailAddress;
	@Size(max = 200)
	private String mailName;
	@Size(max = 200)
	private String mailNameKana;
	@Size(max = 20)
	private String mailPhone;

	// ===== 共同事業者情報 =====
	private boolean kyodoFlg;
	private List<KyodoJigyoshaDto> kyodoList = new ArrayList<>();

	// ===== その他の情報 =====
	private String eltaxUmu;
	private String remarks;

	// ===== 施設営業休止/再開/廃止情報 =====
	private String declarationCategory;
	private LocalDate suspensionStartDate;
	private LocalDate suspensionEndDate;
	private boolean suspensionEndDateUndecided;
	private LocalDate resumptionOrAbolitionDate;
	private String suspensionOrAbolitionReason;

	private String shiteiNo;

	public String getTokugimuYubinNo() {
		return mailAddressNo != null && !mailAddressNo.isBlank() ? mailAddressNo : tokugimuAddressNo;
	}

	// ===== カスタムバリデーションアノテーション =====

	@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@Constraint(validatedBy = TokugimuValidator.class)
	@Documented
	public @interface TokugimuValid {
		String message() default "入力内容に誤りがあります";

		Class<?>[] groups() default {};

		Class<? extends Payload>[] payload() default {};
	}

	// ===== カスタムバリデーター =====

	public static class TokugimuValidator implements ConstraintValidator<TokugimuValid, TokugimuForm> {

		/** HTML表示順に対応したフィールド名→エラーメッセージのマップを返す */
		public static Map<String, String> validate(TokugimuForm f) {
			Map<String, String> errors = new LinkedHashMap<>();

			if (f.getRegistrationDate() == null)
				errors.put("registrationDate", "特別徴収義務者情報の登録日は必須です");
			if (!StringUtils.hasText(f.getTokugimuAddressNo()))
				errors.put("tokugimuAddressNo", "特別徴収義務者情報の郵便番号は必須です");
			if (!StringUtils.hasText(f.getTokugimuAddress()))
				errors.put("tokugimuAddress", "特別徴収義務者情報の住所は必須です");
			if (!StringUtils.hasText(f.getName()))
				errors.put("name", "特別徴収義務者情報の氏名または名称は必須です");
			if (!StringUtils.hasText(f.getNameKana()))
				errors.put("nameKana", "特別徴収義務者情報の氏名(ふりがな)は必須です");
			if (!StringUtils.hasText(f.getTokugimuPhone()))
				errors.put("tokugimuPhone", "特別徴収義務者情報の電話番号は必須です");
			if (!StringUtils.hasText(f.getFacilityName()))
				errors.put("facilityName", "宿泊施設情報の施設名称は必須です");
			if (!StringUtils.hasText(f.getFacilityNameKana()))
				errors.put("facilityNameKana", "宿泊施設情報の施設名称(ふりがな)は必須です");
			if (f.getBusinessStartDate() == null)
				errors.put("businessStartDate", "宿泊施設情報の営業開始(予定)日は必須です");
			if (!StringUtils.hasText(f.getLicenseName()))
				errors.put("licenseName", "営業許可等情報の氏名は必須です");
			if (!StringUtils.hasText(f.getLicenseNameKana()))
				errors.put("licenseNameKana", "営業許可等情報の氏名(ふりがな)は必須です");
			if (!StringUtils.hasText(f.getOwnerName()))
				errors.put("ownerName", "施設所有者情報の氏名は必須です");
			if (!StringUtils.hasText(f.getOwnerNameKana()))
				errors.put("ownerNameKana", "施設所有者情報の氏名(ふりがな)は必須です");
			if (!StringUtils.hasText(f.getMailName()))
				errors.put("mailName", "書類送付先情報の氏名は必須です");
			if (!StringUtils.hasText(f.getMailNameKana()))
				errors.put("mailNameKana", "書類送付先情報の氏名(ふりがな)は必須です");
			if (f.isKyodoFlg()) {
				if (f.getKyodoList().stream().anyMatch(k -> !StringUtils.hasText(k.getKyodoName())))
					errors.put("kyodoName", "共同事業者情報の氏名は必須です");
				if (f.getKyodoList().stream().anyMatch(k -> !StringUtils.hasText(k.getKyodoNameKana())))
					errors.put("kyodoNameKana", "共同事業者情報の氏名(ふりがな)は必須です");
			}

			return errors;
		}

		@Override
		public void initialize(TokugimuValid constraintAnnotation) {
		}

		@Override
		public boolean isValid(TokugimuForm f, ConstraintValidatorContext ctx) {
			ctx.disableDefaultConstraintViolation();
			Map<String, String> errors = validate(f);
			errors.forEach((field, message) -> ctx.buildConstraintViolationWithTemplate(message)
					.addPropertyNode(field)
					.addConstraintViolation());
			return errors.isEmpty();
		}
	}
}

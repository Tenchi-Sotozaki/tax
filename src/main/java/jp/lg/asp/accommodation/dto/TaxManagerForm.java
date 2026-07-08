package jp.lg.asp.accommodation.dto;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;

import lombok.Data;

@Data
@TaxManagerForm.TaxManagerValid
public class TaxManagerForm {

	private Long collectorId;
	private String obligorName;
	private String facilityName;
	private String obligorAtenaNo;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate registrationDate;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate declarationDate;

	private String atenaNo;
	private String managerYubinNo;
	private String managerAddress;
	private String managerName;
	private String managerNameKana;
	private String managerPhone;

	private boolean exemptionFlag;
	private String exemptionReason;

	private boolean edit;
	private String shiteiNo;

	// ===== カスタムバリデーションアノテーション =====

	@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@Constraint(validatedBy = TaxManagerValidator.class)
	@Documented
	public @interface TaxManagerValid {
		String message() default "入力内容に誤りがあります";

		Class<?>[] groups() default {};

		Class<? extends Payload>[] payload() default {};
	}

	// ===== カスタムバリデーター =====

	public static class TaxManagerValidator implements ConstraintValidator<TaxManagerValid, TaxManagerForm> {

		@Override
		public void initialize(TaxManagerValid constraintAnnotation) {
		}

		public static java.util.Map<String, String> validate(TaxManagerForm form) {
			java.util.Map<String, String> errors = new java.util.LinkedHashMap<>();
			if (form.getRegistrationDate() == null)
				errors.put("registrationDate", "登録日は必須です");
			if (form.getDeclarationDate() == null)
				errors.put("declarationDate", "申告日は必須です");
			if (form.isExemptionFlag()) {
				if (!StringUtils.hasText(form.getExemptionReason()))
					errors.put("exemptionReason", "選任免除理由は必須です");
			} else {
				form.setExemptionReason(null);
				if (!StringUtils.hasText(form.getManagerYubinNo()))
					errors.put("managerYubinNo", "住所（郵便番号）は必須です");
				if (!StringUtils.hasText(form.getManagerAddress()))
					errors.put("managerAddress", "住所（所在地）は必須です");
				if (!StringUtils.hasText(form.getManagerName()))
					errors.put("managerName", "氏名は必須です");
				if (!StringUtils.hasText(form.getManagerNameKana()))
					errors.put("managerNameKana", "ふりがなは必須です");
				if (!StringUtils.hasText(form.getManagerPhone()))
					errors.put("managerPhone", "電話番号は必須です");
			}
			return errors;
		}

		@Override
		public boolean isValid(TaxManagerForm form, ConstraintValidatorContext context) {
			context.disableDefaultConstraintViolation();
			java.util.Map<String, String> errors = validate(form);
			errors.forEach((field, message) ->
				context.buildConstraintViolationWithTemplate(message)
						.addPropertyNode(field).addConstraintViolation()
			);
			return errors.isEmpty();
		}
	}
}
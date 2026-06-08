package jp.lg.asp.accommodation.dto;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@TaxManagerForm.TaxManagerValid
public class TaxManagerForm {

	private Long collectorId;
	private String obligorName;
	private String facilityName;
	private String obligorAtenaNo; // 特別徴収義務者の宛名番号

	@NotNull(message = "登録日は必須です")
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate registrationDate;

	@NotNull(message = "申告日は必須です")
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

		@Override
		public boolean isValid(TaxManagerForm form, ConstraintValidatorContext context) {
			boolean isValid = true;

			if (form.isExemptionFlag()) {
				// --- 選任免除が「有効」の場合 ---
				// 免除理由が空ならエラー
				if (!StringUtils.hasText(form.getExemptionReason())) {
					addError(context, "exemptionReason", "選任免除理由を入力してください");
					isValid = false;
				}
			} else {
				// --- 選任免除が「無効」の場合 ---
				// 宛名番号チェック
				if (!StringUtils.hasText(form.getAtenaNo())) {
					addError(context, "atenaNo", "宛名番号を入力してください");
					isValid = false;
				}
				// 郵便番号・住所・氏名・ふりがな・電話番号が空ならエラー
				if (!StringUtils.hasText(form.getManagerYubinNo())) {
					addError(context, "managerYubinNo", "住所（郵便番号）を入力してください");
					isValid = false;
				}
				if (!StringUtils.hasText(form.getManagerAddress())) {
					addError(context, "managerAddress", "住所（所在地）を入力してください");
					isValid = false;
				}
				if (!StringUtils.hasText(form.getManagerName())) {
					addError(context, "managerName", "氏名を入力してください");
					isValid = false;
				}
				if (!StringUtils.hasText(form.getManagerNameKana())) {
					addError(context, "managerNameKana", "ふりがなを入力してください");
					isValid = false;
				}
				if (!StringUtils.hasText(form.getManagerPhone())) {
					addError(context, "managerPhone", "電話番号を入力してください");
					isValid = false;
				}
			}

			return isValid;
		}

		// 特定のフィールドにエラーメッセージを紐付けるための補助メソッド
		private void addError(ConstraintValidatorContext context, String fieldName, String message) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate(message)
					.addPropertyNode(fieldName)
					.addConstraintViolation();
		}
	}
}
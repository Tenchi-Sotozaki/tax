package jp.lg.asp.accommodation.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.springframework.util.StringUtils;

public class TaxManagerValidator implements ConstraintValidator<TaxManagerValid, TaxManagerForm> {

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
            // 住所・氏名・ふりがな・電話番号が空ならエラー
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
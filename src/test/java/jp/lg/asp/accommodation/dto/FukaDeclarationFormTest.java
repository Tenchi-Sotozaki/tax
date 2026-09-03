package jp.lg.asp.accommodation.dto;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class FukaDeclarationFormTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    // No.134 全項目が正常 → エラーがでない
    @Test
    void 全項目正常_エラーなし() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setTorokuDate(LocalDate.of(2024, 5, 1));
        form.setShinkokuDate(LocalDate.of(2024, 5, 1));
        form.setAdditionalRate1("99.9");
        form.setAdditionalAmount1(9_999_999_999_999L);
        form.setEntaikin(9_999_999_999_999L);
        assertThat(validator.validate(form)).isEmpty();
    }

    // No.135 torokuDateがnull → バリデーションエラー
    @Test
    void torokuDateがnull_バリデーションエラー() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setTorokuDate(null);
        form.setShinkokuDate(LocalDate.now());
        Set<ConstraintViolation<FukaDeclarationForm>> violations = validator.validateProperty(form, "torokuDate");
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("登録年月日は必須です");
    }

    // No.136 shinkokuDateがnull → バリデーションエラー
    @Test
    void shinkokuDateがnull_バリデーションエラー() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setTorokuDate(LocalDate.now());
        form.setShinkokuDate(null);
        Set<ConstraintViolation<FukaDeclarationForm>> violations = validator.validateProperty(form, "shinkokuDate");
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("申告年月日は必須です");
    }

    // No.137 additionalRate1が101以上 → エラー
    @Test
    void additionalRate1が101以上_エラー() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setAdditionalRate1("101");
        Set<ConstraintViolation<FukaDeclarationForm>> violations = validator.validateProperty(form, "additionalRate1");
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("加算割合1は0〜100の半角数字");
    }

    // No.138 additionalAmount1が14桁 → エラー
    @Test
    void additionalAmount1が14桁_エラー() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setAdditionalAmount1(99_999_999_999_999L);
        Set<ConstraintViolation<FukaDeclarationForm>> violations = validator.validateProperty(form, "additionalAmount1");
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("13桁以内で入力してください");
    }

    // No.139 entaikinが14桁 → エラー
    @Test
    void entaikinが14桁_エラー() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setEntaikin(99_999_999_999_999L);
        Set<ConstraintViolation<FukaDeclarationForm>> violations = validator.validateProperty(form, "entaikin");
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("13桁以内で入力してください");
    }

    // No.140 monthlyDetail内のtaxDetails.hakusuが9桁 → エラー
    @Test
    void taxDetailsのhakusuが9桁_エラー() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setTorokuDate(LocalDate.now());
        form.setShinkokuDate(LocalDate.now());
        FukaTaxDetailDto detail = new FukaTaxDetailDto();
        detail.setHakusu(999_999_999L);
        form.getMonthlyDetail().getTaxDetails().add(detail);
        Set<ConstraintViolation<FukaDeclarationForm>> violations = validator.validate(form);
        assertThat(violations).anyMatch(v -> v.getMessage().equals("8桁以内で入力してください"));
    }

    // No.141 monthlyDetail内のtaxDetails.zeigakuが14桁 → エラー
    @Test
    void taxDetailsのzeigakuが14桁_エラー() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setTorokuDate(LocalDate.now());
        form.setShinkokuDate(LocalDate.now());
        FukaTaxDetailDto detail = new FukaTaxDetailDto();
        detail.setZeigaku(99_999_999_999_999L);
        form.getMonthlyDetail().getTaxDetails().add(detail);
        Set<ConstraintViolation<FukaDeclarationForm>> violations = validator.validate(form);
        assertThat(violations).anyMatch(v -> v.getMessage().equals("13桁以内で入力してください"));
    }

    // No.142 dailyItems.hakusuが9桁 → エラー
    @Test
    void dailyItemsのhakusuが9桁_エラー() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setTorokuDate(LocalDate.now());
        form.setShinkokuDate(LocalDate.now());
        form.getMonthlyTally().initialize(1);
        form.getMonthlyTally().getDailyItems().get(0).getHakusu().set(0, 999_999_999);
        Set<ConstraintViolation<FukaDeclarationForm>> violations = validator.validate(form);
        assertThat(violations).anyMatch(v -> v.getMessage().equals("8桁以内で入力してください"));
    }

    // No.143 dailyItems.zeigakuが14桁 → エラー
    @Test
    void dailyItemsのzeigakuが14桁_エラー() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setTorokuDate(LocalDate.now());
        form.setShinkokuDate(LocalDate.now());
        form.getMonthlyTally().initialize(1);
        form.getMonthlyTally().getDailyItems().get(0).setZeigaku(99_999_999_999_999L);
        Set<ConstraintViolation<FukaDeclarationForm>> violations = validator.validate(form);
        assertThat(violations).anyMatch(v -> v.getMessage().equals("13桁以内で入力してください"));
    }
}

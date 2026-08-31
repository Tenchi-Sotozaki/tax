package jp.lg.asp.accommodation.dto;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class JichitaiConfigDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private JichitaiConfigDto validDto() {
        JichitaiConfigDto dto = new JichitaiConfigDto();
        dto.setJichitaiCd("01202");
        dto.setName("占冠村");
        dto.setKbnName("村");
        dto.setNendoStMonth("3");
        dto.setNozeiShuki("1");
        dto.setShiteiStChar("001");
        dto.setGassanStChar("901");
        dto.setAtenaStNo(new BigDecimal("00001"));
        dto.setParam("占冠村");
        dto.setUserId("Test");
        return dto;
    }

    // No.20 正常系: 全フィールドが有効値の場合、バリデーションエラーなし
    @Test
    void 全フィールドが有効値の場合_バリデーションエラーなし() {
        Set<ConstraintViolation<JichitaiConfigDto>> violations = validator.validate(validDto());

        assertThat(violations).isEmpty();
    }

    // No.21 異常系: 自治体コードが未入力の場合、エラーメッセージを返す
    @Test
    void 自治体コードが未入力の場合_エラーメッセージを返す() {
        JichitaiConfigDto dto = validDto();
        dto.setJichitaiCd("");

        Set<ConstraintViolation<JichitaiConfigDto>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("jichitaiCd") &&
                v.getMessage().equals("自治体コードは必須です。"));
    }

    // No.22 異常系: 自治体名称が未入力の場合、エラーメッセージを返す
    @Test
    void 自治体名称が未入力の場合_エラーメッセージを返す() {
        JichitaiConfigDto dto = validDto();
        dto.setName("");

        Set<ConstraintViolation<JichitaiConfigDto>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("name") &&
                v.getMessage().equals("自治体名称は必須です。"));
    }

    // No.23 異常系: 自治体種別名が未入力の場合、エラーメッセージを返す
    @Test
    void 自治体種別名が未入力の場合_エラーメッセージを返す() {
        JichitaiConfigDto dto = validDto();
        dto.setKbnName("");

        Set<ConstraintViolation<JichitaiConfigDto>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("kbnName") &&
                v.getMessage().equals("自治体種別名は必須です。"));
    }

    // No.24 異常系: 年度開始月が未入力の場合、エラーメッセージを返す
    @Test
    void 年度開始月が未入力の場合_エラーメッセージを返す() {
        JichitaiConfigDto dto = validDto();
        dto.setNendoStMonth("");

        Set<ConstraintViolation<JichitaiConfigDto>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("nendoStMonth") &&
                v.getMessage().equals("年度開始月は必須です。"));
    }

    // No.25 異常系: デフォルト納税周期が未入力の場合、エラーメッセージを返す
    @Test
    void デフォルト納税周期が未入力の場合_エラーメッセージを返す() {
        JichitaiConfigDto dto = validDto();
        dto.setNozeiShuki("");

        Set<ConstraintViolation<JichitaiConfigDto>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("nozeiShuki") &&
                v.getMessage().equals("デフォルト納税周期は必須です。"));
    }

    // No.26 異常系: 指定番号が未入力の場合、エラーメッセージを返す
    @Test
    void 指定番号が未入力の場合_エラーメッセージを返す() {
        JichitaiConfigDto dto = validDto();
        dto.setShiteiStChar("");

        Set<ConstraintViolation<JichitaiConfigDto>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("shiteiStChar") &&
                v.getMessage().equals("指定番号は必須です。"));
    }

    // No.27 異常系: 合算指定番号が未入力の場合、エラーメッセージを返す
    @Test
    void 合算指定番号が未入力の場合_エラーメッセージを返す() {
        JichitaiConfigDto dto = validDto();
        dto.setGassanStChar("");

        Set<ConstraintViolation<JichitaiConfigDto>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("gassanStChar") &&
                v.getMessage().equals("合算指定番号は必須です。"));
    }

    // No.28 異常系: 宛名番号が未入力（null）の場合、エラーメッセージを返す
    @Test
    void 宛名番号がnullの場合_エラーメッセージを返す() {
        JichitaiConfigDto dto = validDto();
        dto.setAtenaStNo(null);

        Set<ConstraintViolation<JichitaiConfigDto>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("atenaStNo") &&
                v.getMessage().equals("宛名番号は必須です。"));
    }

    // No.29 異常系: 自治体識別名が未入力（null）の場合、エラーメッセージを返す
    @Test
    void 自治体識別名がnullの場合_エラーメッセージを返す() {
        JichitaiConfigDto dto = validDto();
        dto.setParam(null);

        Set<ConstraintViolation<JichitaiConfigDto>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("param") &&
                v.getMessage().equals("自治体識別名は必須です。"));
    }

    // No.30 異常系: ユーザーIDが未入力の場合、エラーメッセージを返す
    @Test
    void ユーザーIDが未入力の場合_エラーメッセージを返す() {
        JichitaiConfigDto dto = validDto();
        dto.setUserId("");

        Set<ConstraintViolation<JichitaiConfigDto>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("userId") &&
                v.getMessage().equals("ユーザーIDは必須です。"));
    }
}

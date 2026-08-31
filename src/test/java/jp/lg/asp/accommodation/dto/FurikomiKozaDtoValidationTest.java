package jp.lg.asp.accommodation.dto;

import static org.assertj.core.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class FurikomiKozaDtoValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private FurikomiKozaDto validDto() {
        FurikomiKozaDto dto = new FurikomiKozaDto();
        dto.setBankCd("2026");
        dto.setBankName("機関名テスト");
        dto.setBranchCd("123");
        dto.setBranchName("支店名テスト");
        dto.setShumoku("1");
        dto.setKozaNo("0123456");
        dto.setMeigi("口座名義テスト");
        return dto;
    }

    private boolean hasMessage(Set<ConstraintViolation<FurikomiKozaDto>> violations, String message) {
        return violations.stream().anyMatch(v -> v.getMessage().equals(message));
    }

    // No.31 正常系: 全項目が正常、バリデーションエラーなし
    @Test
    void validate_全項目正常_バリデーションエラーなし() {
        Set<ConstraintViolation<FurikomiKozaDto>> violations = validator.validate(validDto());
        assertThat(violations).isEmpty();
    }

    // No.32 異常系: 金融機関コードが未入力
    @Test
    void validate_金融機関コードが未入力_必須エラー() {
        FurikomiKozaDto dto = validDto();
        dto.setBankCd(null);
        Set<ConstraintViolation<FurikomiKozaDto>> violations = validator.validate(dto);
        assertThat(hasMessage(violations, "金融機関コードは必須入力です")).isTrue();
    }

    // No.33 異常系: 金融機関コードが4桁以外
    @Test
    void validate_金融機関コードが4桁以外_フォーマットエラー() {
        FurikomiKozaDto dto = validDto();
        dto.setBankCd("12345");
        Set<ConstraintViolation<FurikomiKozaDto>> violations = validator.validate(dto);
        assertThat(hasMessage(violations, "金融機関コードは4桁の数字で入力してください")).isTrue();
    }

    // No.34 異常系: 金融機関名が未入力
    @Test
    void validate_金融機関名が未入力_必須エラー() {
        FurikomiKozaDto dto = validDto();
        dto.setBankName(null);
        Set<ConstraintViolation<FurikomiKozaDto>> violations = validator.validate(dto);
        assertThat(hasMessage(violations, "金融機関名は必須入力です")).isTrue();
    }

    // No.35 異常系: 金融機関名が30字超
    @Test
    void validate_金融機関名が30字超_桁数エラー() {
        FurikomiKozaDto dto = validDto();
        dto.setBankName("あ".repeat(31));
        Set<ConstraintViolation<FurikomiKozaDto>> violations = validator.validate(dto);
        assertThat(hasMessage(violations, "金融機関名は30文字以内で入力してください")).isTrue();
    }

    // No.36 異常系: 支店コードが未入力
    @Test
    void validate_支店コードが未入力_必須エラー() {
        FurikomiKozaDto dto = validDto();
        dto.setBranchCd(null);
        Set<ConstraintViolation<FurikomiKozaDto>> violations = validator.validate(dto);
        assertThat(hasMessage(violations, "支店コードは必須入力です")).isTrue();
    }

    // No.37 異常系: 支店コードが3桁以外
    @Test
    void validate_支店コードが3桁以外_フォーマットエラー() {
        FurikomiKozaDto dto = validDto();
        dto.setBranchCd("1234");
        Set<ConstraintViolation<FurikomiKozaDto>> violations = validator.validate(dto);
        assertThat(hasMessage(violations, "支店コードは3桁の数字で入力してください")).isTrue();
    }

    // No.38 異常系: 支店名が未入力
    @Test
    void validate_支店名が未入力_必須エラー() {
        FurikomiKozaDto dto = validDto();
        dto.setBranchName(null);
        Set<ConstraintViolation<FurikomiKozaDto>> violations = validator.validate(dto);
        assertThat(hasMessage(violations, "支店名は必須入力です")).isTrue();
    }

    // No.39 異常系: 支店名が30字超
    @Test
    void validate_支店名が30字超_桁数エラー() {
        FurikomiKozaDto dto = validDto();
        dto.setBranchName("あ".repeat(31));
        Set<ConstraintViolation<FurikomiKozaDto>> violations = validator.validate(dto);
        assertThat(hasMessage(violations, "支店名は30文字以内で入力してください")).isTrue();
    }

    // No.40 異常系: 預金種目が未選択
    @Test
    void validate_預金種目が未選択_必須エラー() {
        FurikomiKozaDto dto = validDto();
        dto.setShumoku(null);
        Set<ConstraintViolation<FurikomiKozaDto>> violations = validator.validate(dto);
        assertThat(hasMessage(violations, "預金種目は必須選択です")).isTrue();
    }

    // No.41 異常系: 口座番号が未入力
    @Test
    void validate_口座番号が未入力_必須エラー() {
        FurikomiKozaDto dto = validDto();
        dto.setKozaNo(null);
        Set<ConstraintViolation<FurikomiKozaDto>> violations = validator.validate(dto);
        assertThat(hasMessage(violations, "口座番号は必須入力です")).isTrue();
    }

    // No.42 異常系: 口座番号が7桁以外
    @Test
    void validate_口座番号が7桁以外_フォーマットエラー() {
        FurikomiKozaDto dto = validDto();
        dto.setKozaNo("12345678");
        Set<ConstraintViolation<FurikomiKozaDto>> violations = validator.validate(dto);
        assertThat(hasMessage(violations, "口座番号は7桁の数字で入力してください")).isTrue();
    }

    // No.43 異常系: 口座名義が未入力
    @Test
    void validate_口座名義が未入力_必須エラー() {
        FurikomiKozaDto dto = validDto();
        dto.setMeigi(null);
        Set<ConstraintViolation<FurikomiKozaDto>> violations = validator.validate(dto);
        assertThat(hasMessage(violations, "口座名義は必須入力です")).isTrue();
    }

    // No.44 異常系: 口座名義が30字超
    @Test
    void validate_口座名義が30字超_桁数エラー() {
        FurikomiKozaDto dto = validDto();
        dto.setMeigi("あ".repeat(31));
        Set<ConstraintViolation<FurikomiKozaDto>> violations = validator.validate(dto);
        assertThat(hasMessage(violations, "口座名義は30文字以内で入力してください")).isTrue();
    }
}

package jp.lg.asp.accommodation.dto;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;

class KofuRitsuConfigDtoTest {

    private KofuRitsuConfigDto validDto() {
        KofuRitsuConfigDto dto = new KofuRitsuConfigDto();
        dto.setTekiyoStNendo("2026");
        dto.setKofuRitsu(new BigDecimal("1.50"));
        dto.setSanshutsu(100);
        dto.setKbn("1");
        dto.setSaiteigaku(new BigDecimal("100"));
        return dto;
    }

    @Test
    void validate_全項目正常_エラーなし() {
        assertThat(KofuRitsuConfigDto.validate(validDto())).isEmpty();
    }

    @Test
    void validate_交付率未入力() {
        KofuRitsuConfigDto dto = validDto();
        dto.setKofuRitsu(null);
        assertThat(KofuRitsuConfigDto.validate(dto)).containsEntry("kofuRitsu", "交付率は必須です");
    }

    @Test
    void validate_交付率下限_エラーなし() {
        KofuRitsuConfigDto dto = validDto();
        dto.setKofuRitsu(BigDecimal.ZERO);
        assertThat(KofuRitsuConfigDto.validate(dto)).doesNotContainKey("kofuRitsu");
    }

    @Test
    void validate_交付率上限_エラーなし() {
        KofuRitsuConfigDto dto = validDto();
        dto.setKofuRitsu(new BigDecimal("999.99"));
        assertThat(KofuRitsuConfigDto.validate(dto)).doesNotContainKey("kofuRitsu");
    }

    @Test
    void validate_交付率下限未満() {
        KofuRitsuConfigDto dto = validDto();
        dto.setKofuRitsu(new BigDecimal("-0.01"));
        assertThat(KofuRitsuConfigDto.validate(dto)).containsEntry("kofuRitsu", "交付率は0～999.99の範囲で入力してください");
    }

    @Test
    void validate_交付率上限超過() {
        KofuRitsuConfigDto dto = validDto();
        dto.setKofuRitsu(new BigDecimal("1000.00"));
        assertThat(KofuRitsuConfigDto.validate(dto)).containsEntry("kofuRitsu", "交付率は0～999.99の範囲で入力してください");
    }

    @Test
    void validate_算出単位未入力() {
        KofuRitsuConfigDto dto = validDto();
        dto.setSanshutsu(null);
        assertThat(KofuRitsuConfigDto.validate(dto)).containsEntry("sanshutsu", "算出単位は必須です");
    }

    @Test
    void validate_区分未選択() {
        KofuRitsuConfigDto dto = validDto();
        dto.setKbn("");
        assertThat(KofuRitsuConfigDto.validate(dto)).containsEntry("kbn", "区分は必須です");
    }

    @Test
    void validate_最低額未入力() {
        KofuRitsuConfigDto dto = validDto();
        dto.setSaiteigaku(null);
        assertThat(KofuRitsuConfigDto.validate(dto)).containsEntry("saiteigaku", "最低額は必須です");
    }

    @Test
    void validate_適用開始年度未入力() {
        KofuRitsuConfigDto dto = validDto();
        dto.setTekiyoStNendo(null);
        assertThat(KofuRitsuConfigDto.validate(dto)).containsEntry("tekiyoStNendo", "適用開始年度は必須です");
    }

    @Test
    void validate_全項目未入力_5件のエラーが順序通りに返る() {
        KofuRitsuConfigDto dto = new KofuRitsuConfigDto();
        Map<String, String> errors = KofuRitsuConfigDto.validate(dto);
        assertThat(errors).hasSize(5);
        assertThat(errors.keySet()).containsExactly("kofuRitsu", "sanshutsu", "kbn", "saiteigaku", "tekiyoStNendo");
    }
}

package jp.lg.asp.accommodation.dto;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class FukaMonthlyTallyDtoTest {

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

    // ===== コンストラクタ (No.155-158) =====

    // No.155 dailyItemsが31件生成される
    @Test
    void コンストラクタ_dailyItemsが31件生成される() {
        FukaMonthlyTallyDto dto = new FukaMonthlyTallyDto();
        assertThat(dto.getDailyItems()).hasSize(31);
    }

    // No.156 day=1〜31が順番に設定される
    @Test
    void コンストラクタ_dayが1から31の順で設定される() {
        FukaMonthlyTallyDto dto = new FukaMonthlyTallyDto();
        assertThat(dto.getDailyItems().get(0).getDay()).isEqualTo(1);
        assertThat(dto.getDailyItems().get(30).getDay()).isEqualTo(31);
    }

    // No.157 各DailyItemのhakusu/ryokin/sogakuは空リスト
    @Test
    void コンストラクタ_hakusuRyokinSogakuは空リスト() {
        FukaMonthlyTallyDto dto = new FukaMonthlyTallyDto();
        FukaMonthlyTallyDto.DailyItem item = dto.getDailyItems().get(0);
        assertThat(item.getHakusu()).isEmpty();
        assertThat(item.getRyokin()).isEmpty();
        assertThat(item.getSogaku()).isEmpty();
    }

    // No.158 各DailyItemのmenjoHakusu/menjoRyokin/zeigakuはnull
    @Test
    void コンストラクタ_menjoHakusuMenjoRyokinZeigakuはnull() {
        FukaMonthlyTallyDto dto = new FukaMonthlyTallyDto();
        FukaMonthlyTallyDto.DailyItem item = dto.getDailyItems().get(0);
        assertThat(item.getMenjoHakusu()).isNull();
        assertThat(item.getMenjoRyokin()).isNull();
        assertThat(item.getZeigaku()).isNull();
    }

    // ===== initialize (No.159-164) =====

    // No.159 categoryCount=2 → dailyItemsが31件生成される
    @Test
    void initialize_categoryCount2_dailyItemsが31件() {
        FukaMonthlyTallyDto dto = new FukaMonthlyTallyDto();
        dto.initialize(2);
        assertThat(dto.getDailyItems()).hasSize(31);
    }

    // No.160 categoryCount=2 → day=1〜31が順番に設定される
    @Test
    void initialize_categoryCount2_dayが1から31の順で設定される() {
        FukaMonthlyTallyDto dto = new FukaMonthlyTallyDto();
        dto.initialize(2);
        assertThat(dto.getDailyItems().get(0).getDay()).isEqualTo(1);
        assertThat(dto.getDailyItems().get(30).getDay()).isEqualTo(31);
    }

    // No.161 categoryCount=2 → hakusu/ryokin/sogakuが2要素で0初期化される
    @Test
    void initialize_categoryCount2_hakusuRyokinSogakuが2要素で0初期化() {
        FukaMonthlyTallyDto dto = new FukaMonthlyTallyDto();
        dto.initialize(2);
        FukaMonthlyTallyDto.DailyItem item = dto.getDailyItems().get(0);
        assertThat(item.getHakusu()).containsExactly(0, 0);
        assertThat(item.getRyokin()).containsExactly(0L, 0L);
        assertThat(item.getSogaku()).containsExactly(0L, 0L);
    }

    // No.162 categoryCount=2 → menjoHakusu=0、menjoRyokin=0、zeigaku=0で初期化される
    @Test
    void initialize_categoryCount2_menjoHakusuMenjoRyokinZeigakuが0初期化() {
        FukaMonthlyTallyDto dto = new FukaMonthlyTallyDto();
        dto.initialize(2);
        FukaMonthlyTallyDto.DailyItem item = dto.getDailyItems().get(0);
        assertThat(item.getMenjoHakusu()).isEqualTo(0);
        assertThat(item.getMenjoRyokin()).isEqualTo(0L);
        assertThat(item.getZeigaku()).isEqualTo(0L);
    }

    // No.163 categoryCount=0 → hakusu/ryokin/sogakuが空リスト
    @Test
    void initialize_categoryCount0_hakusuRyokinSogakuが空リスト() {
        FukaMonthlyTallyDto dto = new FukaMonthlyTallyDto();
        dto.initialize(0);
        FukaMonthlyTallyDto.DailyItem item = dto.getDailyItems().get(0);
        assertThat(item.getHakusu()).isEmpty();
        assertThat(item.getRyokin()).isEmpty();
        assertThat(item.getSogaku()).isEmpty();
    }

    // No.164 initialize後はコンストラクタで生成した既存のdailyItemsが破棄される
    @Test
    void initialize_既存のdailyItemsが破棄されて新規生成される() {
        FukaMonthlyTallyDto dto = new FukaMonthlyTallyDto();
        List<FukaMonthlyTallyDto.DailyItem> before = dto.getDailyItems();
        dto.initialize(1);
        assertThat(dto.getDailyItems()).isNotSameAs(before);
        assertThat(dto.getDailyItems()).hasSize(31);
    }

    // ===== バリデーション (No.165-171) =====

    // No.165 全項目が正常 → エラーがでない
    @Test
    void バリデーション_全項目正常_エラーなし() {
        FukaMonthlyTallyDto dto = new FukaMonthlyTallyDto();
        dto.initialize(1);
        dto.getDailyItems().get(0).getHakusu().set(0, 99_999_999);
        dto.getDailyItems().get(0).getRyokin().set(0, 9_999_999_999_999L);
        dto.getDailyItems().get(0).getSogaku().set(0, 9_999_999_999_999L);
        dto.getDailyItems().get(0).setMenjoHakusu(99_999_999);
        dto.getDailyItems().get(0).setMenjoRyokin(9_999_999_999_999L);
        dto.getDailyItems().get(0).setZeigaku(9_999_999_999_999L);
        assertThat(validator.validate(dto)).isEmpty();
    }

    // No.166 hakusuリスト要素が9桁 → エラー
    @Test
    void バリデーション_hakusuリスト要素が9桁_エラー() {
        FukaMonthlyTallyDto dto = new FukaMonthlyTallyDto();
        dto.initialize(1);
        dto.getDailyItems().get(0).getHakusu().set(0, 999_999_999);
        Set<ConstraintViolation<FukaMonthlyTallyDto>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getMessage().equals("8桁以内で入力してください"));
    }

    // No.167 ryokinリスト要素が14桁 → エラー
    @Test
    void バリデーション_ryokinリスト要素が14桁_エラー() {
        FukaMonthlyTallyDto dto = new FukaMonthlyTallyDto();
        dto.initialize(1);
        dto.getDailyItems().get(0).getRyokin().set(0, 99_999_999_999_999L);
        Set<ConstraintViolation<FukaMonthlyTallyDto>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getMessage().equals("13桁以内で入力してください"));
    }

    // No.168 sogakuリスト要素が14桁 → エラー
    @Test
    void バリデーション_sogakuリスト要素が14桁_エラー() {
        FukaMonthlyTallyDto dto = new FukaMonthlyTallyDto();
        dto.initialize(1);
        dto.getDailyItems().get(0).getSogaku().set(0, 99_999_999_999_999L);
        Set<ConstraintViolation<FukaMonthlyTallyDto>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getMessage().equals("13桁以内で入力してください"));
    }

    // No.169 menjoHakusuが9桁 → エラー
    @Test
    void バリデーション_menjoHakusuが9桁_エラー() {
        FukaMonthlyTallyDto dto = new FukaMonthlyTallyDto();
        dto.initialize(1);
        dto.getDailyItems().get(0).setMenjoHakusu(999_999_999);
        Set<ConstraintViolation<FukaMonthlyTallyDto>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getMessage().equals("8桁以内で入力してください"));
    }

    // No.170 menjoRyokinが14桁 → エラー
    @Test
    void バリデーション_menjoRyokinが14桁_エラー() {
        FukaMonthlyTallyDto dto = new FukaMonthlyTallyDto();
        dto.initialize(1);
        dto.getDailyItems().get(0).setMenjoRyokin(99_999_999_999_999L);
        Set<ConstraintViolation<FukaMonthlyTallyDto>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getMessage().equals("13桁以内で入力してください"));
    }

    // No.171 zeigakuが14桁 → エラー
    @Test
    void バリデーション_zeigakuが14桁_エラー() {
        FukaMonthlyTallyDto dto = new FukaMonthlyTallyDto();
        dto.initialize(1);
        dto.getDailyItems().get(0).setZeigaku(99_999_999_999_999L);
        Set<ConstraintViolation<FukaMonthlyTallyDto>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getMessage().equals("13桁以内で入力してください"));
    }
}

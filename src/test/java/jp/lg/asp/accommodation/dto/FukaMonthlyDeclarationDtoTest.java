package jp.lg.asp.accommodation.dto;

import static org.assertj.core.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class FukaMonthlyDeclarationDtoTest {

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

	private Set<ConstraintViolation<FukaMonthlyDeclarationDto>> validateTotalStayCount(Long totalStayCount) {
		FukaMonthlyDeclarationDto dto = new FukaMonthlyDeclarationDto();
		dto.setTotalStayCount(totalStayCount);
		return validator.validateProperty(dto, "totalStayCount");
	}

	@Test
	void 宿泊数合計_9桁ちょうどは許容される() {
		assertThat(validateTotalStayCount(999_999_999L)).isEmpty();
	}

	@Test
	void 宿泊数合計_10桁はエラーになる() {
		Set<ConstraintViolation<FukaMonthlyDeclarationDto>> violations = validateTotalStayCount(1_000_000_000L);
		assertThat(violations).hasSize(1);
		assertThat(violations.iterator().next().getMessage()).isEqualTo("9桁以内で入力してください");
	}

	@Test
	void 宿泊数合計_未入力は桁数チェックの対象外() {
		assertThat(validateTotalStayCount(null)).isEmpty();
	}

	private Set<ConstraintViolation<FukaMonthlyDeclarationDto>> validateExemptStayCount(Long exemptStayCount) {
		FukaMonthlyDeclarationDto dto = new FukaMonthlyDeclarationDto();
		dto.setExemptStayCount(exemptStayCount);
		return validator.validateProperty(dto, "exemptStayCount");
	}

	@Test
	void 課税対象外宿泊数_9桁ちょうどは許容される() {
		assertThat(validateExemptStayCount(999_999_999L)).isEmpty();
	}

	@Test
	void 課税対象外宿泊数_10桁はエラーになる() {
		Set<ConstraintViolation<FukaMonthlyDeclarationDto>> violations = validateExemptStayCount(1_000_000_000L);
		assertThat(violations).hasSize(1);
		assertThat(violations.iterator().next().getMessage()).isEqualTo("9桁以内で入力してください");
	}

	@Test
	void 課税対象外宿泊数_未入力は桁数チェックの対象外() {
		assertThat(validateExemptStayCount(null)).isEmpty();
	}

	// No.144 全項目が正常 → エラーがでない
	@Test
	void 全項目正常_エラーなし() {
		FukaMonthlyDeclarationDto dto = new FukaMonthlyDeclarationDto();
		dto.setExemptRyokin(9_999_999_999_999L);
		dto.setExemptStayCount(999_999_999L);
		dto.setTotalSogaku(9_999_999_999_999L);
		dto.setTotalStayCount(999_999_999L);
		dto.setKazeiRyokin(9_999_999_999_999L);
		dto.setTotalPaymentAmount(9_999_999_999_999L);
		dto.setTotalCityZeigaku(9_999_999_999_999L);
		dto.setTotalKenZeigaku(9_999_999_999_999L);
		assertThat(validator.validate(dto)).isEmpty();
	}

	// No.145 exemptRyokinが14桁 → エラー
	@Test
	void exemptRyokin_14桁_エラー() {
		FukaMonthlyDeclarationDto dto = new FukaMonthlyDeclarationDto();
		dto.setExemptRyokin(99_999_999_999_999L);
		var violations = validator.validateProperty(dto, "exemptRyokin");
		assertThat(violations).hasSize(1);
		assertThat(violations.iterator().next().getMessage()).isEqualTo("13桁以内で入力してください");
	}

	// No.147 totalSogakuが14桁 → エラー
	@Test
	void totalSogaku_14桁_エラー() {
		FukaMonthlyDeclarationDto dto = new FukaMonthlyDeclarationDto();
		dto.setTotalSogaku(99_999_999_999_999L);
		var violations = validator.validateProperty(dto, "totalSogaku");
		assertThat(violations).hasSize(1);
		assertThat(violations.iterator().next().getMessage()).isEqualTo("13桁以内で入力してください");
	}

	// No.149 kazeiRyokinが14桁 → エラー
	@Test
	void kazeiRyokin_14桁_エラー() {
		FukaMonthlyDeclarationDto dto = new FukaMonthlyDeclarationDto();
		dto.setKazeiRyokin(99_999_999_999_999L);
		var violations = validator.validateProperty(dto, "kazeiRyokin");
		assertThat(violations).hasSize(1);
		assertThat(violations.iterator().next().getMessage()).isEqualTo("13桁以内で入力してください");
	}

	// No.150 totalPaymentAmountが14桁 → エラー
	@Test
	void totalPaymentAmount_14桁_エラー() {
		FukaMonthlyDeclarationDto dto = new FukaMonthlyDeclarationDto();
		dto.setTotalPaymentAmount(99_999_999_999_999L);
		var violations = validator.validateProperty(dto, "totalPaymentAmount");
		assertThat(violations).hasSize(1);
		assertThat(violations.iterator().next().getMessage()).isEqualTo("13桁以内で入力してください");
	}

	// No.151 totalCityZeigakuが14桁 → エラー
	@Test
	void totalCityZeigaku_14桁_エラー() {
		FukaMonthlyDeclarationDto dto = new FukaMonthlyDeclarationDto();
		dto.setTotalCityZeigaku(99_999_999_999_999L);
		var violations = validator.validateProperty(dto, "totalCityZeigaku");
		assertThat(violations).hasSize(1);
		assertThat(violations.iterator().next().getMessage()).isEqualTo("13桁以内で入力してください");
	}

	// No.152 totalKenZeigakuが14桁 → エラー
	@Test
	void totalKenZeigaku_14桁_エラー() {
		FukaMonthlyDeclarationDto dto = new FukaMonthlyDeclarationDto();
		dto.setTotalKenZeigaku(99_999_999_999_999L);
		var violations = validator.validateProperty(dto, "totalKenZeigaku");
		assertThat(violations).hasSize(1);
		assertThat(violations.iterator().next().getMessage()).isEqualTo("13桁以内で入力してください");
	}
}

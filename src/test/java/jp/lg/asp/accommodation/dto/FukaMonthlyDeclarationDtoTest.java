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

/**
 * 宿泊数合計・課税対象外宿泊数の桁数チェック。
 */
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
		assertThat(violations.iterator().next().getMessage())
				.isEqualTo("9桁以内で入力してください");
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
		assertThat(violations.iterator().next().getMessage())
				.isEqualTo("9桁以内で入力してください");
	}

	@Test
	void 課税対象外宿泊数_未入力は桁数チェックの対象外() {
		assertThat(validateExemptStayCount(null)).isEmpty();
	}
}

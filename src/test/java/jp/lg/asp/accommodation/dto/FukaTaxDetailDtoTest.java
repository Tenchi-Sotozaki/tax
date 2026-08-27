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
 * 区分ごとの宿泊数の桁数チェック。
 */
class FukaTaxDetailDtoTest {

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

	private Set<ConstraintViolation<FukaTaxDetailDto>> validateHakusu(Long hakusu) {
		FukaTaxDetailDto dto = new FukaTaxDetailDto();
		dto.setHakusu(hakusu);
		return validator.validateProperty(dto, "hakusu");
	}

	@Test
	void 宿泊数_8桁ちょうどは許容される() {
		assertThat(validateHakusu(99_999_999L)).isEmpty();
	}

	@Test
	void 宿泊数_9桁はエラーになる() {
		Set<ConstraintViolation<FukaTaxDetailDto>> violations = validateHakusu(100_000_000L);

		assertThat(violations).hasSize(1);
		assertThat(violations.iterator().next().getMessage())
				.isEqualTo("8桁以内で入力してください");
	}

	@Test
	void 宿泊数_未入力は桁数チェックの対象外() {
		assertThat(validateHakusu(null)).isEmpty();
	}
}

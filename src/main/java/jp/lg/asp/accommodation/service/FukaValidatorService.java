package jp.lg.asp.accommodation.service;

import java.util.List;

import org.springframework.validation.BindingResult;

import jp.lg.asp.accommodation.dto.FukaDeclarationForm;

/**
 * 宿泊税申告のバリデーションを行うサービスのインターフェース
 */
public interface FukaValidatorService {

	boolean hasDiscrepancy(FukaDeclarationForm form);

	List<String> getDiscrepancyMessages(FukaDeclarationForm form);

	long calculateExpectedTotal(FukaDeclarationForm form);

	void validateTallyVsParent(FukaDeclarationForm form, BindingResult result);

}
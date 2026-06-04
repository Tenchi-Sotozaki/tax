package jp.lg.asp.accommodation.dto;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

/**
 * 宿泊税情報登録/編集(照会)画面用 Formクラス
 */
@Data 
public class FukaDeclarationForm {

	// ========== 制御用フィールド ==========
	private Long declarationId;
	private String shiteiNo;
	private String fukaKbn;

	// ========== 納税額情報エリア ==========
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@NotNull(message = "※登録日を入力してください")
	private LocalDate registrationDate;

	private String obligorName;
	private String facilityName;

	@Valid
	private FukaMonthlyDeclarationDto monthlyDetail = new FukaMonthlyDeclarationDto();

	// ========== 加算金額入力エリア ==========
	private String additionalCategory;
	private String additionalRate;
	private Long additionalAmount;
	
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate additionalDueDate;

	// ========== 更生/修正エリア ==========
	private String modificationCategory;
	private String modificationReason;
	private String nendo;
	private Integer kibetsu;
	private FukaMonthlyTallyDto monthlyTally = new FukaMonthlyTallyDto();
	private Integer rno;

	// ========== バリデーション制御用フィールド ==========
	private boolean taxCheckBypassed = false;
	private Boolean showTaxWarningModal = false;

	private boolean edit;
	private boolean view;

	// ========== 定率制（fukaKbn == '2'）入力エリア ==========
	private Long kazeiRyokin;
	private Long teiritsuZeigaku;
	private Long menjoRyokin;
	private Long kazeiHakusu;
}
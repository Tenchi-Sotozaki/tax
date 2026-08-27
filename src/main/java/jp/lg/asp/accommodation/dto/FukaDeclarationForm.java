package jp.lg.asp.accommodation.dto;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

/**
 * 宿泊税情報登録/編集(照会)画面用 Formクラス
 */
@Data
public class FukaDeclarationForm {

	// ========== 制御用フィールド ==========
	private boolean edit;
	private boolean view;
	private Long declarationId;
	private String shiteiNo;
	private String nendo;
	private Integer kibetsu;
	private Integer rno;
	private Integer maxRno;
	private Integer minRno;
	private String fukaKbn;

	// ========== 変更区分 ==========
	private String modificationCategory;
	private String modificationReason;

	// ========== 納税額情報エリア ==========
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@NotNull(message = "登録年月日は必須です")
	private LocalDate torokuDate;
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@NotNull(message = "申告年月日は必須です")
	private LocalDate shinkokuDate;

	private String obligorName;
	private String facilityName;

	// 賦課情報
	@Valid
	private FukaMonthlyDeclarationDto monthlyDetail = new FukaMonthlyDeclarationDto();

	// ========== 加算金額入力エリア ==========
	private String additionalCategory1;
	/** 0〜100、小数点以下2桁まで。kasan_ritsu numeric(5,2) に収まる範囲 */
	@Pattern(regexp = "^$|^(100(\\.0{1,2})?|[0-9]{1,2}(\\.[0-9]{1,2})?)$",
			message = "加算割合1は0〜100の半角数字（小数点以下2桁まで）で入力してください")
	private String additionalRate1;
	@Digits(integer = 13, fraction = 0, message = "13桁以内で入力してください")
	private Long additionalAmount1;
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate additionalDueDate1;
	private String additionalCategory2;
	/** 0〜100、小数点以下2桁まで。kasan_ritsu numeric(5,2) に収まる範囲 */
	@Pattern(regexp = "^$|^(100(\\.0{1,2})?|[0-9]{1,2}(\\.[0-9]{1,2})?)$",
			message = "加算割合2は0〜100の半角数字（小数点以下2桁まで）で入力してください")
	private String additionalRate2;
	@Digits(integer = 13, fraction = 0, message = "13桁以内で入力してください")
	private Long additionalAmount2;
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate additionalDueDate2;
	private String additionalCategory3;
	/** 0〜100、小数点以下2桁まで。kasan_ritsu numeric(5,2) に収まる範囲 */
	@Pattern(regexp = "^$|^(100(\\.0{1,2})?|[0-9]{1,2}(\\.[0-9]{1,2})?)$",
			message = "加算割合3は0〜100の半角数字（小数点以下2桁まで）で入力してください")
	private String additionalRate3;
	@Digits(integer = 13, fraction = 0, message = "13桁以内で入力してください")
	private Long additionalAmount3;
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate additionalDueDate3;

	// 延滞金・納入期限（テーブル定義書2026-06-18：加算の納期限を1本化）
	@Digits(integer = 13, fraction = 0, message = "13桁以内で入力してください")
	private Long entaikin;
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate nokigen;

	// 徴収原簿
	@Valid
	private FukaMonthlyTallyDto monthlyTally = new FukaMonthlyTallyDto();

	// ========== 納入情報エリア ==========
	// 必須項目ではない。納入年月日・納入金額の両方に入力がある場合のみ登録処理を行う。
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate shunoYmd;
	private Long shunoKingaku;

	// ========== バリデーション制御用フィールド ==========
	private boolean taxCheckBypassed = false;
	private Boolean showTaxWarningModal = false;
}

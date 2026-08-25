package jp.lg.asp.accommodation.dto;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.util.StringUtils;

import lombok.Data;

@Data
@TokugimuForm.TokugimuValid
public class TokugimuForm {

	private Long id;
	private Long atenaNo;

	// ===== 特別徴収義務者情報 =====
	private LocalDate registrationDate;
	private LocalDate shinseiDate;
	private LocalDate henkoDate;
	@Size(max = 10, message = "特別徴収義務者情報の郵便番号は10文字以内で入力してください")
	private String tokugimuAddressNo;
	@Size(max = 200, message = "特別徴収義務者情報の住所は200文字以内で入力してください")
	private String tokugimuAddress;
	@Size(max = 200, message = "特別徴収義務者情報の氏名または名称は200文字以内で入力してください")
	private String name;
	@Size(max = 200, message = "特別徴収義務者情報の氏名(ふりがな)は200文字以内で入力してください")
	private String nameKana;
	@Size(max = 64, message = "特別徴収義務者情報の個人番号は64文字以内で入力してください")
	private String personalNumber;
	@Size(max = 13, message = "特別徴収義務者情報の法人番号は13文字以内で入力してください")
	private String corporateNumber;
	@Size(max = 20, message = "特別徴収義務者情報の電話番号は20文字以内で入力してください")
	private String tokugimuPhone;

	// ===== 宿泊施設情報 =====
	@Size(max = 10, message = "宿泊施設情報の郵便番号は10文字以内で入力してください")
	private String facilityAddressNo;
	@Size(max = 200, message = "宿泊施設情報の住所は200文字以内で入力してください")
	private String facilityAddress;
	@Size(max = 200, message = "宿泊施設情報の施設名称は200文字以内で入力してください")
	private String facilityName;
	@Size(max = 200, message = "宿泊施設情報の施設名称(ふりがな)は200文字以内で入力してください")
	private String facilityNameKana;
	@Size(max = 20, message = "宿泊施設情報の電話番号は20文字以内で入力してください")
	private String facilityPhone;
	/** 半角数字とピリオドのみ。yuka_menseki numeric(9,2) に合わせて整数部7桁・小数部2桁まで */
	@Pattern(regexp = "^[0-9]{0,7}(\\.[0-9]{1,2})?$",
			message = "宿泊施設情報の延床面積は半角数字とピリオドで、整数部7桁、小数部2桁以内で入力してください")
	private String floorArea;
	/** 半角数字のみ。chijo_kai numeric(3) に合わせて3桁まで */
	@Pattern(regexp = "^[0-9]{0,3}$", message = "宿泊施設情報の階層(地上)は半角数字3桁以内で入力してください")
	private String aboveGroundFloor;
	/** 半角数字のみ。chika_kai numeric(2) に合わせて2桁まで */
	@Pattern(regexp = "^[0-9]{0,2}$", message = "宿泊施設情報の階層(地下)は半角数字2桁以内で入力してください")
	private String basementFloor;
	/** 半角数字のみ。kyakushitsu_su numeric(5) に合わせて5桁まで */
	@Pattern(regexp = "^[0-9]{0,5}$", message = "宿泊施設情報の客室数は半角数字5桁以内で入力してください")
	private String roomCount;
	/** 半角数字のみ。shuyo_su numeric(7) に合わせて7桁まで */
	@Pattern(regexp = "^[0-9]{0,7}$", message = "宿泊施設情報の収容人数は半角数字7桁以内で入力してください")
	private String capacity;
	private LocalDate businessStartDate;

	// ===== 営業許可等情報 =====
	@Size(max = 10, message = "営業許可等情報の郵便番号は10文字以内で入力してください")
	private String licenseAddressNo;
	@Size(max = 200, message = "営業許可等情報の住所は200文字以内で入力してください")
	private String licenseAddress;
	@Size(max = 200, message = "営業許可等情報の氏名は200文字以内で入力してください")
	private String licenseName;
	@Size(max = 200, message = "営業許可等情報の氏名(ふりがな)は200文字以内で入力してください")
	private String licenseNameKana;
	@Size(max = 20, message = "営業許可等情報の電話番号は20文字以内で入力してください")
	private String licensePhone;
	private String businessType;
	@Size(max = 200, message = "営業許可等情報の許可番号は200文字以内で入力してください")
	private String licenseNumber;

	// ===== 施設所有者情報 =====
	@Size(max = 10, message = "施設所有者情報の郵便番号は10文字以内で入力してください")
	private String ownerAddressNo;
	@Size(max = 200, message = "施設所有者情報の住所は200文字以内で入力してください")
	private String ownerAddress;
	@Size(max = 200, message = "施設所有者情報の氏名は200文字以内で入力してください")
	private String ownerName;
	@Size(max = 200, message = "施設所有者情報の氏名(ふりがな)は200文字以内で入力してください")
	private String ownerNameKana;
	@Size(max = 20, message = "施設所有者情報の電話番号は20文字以内で入力してください")
	private String ownerPhone;

	// ===== 書類送付先情報 =====
	@Size(max = 10, message = "書類送付先情報の郵便番号は10文字以内で入力してください")
	private String mailAddressNo;
	@Size(max = 200, message = "書類送付先情報の住所は200文字以内で入力してください")
	private String mailAddress;
	@Size(max = 200, message = "書類送付先情報の氏名は200文字以内で入力してください")
	private String mailName;
	@Size(max = 200, message = "書類送付先情報の氏名(ふりがな)は200文字以内で入力してください")
	private String mailNameKana;
	@Size(max = 20, message = "書類送付先情報の電話番号は20文字以内で入力してください")
	private String mailPhone;

	// ===== 共同事業者情報 =====
	@Valid
	private List<KyodoJigyoshaDto> kyodoList = new ArrayList<>();

	// ===== その他の情報 =====
	private String eltaxUmu;
	@Size(max = 400, message = "その他の情報の備考は400文字以内で入力してください")
	private String remarks;

	// ===== 施設営業休止/再開/廃止情報 =====
	private boolean businessStatusFlg;
	private String declarationCategory;
	private LocalDate suspensionStartDate;
	private LocalDate suspensionEndDate;
	private boolean suspensionEndDateUndecided;
	private LocalDate resumptionOrAbolitionDate;
	@Size(max = 400, message = "施設営業休止/再開/廃止情報の休止または廃止理由は400文字以内で入力してください")
	private String suspensionOrAbolitionReason;

	private String shiteiNo;
	private Integer rno;
	private Integer maxRno;
	private Integer minRno;

	/**
	 * エラーサマリの表示順。tTokugimuConfig.html の項目順に並べる。
	 * 画面に項目を追加したときは、ここにも追加すること。
	 */
	private static final List<String> FIELD_ORDER = List.of(
			"atenaNo", "registrationDate", "henkoDate", "shinseiDate", "tokugimuAddressNo",
			"tokugimuAddress", "name", "nameKana", "personalNumber",
			"corporateNumber", "tokugimuPhone", "businessStartDate", "facilityAddressNo",
			"facilityAddress", "facilityName", "facilityNameKana", "facilityPhone",
			"floorArea", "aboveGroundFloor", "basementFloor", "roomCount",
			"capacity", "licenseAddressNo", "licenseAddress", "licenseName",
			"licenseNameKana", "licensePhone", "businessType", "licenseNumber",
			"ownerAddressNo", "ownerAddress", "ownerName", "ownerNameKana",
			"ownerPhone", "mailAddressNo", "mailAddress", "mailName",
			"mailNameKana", "mailPhone", "kyodoName", "kyodoNameKana",
			"eltaxUmu", "remarks", "declarationCategory", "suspensionStartDate",
			"suspensionEndDate", "resumptionOrAbolitionDate", "suspensionOrAbolitionReason");

	/** FIELD_ORDER 上の位置を返す。未登録の項目は末尾に回す */
	public static int fieldOrder(String field) {
		// kyodoList[0].kyodoName のような添字付きの項目は、末尾の項目名で引く
		int dot = field.lastIndexOf('.');
		int index = FIELD_ORDER.indexOf(dot < 0 ? field : field.substring(dot + 1));
		return index < 0 ? FIELD_ORDER.size() : index;
	}

	public String getTokugimuYubinNo() {
		return mailAddressNo != null && !mailAddressNo.isBlank() ? mailAddressNo : tokugimuAddressNo;
	}

	// ===== カスタムバリデーションアノテーション =====

	@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@Constraint(validatedBy = TokugimuValidator.class)
	@Documented
	public @interface TokugimuValid {
		String message() default "入力内容に誤りがあります";

		Class<?>[] groups() default {};

		Class<? extends Payload>[] payload() default {};
	}

	// ===== カスタムバリデーター =====

	public static class TokugimuValidator implements ConstraintValidator<TokugimuValid, TokugimuForm> {

		/** HTML表示順に対応したフィールド名→エラーメッセージのマップを返す */
		public static Map<String, String> validate(TokugimuForm f) {
			Map<String, String> errors = new LinkedHashMap<>();

			if (f.getRegistrationDate() == null)
				errors.put("registrationDate", "特別徴収義務者情報の登録年月日は必須です");
			if (f.getShinseiDate() == null)
				errors.put("shinseiDate", "特別徴収義務者情報の申請年月日は必須です");
			if (f.getAtenaNo() == null) {
				errors.put("atenaNo", "宛名が選択されていません");
			} else {
				if (!StringUtils.hasText(f.getTokugimuAddressNo()))
					errors.put("tokugimuAddressNo", "特別徴収義務者情報の郵便番号は必須です");
				if (!StringUtils.hasText(f.getTokugimuAddress()))
					errors.put("tokugimuAddress", "特別徴収義務者情報の住所は必須です");
				if (!StringUtils.hasText(f.getName()))
					errors.put("name", "特別徴収義務者情報の氏名または名称は必須です");
				if (!StringUtils.hasText(f.getNameKana()))
					errors.put("nameKana", "特別徴収義務者情報の氏名(ふりがな)は必須です");
				if (!StringUtils.hasText(f.getTokugimuPhone()))
					errors.put("tokugimuPhone", "特別徴収義務者情報の電話番号は必須です");
			}
			if (!StringUtils.hasText(f.getFacilityName()))
				errors.put("facilityName", "宿泊施設情報の施設名称は必須です");
			if (!StringUtils.hasText(f.getFacilityNameKana()))
				errors.put("facilityNameKana", "宿泊施設情報の施設名称(ふりがな)は必須です");
			if (f.getBusinessStartDate() == null)
				errors.put("businessStartDate", "宿泊施設情報の営業開始(予定)日は必須です");
			if (!StringUtils.hasText(f.getLicenseAddressNo()))
				errors.put("licenseAddressNo", "営業許可等情報の郵便番号は必須です");
			if (!StringUtils.hasText(f.getLicenseAddress()))
				errors.put("licenseAddress", "営業許可等情報の住所は必須です");
			if (!StringUtils.hasText(f.getLicenseName()))
				errors.put("licenseName", "営業許可等情報の氏名は必須です");
			if (!StringUtils.hasText(f.getLicenseNameKana()))
				errors.put("licenseNameKana", "営業許可等情報の氏名(ふりがな)は必須です");
			if (!StringUtils.hasText(f.getBusinessType()))
				errors.put("businessType", "営業許可等情報の営業種別は必須です");
			if (!StringUtils.hasText(f.getLicenseNumber()))
				errors.put("licenseNumber", "営業許可等情報の許可番号は必須です");
			boolean ownerAnyInput = StringUtils.hasText(f.getOwnerName()) || StringUtils.hasText(f.getOwnerNameKana())
					|| StringUtils.hasText(f.getOwnerAddressNo()) || StringUtils.hasText(f.getOwnerAddress())
					|| StringUtils.hasText(f.getOwnerPhone());
			if (ownerAnyInput) {
				if (!StringUtils.hasText(f.getOwnerName()))
					errors.put("ownerName", "施設所有者情報の氏名は必須です");
				if (!StringUtils.hasText(f.getOwnerNameKana()))
					errors.put("ownerNameKana", "施設所有者情報の氏名(ふりがな)は必須です");
			}
			if (!StringUtils.hasText(f.getMailName()))
				errors.put("mailName", "書類送付先情報の氏名は必須です");
			// 1行でも入力があれば、その行の氏名・氏名(ふりがな)を必須とする。
			// 完全に空の行は入力なしとみなして無視する（施設所有者情報と同じ扱い）。
			// エラーのキーは画面の th:field と揃えるため添字付きにする。
			List<KyodoJigyoshaDto> kyodoList = f.getKyodoList();
			if (kyodoList != null) {
				for (int i = 0; i < kyodoList.size(); i++) {
					KyodoJigyoshaDto k = kyodoList.get(i);
					if (!hasAnyInput(k))
						continue;
					if (!StringUtils.hasText(k.getKyodoName()))
						errors.put("kyodoList[" + i + "].kyodoName", "共同事業者情報の氏名は必須です");
					if (!StringUtils.hasText(k.getKyodoNameKana()))
						errors.put("kyodoList[" + i + "].kyodoNameKana", "共同事業者情報の氏名(ふりがな)は必須です");
				}
			}

			return errors;
		}

		/** 共同事業者の1行に何か入力されているか */
		private static boolean hasAnyInput(KyodoJigyoshaDto k) {
			return StringUtils.hasText(k.getKyodoName())
					|| StringUtils.hasText(k.getKyodoNameKana())
					|| StringUtils.hasText(k.getKyodoAddressNo())
					|| StringUtils.hasText(k.getKyodoAddress())
					|| StringUtils.hasText(k.getKyodoPhone());
		}

		@Override
		public void initialize(TokugimuValid constraintAnnotation) {
		}

		@Override
		public boolean isValid(TokugimuForm f, ConstraintValidatorContext ctx) {
			ctx.disableDefaultConstraintViolation();
			Map<String, String> errors = validate(f);
			errors.forEach((field, message) -> ctx.buildConstraintViolationWithTemplate(message)
					.addPropertyNode(field)
					.addConstraintViolation());
			return errors.isEmpty();
		}
	}
}

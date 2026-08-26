package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 特別徴収事務交付金照会／登録／編集 DTO
 * 仕様書：特別徴収事務交付金照会・登録・編集.csv に基づく実装
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShoreikinConfigDto {

	// ========== 特別徴収義務者エリア ==========

	/** No.1 指定番号 (t_shoreikin.shitei_no) */
	private String shiteiNo;

	/** No.2 施設名称 (t_tokugimu.shisetsu_name) */
	private String shisetsuName;

	/** No.3 氏名 (m_atena.name) */
	private String name;

	// ========== 交付金情報エリア ==========

	/** No.4 交付金年度 (t_shoreikin.nendo) */
	@NotBlank(message = "交付金年度は必須です")
	@Pattern(regexp = "^[0-9]{4}$", message = "交付金年度は4桁の数字で入力してください")
	private String nendo;

	/** No.5 納入税額 (t_shoreikin.kofu_zeigaku) */
	@NotNull(message = "納入税額は必須です")
	@Min(value = 0, message = "納入税額は0以上で入力してください")
	@Max(value = 99999999999999L, message = "納入税額は14桁以内で入力してください")
	private Long kofuZeigaku;

	/** No.6 交付率 (t_shoreikin.kofu_ritsu) */
	@NotNull(message = "交付率が設定されていません。交付率設定画面で登録してください")
	@DecimalMin(value = "0.00", message = "交付率は0.00以上で入力してください")
	@Digits(integer = 5, fraction = 2, message = "交付率は整数部5桁、小数部2桁以内で入力してください")
	private BigDecimal kofuRitsu;

	/** No.7 交付額 (t_shoreikin.kofu_gaku) */
	@NotNull(message = "交付額は必須です")
	@Min(value = 0, message = "交付額は0以上で入力してください")
	@Max(value = 9999999999999L, message = "交付額は13桁以内で入力してください")
	private Long kofuGaku;

	/** 交付年月日 (t_shoreikin.kofu_ymd) */
	private LocalDate kofuYmd;

	// ========== バリデーションサマリー用 ==========

	/**
	 * バリデーションエラーを、画面項目順のマップで返す。
	 *
	 * BindingResult のフィールドエラー順は Hibernate Validator が返す Set 由来で
	 * 実行ごとに変わりうるため、サマリーはこのメソッドで組み立てる。
	 * 画面に項目を足したらここにも足すこと。
	 *
	 * @param f 入力内容
	 * @return 画面項目順の（項目名, メッセージ）
	 */
	public static Map<String, String> validate(ShoreikinConfigDto f) {
		Map<String, String> errors = new LinkedHashMap<>();

		if (f.getNendo() == null || f.getNendo().isBlank())
			errors.put("nendo", "交付金年度は必須です");
		else if (!f.getNendo().matches("^[0-9]{4}$"))
			errors.put("nendo", "交付金年度は4桁の数字で入力してください");

		if (f.getKofuZeigaku() == null)
			errors.put("kofuZeigaku", "納入税額は必須です");
		else if (f.getKofuZeigaku() < 0)
			errors.put("kofuZeigaku", "納入税額は0以上で入力してください");
		else if (f.getKofuZeigaku() > 99999999999999L)
			errors.put("kofuZeigaku", "納入税額は14桁以内で入力してください");

		// 交付率は画面で入力せず交付率設定から取得するため、未設定なら設定を促す
		if (f.getKofuRitsu() == null)
			errors.put("kofuRitsu", "交付率が設定されていません。交付率設定画面で登録してください");
		else if (f.getKofuRitsu().compareTo(BigDecimal.ZERO) < 0
				|| f.getKofuRitsu().compareTo(new BigDecimal("99999.99")) > 0
				|| f.getKofuRitsu().stripTrailingZeros().scale() > 2)
			errors.put("kofuRitsu", "交付率は整数部5桁、小数部2桁以内で入力してください");

		if (f.getKofuGaku() == null)
			errors.put("kofuGaku", "交付額は必須です");
		else if (f.getKofuGaku() < 0)
			errors.put("kofuGaku", "交付額は0以上で入力してください");
		else if (f.getKofuGaku() > 9999999999999L)
			errors.put("kofuGaku", "交付額は13桁以内で入力してください");

		return errors;
	}

	// ========== 制御用フィールド ==========

	/** 画面モード（view: 照会, edit: 編集, create: 新規登録） */
	private String mode = "view";

	/** 既存レコード存在フラグ */
	private boolean exists = false;

	/** バージョン（楽観的排他制御用） */
	private Integer version;

	/**
	 * 編集可能かどうかを判定
	 */
	public boolean isEditable() {
		return "edit".equals(mode) || "create".equals(mode);
	}

	/**
	 * 照会モードかどうかを判定
	 */
	public boolean isViewMode() {
		return "view".equals(mode);
	}

	/**
	 * 新規登録モードかどうかを判定
	 */
	public boolean isCreateMode() {
		return "create".equals(mode);
	}

	/**
	 * 編集モードかどうかを判定
	 */
	public boolean isEditMode() {
		return "edit".equals(mode);
	}
}
package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

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
	private String nendo;

	/** No.5 納入税額 (t_shoreikin.kofu_zeigaku) */
	private Long kofuZeigaku;

	/** No.6 交付率 (t_shoreikin.kofu_ritsu) */
	private BigDecimal kofuRitsu;

	/** No.7 交付額 (t_shoreikin.kofu_gaku) */
	private Long kofuGaku;

	/** 交付年月日 (t_shoreikin.kofu_ymd) */
	private LocalDate kofuYmd;

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
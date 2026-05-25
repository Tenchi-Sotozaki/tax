package jp.lg.asp.accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 振込先口座照会／登録／編集 DTO
 * 仕様書：振込先口座照会・登録・編集.csv に基づく実装
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FurikomiKozaDto {

	// ========== 特別徴収義務者エリア ==========

	/** No.1 指定番号 (t_furikomi_koza.shitei_no) */
	private String shiteiNo;

	/** No.2 施設名称 (t_tokugimu.shisetsu_name) */
	private String shisetsuName;

	/** No.3 氏名 (m_atena.name) */
	private String name;

	// ========== 振込先口座情報エリア ==========

	/** No.4 金融機関コード (t_furikomi_koza.bank_cd) */
	private String bankCd;

	/** No.5 金融機関名 (t_furikomi_koza.bank_name) */
	private String bankName;

	/** No.6 支店コード (t_furikomi_koza.branch_cd) */
	private String branchCd;

	/** No.7 支店名 (t_furikomi_koza.branch_name) */
	private String branchName;

	/** No.8 預金種目 (t_furikomi_koza.shumoku) */
	private String shumoku;

	/** No.9 口座番号 (t_furikomi_koza.koza_no) */
	private String kozaNo;

	/** No.10 口座名義 (t_furikomi_koza.meigi) */
	private String meigi;

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

	/**
	 * 預金種目の表示名を取得
	 */
	public String getShumokuName() {
		if ("1".equals(shumoku)) {
			return "普通";
		} else if ("2".equals(shumoku)) {
			return "当座";
		}
		return "";
	}
}
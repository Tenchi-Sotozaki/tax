package jp.lg.asp.accommodation.dto;

import lombok.Data;

/**
 * 宿泊税特別徴収事務交付金決定通知書・交付申請書DTO
 */
@Data
public class KofuKetteiTsuchiShinseiDto {

	/** 指定番号 */
	private String shiteiNo;

	/** 年度 */
	private String nendo;

	/** 発行様式 */
	private String hakkoYoshiki;

	/** 市区町村名 */
	private String cityName;

	/** 条例 */
	private String jorei;

	/** 施設住所 */
	private String shisetsuJusho;

	/** 施設名称 */
	private String shisetsuName;

	/** 申告納入金額 */
	private String nonyugaku;

	/** 交付申請額 */
	private String kofugaku;

	/** 交付条件文 */
	private String kofuJoken;

	/** 特別徴収義務者名 */
	private String tokuName;

	/** 発行年月日 */
	private String hakkoYmd;

	/** 交付年月日 */
	private String kofuYmd;
<<<<<<< HEAD
	
	/** 条例 */
=======

	/** 条令 */
>>>>>>> refs/remotes/origin/master
	private String hakkoJorei;

	/** 公印 */
	private byte[] koin;

	/** 印刷対象：決定通知書 */
	private boolean ketteiTsuchi = true;

	/** 印刷対象：交付申請書 */
	private boolean shinsei = true;

	/** 操作（帳票ログ用） */
	private String operation;
}
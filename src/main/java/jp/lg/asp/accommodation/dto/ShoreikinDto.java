package jp.lg.asp.accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 特別徴収事務交付金 DTO
 * 検索フォームおよび一覧表示用を兼ねる。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShoreikinDto {

	// ========== 抽出条件エリア ==========

	/** No.1 交付金年度 (t_shoreikin.nendo) */
	private String nendo;

	/**
	 * No.2 交付金算出有無: 1=算出有 / 2=算出無 / 999=すべて
	 * t_shoreikin レコードの存在有無で判定する。
	 */
	private String kofuSanshutsuUmu = "999";

	/** No.3 指定番号 (t_tokugimu.shitei_no) */
	private String shiteiNo;

	/** No.4 氏名／名称 (m_atena.name) */
	private String name;

	/** No.5 施設名称 (t_tokugimu.shisetsu_name) */
	private String shisetsuName;

	/** No.6 営業種別 (t_tokugimu.kyoka_shu): 1=ホテル/2=旅館/3=簡易宿所/4=民泊/999=すべて */
	private String kyokaShu = "999";

	/** No.7 合算対象 (t_gassan_uchi): 1=非対象/2=対象/999=すべて */
	private String gassanTaisho = "999";

	/**
	 * No.8 ステータス (t_tokugimu の営業・休止日付から判定):
	 * 1=営業中/2=休止/3=廃止/999=すべて
	 */
	private String status = "999";

	/** No.9 個人番号 (m_atena.kojin_no) */
	private String kojinNo;

	/** No.10 法人番号 (m_atena.hojin_no) */
	private String hojinNo;

	// ========== 情報一覧エリア（表示用） ==========

	/** No.12 チェックボックス用 */
	private boolean selected;

	/** No.13 指定番号（一覧表示用） */
	private String listShiteiNo;

	/** No.14 事業所名称（t_tokugimu.shisetsu_name） */
	private String jigyoshoName;

	/** No.15 氏名（m_atena.name） */
	private String shimei;

	/** No.16 交付額 (t_shoreikin.kofu_gaku) */
	private Long kofuGaku;
}

package jp.lg.asp.accommodation.dto;

import java.time.LocalDate;
import java.time.Month;

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
	private String nendo = String.valueOf(
			LocalDate.now().getMonthValue() >= Month.APRIL.getValue()
					? LocalDate.now().getYear()
					: LocalDate.now().getYear() - 1);

	/**
	 * No.2 交付金算出有無: 1=算出有 / 2=算出無 / 999=すべて
	 * t_shoreikin レコードの存在有無で判定する。
	 */
	private String kofuSanshutsuUmu = "999";

	/** No.3 指定番号 (t_tokugimu.shitei_no) */
	private String shiteiNo;

	/** No.4 氏名／名称 (m_atena.name) */
	private String name;
	private String nameMatchType = "partial";

	/** No.5 施設名称 (t_tokugimu.shisetsu_name) */
	private String shisetsuName;
	private String shisetsuNameMatchType = "partial";

	/** No.6 合算指定番号 (t_gassan.gassan_shitei_no) */
	private String gassanShiteiNo;

	/** No.7 ページ番号 */
	private int page = 0;

	/** No.8 1ページあたりの表示件数 */
	private int pageSize = 10;

	// ========== 情報一覧エリア（表示用） ==========

	/** No.13 チェックボックス用 */
	private boolean selected;

	/** No.14 指定番号（一覧表示用） */
	private String listShiteiNo;

	/** No.15 施設名称（t_tokugimu.shisetsu_name） */
	private String listShisetsuName;

	/** No.16 氏名（m_atena.name） */
	private String shimei;

	/** No.17 交付年度 (t_shoreikin.nendo) */
	private Integer kofuNendo;

	/** No.18 交付額 (t_shoreikin.kofu_gaku) */
	private Long kofuGaku;

	/** No.19 交付年月日 (t_shoreikin.kofu_ymd) */
	private LocalDate kofuYmd;
}

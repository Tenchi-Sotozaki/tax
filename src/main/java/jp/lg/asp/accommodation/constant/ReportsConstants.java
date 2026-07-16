package jp.lg.asp.accommodation.constant;

public final class ReportsConstants {

	private ReportsConstants() {
	}

	// 帳票ＩＤ
	// 特別徴収義務者指定通知書
	public static final String TOKUGIMU_SHITEI_TSUCHI = "0000000001";
	// 特別徴収義務者申請受理通知書
	public static final String TOKUGIMU_JURI_TSUCHI = "0000000002";
	// 納税管理人承認（不承認）通知書
	public static final String NOZEI_KANRININ_SHONIN_TSUCHI = "0000000003";
	// 納税管理人選任免除認定（不認定）通知書
	public static final String NOZEI_KANRININ_NINTEI = "0000000004";
	// 納入申告書の提出期限等の特例適用者指定通知書
	public static final String TOKUREI_SHITEI = "0000000005";
	// 納入申告書の提出期限等の特例適用者指定取消通知書
	public static final String TOKUREI_SHITEI_CANCEL = "0000000006";
	// 納入書
	public static final String NONYUSHO = "0000000007";
	// 徴収不能額等の還付又は納入義務の免除決定通知書
	public static final String KANPU_MENJO_TSUCHI = "0000000008";
	// 宿泊税更正（決定）通知書
	public static final String KOSEI_KETTEI_TSUCHI = "0000000009";
	// 合算申告納入承認通知書
	public static final String GASSAN_NONYU_TSUCHI = "0000000010";
	// 宿泊税特別徴収事務交付金交付申請書
	public static final String KOFU_SHINSEI = "0000000011";
	// 宿泊税特別徴収事務交付金交付決定通知書
	public static final String KOFU_KETTEI_TSUCHI = "0000000012";

	// 帳票操作
	public static final String SOUSA_PDF = "1";
	public static final String SOUSA_PREVIEW = "2";
	public static final String SOUSA_PRINT = "3";

	// 区分
	public static final String KBN_TEXT = "1";
	public static final String KBN_DATA = "2";

	// 定義ID
	// 公印
	public static final String KOIN = "RPT0000001";
	// 特別徴収義務者指定通知条令
	public static final String TOKUGIMU_SHITEI_JOREI = "RPT0000002";
	// 特別徴収義務者承認受理通知条令
	public static final String TOKUGIMU_JURI_JOREI = "RPT0000003";
	// 納入申告書の提出期限等の特例適用者指定通知条令
	public static final String TOKUREI_SHITEI_JOREI = "RPT0000005";
	// 特別徴収義務者奨励金交付要綱
	public static final String SHOREIKIN_KOFU_JOREI = "RPT0000006";
	// 更正・決定通知書 法令引用文1
	public static final String KOSEI_KETTEI_HOREI_INYOU1 = "RPT0000007";
	// 更正・決定通知書 法令引用文2
	public static final String KOSEI_KETTEI_HOREI_INYOU2 = "RPT0000008";

	/* 
	 * 操作名変換
	 * @param sousa 操作
	 * @return 操作名
	 */
	public static String resolveSousaName(String sousa) {
		if (sousa == null)
			return "";
		return switch (sousa.strip()) {
		case SOUSA_PDF -> "PDF";
		case SOUSA_PREVIEW -> "プレビュー";
		case SOUSA_PRINT -> "印刷";
		default -> sousa;
		};
	}
}

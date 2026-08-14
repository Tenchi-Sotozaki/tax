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

	// 区分（m_reports_def.kbn の値。値の格納先を表す）
	public static final String KBN_TEXT = "1";
	public static final String KBN_DATA = "2";

	// 入力形式（設定画面に描画する入力欄の種類）
	// kbn とは別物。kbn は def_text / def_data のどちらに値が入るかを表す
	public static final int INPUT_TEXTBOX = 1;
	public static final int INPUT_TEXTAREA = 2;

	// 定義ID
	public enum reportsOutputFiled {

		TOKUGIMU_SHITEI_JOREI("RPT0000002", "特別徴収義務者指定通知条例", INPUT_TEXTBOX),
		TOKUGIMU_JURI_JOREI("RPT0000003", "特別徴収義務者承認受理通知条例", INPUT_TEXTBOX),
		TOKUREI_SHITEI_JOREI("RPT0000005", "納入申告書の提出期限等の特例適用者指定通知条例", INPUT_TEXTBOX),
		SHOREIKIN_KOFU_JOREI("RPT0000006", "特別徴収義務者奨励金交付要綱", INPUT_TEXTBOX),
		KOSEI_KETTEI_HOREI_INYOU1("RPT0000007", "更正・決定通知書 法令引用文1", INPUT_TEXTBOX),
		KOSEI_KETTEI_HOREI_INYOU2("RPT0000008", "更正・決定通知書 法令引用文2", INPUT_TEXTBOX),
		NONYUSHO_KOZA("RPT0000011", "納入書　納入場所", INPUT_TEXTAREA),
		NONYUSHO_KOZA_NO("RPT0000012", "納入書　口座番号", INPUT_TEXTBOX),
		NONYUSHO_SHITEI_KINYU_NAME("RPT0000013", "納入書　指定金融機関名", INPUT_TEXTBOX),
		NONYUSHO_TORIMATOME("RPT0000014", "納入書　取りまとめ店", INPUT_TEXTBOX),
		KOFU_HAKKO_YOSHIKI("RPT0000009", "交付金　発行様式", INPUT_TEXTBOX),
		KOFU_JOKEN("RPT0000010", "交付金　交付条件", INPUT_TEXTAREA),
		GASSAN_NONYU_JOREI("RPT0000015", "合算申告納入承認通知条例", INPUT_TEXTBOX),
		TOKUREI_CANCEL_JOREI("RPT0000016", "納入申告書の提出期限等の特例適用者指定取消通知条例", INPUT_TEXTBOX),
		NOZEI_KANRININ_SHONIN_JOREI("RPT0000017", "納税管理人承認通知条例", INPUT_TEXTBOX),
		NOZEI_KANRININ_NINTEI_JOREI("RPT0000018", "納税管理人選任免除認定通知条例", INPUT_TEXTBOX);

		private final String id; // 帳票定義マスタのID
		private final String name; // 設定画面に表示する定義名
		private final Integer inputType; // 1:テキストボックス、2:テキストエリア

		reportsOutputFiled(String id, String name, Integer inputType) {
			this.id = id;
			this.name = name;
			this.inputType = inputType;
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Integer getInputType() {
			return inputType;
		}
	}

	// 公印
	public static final String KOIN = "RPT0000001";
	// 特別徴収義務者指定通知条例
	public static final String TOKUGIMU_SHITEI_JOREI = "RPT0000002";
	// 特別徴収義務者承認受理通知条例
	public static final String TOKUGIMU_JURI_JOREI = "RPT0000003";
	// 納入申告書の提出期限等の特例適用者指定通知条例
	public static final String TOKUREI_SHITEI_JOREI = "RPT0000005";
	// 特別徴収義務者奨励金交付要綱
	public static final String SHOREIKIN_KOFU_JOREI = "RPT0000006";
	// 更正・決定通知書 法令引用文1
	public static final String KOSEI_KETTEI_HOREI_INYOU1 = "RPT0000007";
	// 更正・決定通知書 法令引用文2
	public static final String KOSEI_KETTEI_HOREI_INYOU2 = "RPT0000008";
	// 発行様式
	public static final String KOFU_HAKKO_YOSHIKI = "RPT0000009";
	// 交付条件
	public static final String KOFU_JOKEN = "RPT0000010";
	// 納入書　納入場所
	public static final String NONYUSHO_KOZA = "RPT0000011";
	// 納入書　口座番号
	public static final String NONYUSHO_KOZA_NO = "RPT0000012";
	// 納入書　指定金融機関名
	public static final String NONYUSHO_SHITEI_KINYU_NAME = "RPT0000013";
	// 納入書　取りまとめ店
	public static final String NONYUSHO_TORIMATOME = "RPT0000014";
	// 合算申告納入承認通知条例
	public static final String GASSAN_NONYU_JOREI = "RPT0000015";
	// 納入申告書の提出期限等の特例適用者指定取消通知条例
	public static final String TOKUREI_CANCEL_JOREI = "RPT0000016";
	// 納税管理人承認通知条例
	public static final String NOZEI_KANRININ_SHONIN_JOREI = "RPT0000017";
	// 納税管理人選任免除認定通知条例
	public static final String NOZEI_KANRININ_NINTEI_JOREI = "RPT0000018";

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

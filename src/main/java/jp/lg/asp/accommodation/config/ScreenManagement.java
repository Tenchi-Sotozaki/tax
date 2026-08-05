package jp.lg.asp.accommodation.config;

/**
 * 画面IDとControllerの対応を一元管理するクラス
 *
 * 各定数値は m_screen.screen_id と一致させる
 * screen_idを変更する場合はここだけ修正する
 *
 */
public final class ScreenManagement {

	private ScreenManagement() {
	}

	/** 画面区分が判別できない場合の表示名 */
	public static final String SCREEN_KBN_OTHER = "その他";

	/**
	 * 画面区分ごとの画面IDと画面名
	 *
	 * 「メニュー再検討_260727-01.xlsx」のメニュー構成に合わせて定義する
	 * 定義した並び順がそのまま画面上の表示順になる
	 * 画面名は m_screen に未登録の画面を表示するための予備であり、
	 * m_screen にレコードがある場合は screen_name を優先する
	 */
	private static final java.util.Map<String, java.util.Map<String, String>> SCREENS_BY_KBN;
	static {
		java.util.Map<String, java.util.Map<String, String>> kbn = new java.util.LinkedHashMap<>();
		putKbn(kbn, "台帳管理",
				"ms00000001", "特別徴収義務者台帳",
				"ms00000006", "合算申請管理台帳",
				"ms00000008", "宛名管理台帳");
		putKbn(kbn, "eLTAX(電子申告)取込",
				"mt00000002", "eLTAX(電子申告)取込");
		putKbn(kbn, "納入申告管理",
				"ms00000004", "納入申告管理",
				"ms00000005", "納入申告登録",
				"ms00000017", "納入書",
				"ms00000023", "宿泊税更正・決定通知書",
				"ms00000027", "徴収不能額等の還付又は納入義務の免除決定通知書");
		putKbn(kbn, "行政システム連携",
				"mo00000001", "収納管理CSV出力",
				"mo00000002", "収納管理情報確認",
				"mo00000003", "交付金CSV出力",
				"mo00000004", "交付金振込情報確認");
		putKbn(kbn, "特別徴収義務者管理",
				"ms00000002", "特別徴収義務者登録",
				"ms00000003", "納税管理人登録",
				"ms00000013", "特別徴収義務者指定通知書",
				"ms00000014", "特別徴収義務者申請受理通知書",
				"ms00000015", "納税管理人承認（不承認）通知書",
				"ms00000016", "納入申告書の提出期限等の特例適用者承認（不承認）通知書",
				"ms00000018", "納入申告書の提出期限等の特例取消通知書",
				"ms00000022", "納税管理人選任免除認定（不認定）通知書",
				"ms00000024", "納入期限特例照会");
		putKbn(kbn, "合算申請管理",
				"mi00000001", "合算申請登録",
				"ms00000021", "合算申告納入承認通知書");
		putKbn(kbn, "交付金管理",
				"ms00000009", "特別徴収事務交付金一覧",
				"ms00000010", "交付金データ一括登録",
				"ms00000011", "交付金データ照会",
				"ms00000012", "振込先口座登録",
				"ms00000019", "宿泊税特別徴収事務交付金申請書",
				"ms00000020", "宿泊税特別徴収事務交付金決定通知書",
				"ms00000028", "交付金帳票一括発行");
		putKbn(kbn, "帳票発行管理",
				"ms00000007", "帳票発行",
				"ms00000025", "帳票発行履歴照会",
				"ms00000026", "帳票発行状況照会");
		putKbn(kbn, "宛名管理",
				"mi00000002", "宛名登録",
				"mt00000001", "宛名CSV取込");
		putKbn(kbn, "システム管理",
				"sc00000001", "帳票発行設定",
				"sc00000002", "賦課方式登録",
				"sc00000004", "権限設定",
				"sc00000005", "交付金交付率設定",
				"sc00000006", "納入期限登録",
				"sc00000008", "自治体情報",
				"sc00000010", "トップページ編集",
				"ss00000001", "ユーザー管理台帳",
				"ss00000002", "ユーザー登録",
				"ss00000004", "賦課方式設定",
				"ss00000006", "納入期限照会",
				"ss00000008", "操作ログ照会",
				"ss00000009", "休業日設定");
		putKbn(kbn, SCREEN_KBN_OTHER,
				"ms00000029", "トップページ",
				"mt00000003", "電子申告情報取込確認",
				"sc00000003", "納税周期登録/編集",
				"sc00000007", "指定番号・合算指定番号設定/編集",
				"ss00000005", "納税周期管理",
				"ss00000007", "指定番号・合算指定番号照会");
		SCREENS_BY_KBN = java.util.Collections.unmodifiableMap(kbn);
	}

	private static void putKbn(java.util.Map<String, java.util.Map<String, String>> kbn,
			String kbnName, String... screenIdAndNames) {
		java.util.Map<String, String> screens = new java.util.LinkedHashMap<>();
		for (int i = 0; i < screenIdAndNames.length; i += 2) {
			screens.put(screenIdAndNames[i], screenIdAndNames[i + 1]);
		}
		kbn.put(kbnName, java.util.Collections.unmodifiableMap(screens));
	}

	/** 画面IDから画面区分の表示名を取得する */
	public static String getScreenKbnName(String screenId) {
		for (java.util.Map.Entry<String, java.util.Map<String, String>> entry : SCREENS_BY_KBN.entrySet()) {
			if (entry.getValue().containsKey(screenId)) {
				return entry.getKey();
			}
		}
		return SCREEN_KBN_OTHER;
	}

	/** 画面区分ごとの「画面ID→画面名」を表示順で取得する */
	public static java.util.Map<String, java.util.Map<String, String>> getScreensByKbn() {
		return SCREENS_BY_KBN;
	}

	// 特別徴収義務者管理台帳
	public static final String TOKUGIMU_DAICHO = "ms00000001";

	// 宛名管理台帳
	public static final String ATENA_DAICHO = "ms00000008";

	// 宛名取込
	public static final String ATENA_INSERT = "mt00000001";

	// 宛名登録/編集/照会
	public static final String ATENA_CONFIG = "mi00000002";

	//特別徴収義務者登録/編集/照会
	public static final String TOKUGIMU_CONFIG = "ms00000002";

	// 納入申告登録
	public static final String FUKA_CONFIG = "ms00000005";

	// 納入申告管理台帳
	public static final String FUKA_DAICHO = "ms00000004";

	// 納税管理人登録/編集/照会
	public static final String TAXMANAGER_CONFIG = "ms00000003";

	// ユーザー検索
	public static final String USER_MANAGEMENT = "ss00000001";

	// ユーザー登録/編集/削除
	public static final String USER_CONFIG = "ss00000002";

	// 権限管理
	public static final String ROLE_MANAGEMENT = "sc00000004";

	// 納税周期管理
	public static final String NOZEI_SHUKI = "ss00000005";

	// 納税周期登録/編集
	public static final String NOZEI_SHUKI_CONFIG = "sc00000003";

	// 電子申告情報取込
	public static final String ELTAX_RENKEI = "mt00000002";

	// 電子申告情報取込確認
	public static final String ELTAX_RENKEI_KAKUNIN = "mt00000003";

	// 合算申請登録/編集/照会
	public static final String GASSAN_CONFIG = "mi00000001";

	// 合算申請管理台帳
	public static final String GASSAN_LIST = "ms00000006";

	// 収納管理情報連携
	public static final String SHUNO_RENKEI = "mo00000001";

	// 収納管理情報確認
	public static final String SHUNO_RENKEI_KAKUNIN = "mo00000002";

	// 交付金振込情報連携
	public static final String KOFUKIN_FURIKOMI = "mo00000003";

	// 交付金振込確認
	public static final String KOFUKIN_FURIKOMI_KAKUNIN = "mo00000004";

	// 賦課方式設定（税率管理マスタ）
	public static final String ZEIRITSU_CONFIG = "sc00000002";

	// 賦課方式照会
	public static final String ZEIRITSU_DAICHO = "ss00000004";

	// 特別徴収事務交付金
	public static final String SHOREIKIN = "ms00000009";

	// 特別徴収事務交付金一括算出
	public static final String SHOREIKIN_BULK = "ms00000010";

	// 特別徴収事務交付金照会/登録/編集
	public static final String SHOREIKIN_CONFIG = "ms00000011";

	// 振込先口座照会/登録/編集
	public static final String FURIKOMI_KOZA = "ms00000012";

	// 帳票発行
	public static final String TOKUGIMU_REPORT = "ms00000007";

	// 特別徴収義務者指定通知書
	public static final String TOKUGIMU_SHITEI_TSUCHI = "ms00000013";

	// 特別徴収義務者申請受理通知書
	public static final String TOKUGIMU_JURI_TSUCHI = "ms00000014";

	// 納税管理人承認(不承認)通知書
	public static final String NOZEI_KANRININ_SHONIN_TSUCHI = "ms00000015";

	// 納税管理人選任免除認定（不認定）通知書
	public static final String NOZEI_KANRININ_NINTEI = "ms00000022";

	// 納入申告書の提出期限等の特例適用者指定通知書
	public static final String TOKUREI_SHITEI = "ms00000016";

	// 納入書
	public static final String NONYUSHO = "ms00000017";

	// 納入申告書の提出期限等の特例適用者指定取消通知書
	public static final String TOKUREI_SHITEI_CANCEL = "ms00000018";

	// 宿泊税特別徴収事務交付金交付申請書
	public static final String KOFU_SHINSEI = "ms00000019";

	// 宿泊税特別徴収事務交付金交付決定通知書
	public static final String KOFU_KETTEI_TSUCHI = "ms00000020";

	// 合算申告納入承認通知書
	public static final String GASSAN_NONYU_TSUCHI = "ms00000021";

	// 宿泊税更生・決定通知書
	public static final String KOSEI_KETTEI_TSUCHI = "ms00000023";

	// 交付率設定
	public static final String KOFU_RITSU_CONFIG = "sc00000005";

	// 適用納税周期登録/編集/照会
	public static final String TEKIYO_NOZEI_SHUKI_CONFIG = "ms00000024";

	// 徴収不能額の還付又は納入義務の免除決定通知書
	public static final String KANPU_MENJO_TSUCHI = "ms00000027";

	// 納入期限登録/編集
	public static final String NOKIGEN_CONFIG = "sc00000006";

	// 納入期限照会
	public static final String NOKIGEN = "ss00000006";

	// 指定番号・合算指定番号設定/編集
	public static final String SHITEI_GASSAN_CONFIG = "sc00000007";

	// 指定番号・合算指定番号照会
	public static final String SHITEI_GASSAN = "ss00000007";

	// 操作ログ照会
	public static final String OPE_LOG_VIEW = "ss00000008";

	// 帳票ログ照会
	public static final String RPT_LOG_VIEW = "ms00000025";

	// 帳票出力設定
	public static final String REPORTS_CONFIG = "sc00000001";

	// 自治体情報設定
	public static final String JICHITAI_CONFIG = "sc00000008";

	// 特別徴収義務者状況照会
	public static final String TOKUGIMU_STATUS_VIEW = "ms00000026";

	// 交付金帳票一括発行
	public static final String KOFUKIN_REPORT_BULK = "ms00000028";

	// トップページ
	public static final String TOP_PAGE = "ms00000029";

	// トップページ編集
	public static final String TOP_PAGE_CONFIG = "sc00000010";

	// 休業日設定
	public static final String HOLIDAY_CONFIG = "ss00000009";
}

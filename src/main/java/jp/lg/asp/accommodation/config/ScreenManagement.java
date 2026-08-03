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

	// トップページ
	public static final String TOP_PAGE = "ms00000029";
}

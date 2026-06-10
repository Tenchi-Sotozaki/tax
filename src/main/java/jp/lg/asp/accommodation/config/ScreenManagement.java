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
	public static final String TOKUGIMU_DAICHO = "TOKUGIMU";

	// 宛名管理台帳
	public static final String ATENA_DAICHO = "ATENA";

	// 宛名取込
	public static final String ATENA_INSERT = "ATENAINS";

	//特別徴収義務者登録/編集/照会
	public static final String TOKUGIMU_CONFIG = "TOKUCONFIG";

	// 納入申告登録
	public static final String FUKA_CONFIG = "DECLARATIO";

	// 納入金額管理台帳
	public static final String FUKA_DAICHO = "FUKA";

	// 納税管理人登録/編集/照会
	public static final String TAXMANAGER_CONFIG = "TAX_MANAGE";

	// ユーザー管理
	public static final String USER_MANAGEMENT = "USER_MGMT";

	// 権限管理
	public static final String ROLE_MANAGEMENT = "ROLE_MGMT";

	// 納税周期管理
	public static final String NOZEI_SHUKI = "NOZEI_SHUK";

	// 納税周期登録/編集
	public static final String NOZEI_SHUKI_CONFIG = "sc00000003";

	// 電子申告情報取込
	public static final String ELTAX_RENKEI = "ELTAX_RENKEI";

	// 電子申告情報取込確認
	public static final String ELTAX_RENKEI_KAKUNIN = "ELTAX_RENKEI_KAKU";

	// 合算申告登録/編集/照会
	public static final String GASSAN_CONFIG = "GASSANCONF";

	// 収納管理情報連携
	public static final String SHUNO_RENKEI = "SHUNO_RENKEI";

	// 交付金振込情報連携
	public static final String KOFUKIN_FURIKOMI = "KOFUKIN_FURIKOMI";

	// 賦課方式設定（税率管理マスタ）
	public static final String ZEIRITSU_CONFIG = "ZEIRITSU";

	// 特別徴収事務交付金
	public static final String SHOREIKIN = "SHOREIKIN";

	// 特別徴収事務交付金一括算出
	public static final String SHOREIKIN_BULK = "SHOREIKIN_BULK";

	// 特別徴収事務交付金照会/登録/編集
	public static final String SHOREIKIN_CONFIG = "SHOREIKIN_CONFGI";

	// 振込先口座照会/登録/編集
	public static final String FURIKOMI_KOZA = "FURIKOMI_KOZA";

	// 帳票出力
	public static final String TOKUGIMU_REPORT = "TOKU_REPORT";

	// 特別徴収義務者指定通知書
	public static final String TOKUGIMU_SHITEI_TSUCHI = "ms00000013";

	// 特別徴収義務者申請受理通知書
	public static final String TOKUGIMU_JURI_TSUCHI = "ms00000014";

	// 納税管理人承認(不承認)通知書
	public static final String NOZEI_KANRININ_SHONIN_TSUCHI = "ms00000015";

	// 納入申告書の提出期限等の特例適用者指定通知書
	public static final String TOKUREI_SHITEI = "ms00000016";

	// 納入書
	public static final String NONYUSHO = "ms00000017";
}

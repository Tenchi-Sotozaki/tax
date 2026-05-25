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

	// 宿泊税申告登録
	public static final String DECLARATION = "DECLARATIO";

	// 納入金額管理台帳
	public static final String FUKADAICHO = "FUKA";

	// 納税管理人登録・編集
	public static final String TAX_MANAGER = "TAX_MANAGE";

	// ユーザー管理
	public static final String USER_MANAGEMENT = "USER_MGMT";

	// 権限管理
	public static final String ROLE_MANAGEMENT = "ROLE_MGMT";

	// 納税周期管理
	public static final String NOZEI_SHUKI = "NOZEI_SHUK";

	// 電子申告情報取込
	public static final String ELTAX_RENKEI = "ELTAX_RENKEI";

	// 電子申告情報取込確認
	public static final String ELTAX_RENKEI_KAKUNIN = "ELTAX_RENKEI_KAKU";

	// 合算申告管理台帳
	public static final String GASSAN_DAICHO = "GASSAN";

	// 合算申告登録/編集/照会
	public static final String GASSAN_CONFIG = "GASSANCONF";

	// 収納管理情報連携
	public static final String SHUNO_RENKEI = "SHUNO_RENKEI";

	// 交付金振込情報連携
	public static final String KOFUKIN_FURIKOMI = "KOFUKIN_FURIKOMI";
}

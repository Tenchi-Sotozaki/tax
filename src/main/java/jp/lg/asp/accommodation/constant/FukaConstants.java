package jp.lg.asp.accommodation.constant;

import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class FukaConstants {

	private final String value;
	private final String name;

	// =========================================================
	// 定数定義
	// =========================================================
	
	// 賦課方式
	public static final FukaConstants TEIGAKU  = new FukaConstants("1", "定額");
	public static final FukaConstants TEIRITSU = new FukaConstants("2", "定率");

	// 変更区分
	public static final FukaConstants SHINKI  = new FukaConstants("1", "新規");
	public static final FukaConstants SHUESEI = new FukaConstants("2", "修正");
	public static final FukaConstants KOSEI   = new FukaConstants("3", "更正");

	// =========================================================
	// 検索用リスト（Enumの values() の代わり）
	// =========================================================
	public static final List<FukaConstants> FUKA_HOSHIKI_LIST = List.of(TEIGAKU, TEIRITSU);
	public static final List<FukaConstants> HENKO_KUBUN_LIST = List.of(SHINKI, SHUESEI, KOSEI);

	// =========================================================
	// 統合したロジック（計算・検索）
	// =========================================================

	/**
	 * コード値("1"など)から賦課方式の定数オブジェクトを取得する
	 * @param value DBなどに保存されているコード値
	 * @return 該当する定数（見つからない場合はnull）
	 */
	public static FukaConstants getFukaHoshiki(String value) {
		return FUKA_HOSHIKI_LIST.stream()
				.filter(c -> c.getValue().equals(value))
				.findFirst()
				.orElse(null);
	}

}
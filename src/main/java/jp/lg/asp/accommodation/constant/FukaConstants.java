package jp.lg.asp.accommodation.constant;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

	/**
	 * 賦課区分に応じた税額計算を行う
	 * @param baseValue 基準値（定額なら宿泊数、定率なら課税対象料金）
	 * @param rate 税率（または定額単価）
	 * @return 計算後の税額
	 */
	public Long calculateTax(Long baseValue, BigDecimal rate) {
		if (baseValue == null || rate == null) return 0L;

		if (this.equals(TEIGAKU)) {
			// 定額制： 宿泊数(baseValue) × 税額(rate)
			return BigDecimal.valueOf(baseValue)
					.multiply(rate)
					.longValue();
			
		} else if (this.equals(TEIRITSU)) {
			// 定率制： 宿泊料金(baseValue) × 税率(rate) / 100 ※端数切り捨て
			// DBにはパーセント値（例: 2.00 = 2%）で格納されている
			return BigDecimal.valueOf(baseValue)
					.multiply(rate)
					.divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
					.longValue();
		}
		
		return 0L; // 賦課方式以外の定数から呼ばれた場合は0を返す
	}
}
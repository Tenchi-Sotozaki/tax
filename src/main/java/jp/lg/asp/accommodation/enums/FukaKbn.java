package jp.lg.asp.accommodation.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 賦課区分（税の計算方式）を表すEnum。
 * 文字列リテラル "1" / "2" の散在を防ぎ、計算ロジックを集約する。
 */
@Getter
@RequiredArgsConstructor
public enum FukaKbn {

	TEIGAKU("1", "定額制") {
		@Override
		public long calculateTax(long rate, long count) {
			// 単価(円) × 宿泊数
			return rate * count;
		}
	},

	TEIRITSU("2", "定率制") {
		@Override
		public long calculateTax(long rate, long count) {
			// 宿泊料金 × 税率(%) / 100（端数切り捨て）
			return (count * rate) / 100;
		}
	};

	private final String code;
	private final String displayName;

	/**
	 * 税額を計算する（Strategyパターン）。
	 * @param rate 定額制なら単価(円)、定率制なら税率(%)
	 * @param count 定額制なら宿泊数、定率制なら宿泊料金
	 * @return 計算された税額
	 */
	public abstract long calculateTax(long rate, long count);

	/**
	 * コード値からEnumを取得する。
	 * null または未知のコードの場合は TEIGAKU（定額制）をデフォルトとして返す。
	 */
	public static FukaKbn fromCode(String code) {
		if (code == null) {
			return TEIGAKU;
		}
		for (FukaKbn kbn : values()) {
			if (kbn.code.equals(code.trim())) {
				return kbn;
			}
		}
		return TEIGAKU;
	}

	/**
	 * 定率制かどうかを判定する。
	 */
	public boolean isTeiritsu() {
		return this == TEIRITSU;
	}

	/**
	 * 定額制かどうかを判定する。
	 */
	public boolean isTeigaku() {
		return this == TEIGAKU;
	}
}

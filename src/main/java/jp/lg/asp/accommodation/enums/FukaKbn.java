package jp.lg.asp.accommodation.enums;

import java.math.BigDecimal;
import java.math.RoundingMode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 賦課区分（税の計算方式）を表すEnum
 */
@Getter
@RequiredArgsConstructor
public enum FukaKbn {

    TEIGAKU("1", "定額制") {
        @Override
        public long calculateTax(BigDecimal rate, long count) {
            // 定額制：単価(円) × 宿泊数
            if (rate == null) return 0L;
            // BigDecimal同士で掛け算し、最終的な税額(円)を long で返す
            return rate.multiply(BigDecimal.valueOf(count)).longValue();
        }
    },

    TEIRITSU("2", "定率制") {
        @Override
        public long calculateTax(BigDecimal rate, long count) {
            // 定率制：宿泊料金 × 税率(%) / 100（端数切り捨て）
            if (rate == null) return 0L;
            return BigDecimal.valueOf(count)
                    .multiply(rate)
                    .divide(BigDecimal.valueOf(100), RoundingMode.DOWN)
                    .longValue();
        }
    };

    private final String code;
    private final String displayName;

    /**
     * 税額を計算する（Strategyパターン）
     * @param rate 定額制なら単価(円)、定率制なら税率(%) ※BigDecimalで受け取る
     * @param count 定額制なら宿泊数、定率制なら宿泊料金
     */
    public abstract long calculateTax(BigDecimal rate, long count);

    /**
     * コード値からEnumを取得する
     */
    public static FukaKbn fromCode(String code) {
        if (code == null) return TEIGAKU; // デフォルト
        for (FukaKbn kbn : values()) {
            if (kbn.code.equals(code)) {
                return kbn;
            }
        }
        return TEIGAKU;
    }

    /**
     * 定率制かどうかを判定する
     */
    public boolean isTeiritsu() {
        return this == TEIRITSU;
    }

    /**
     * 定額制かどうかを判定する
     */
    public boolean isTeigaku() {
        return this == TEIGAKU;
    }
}
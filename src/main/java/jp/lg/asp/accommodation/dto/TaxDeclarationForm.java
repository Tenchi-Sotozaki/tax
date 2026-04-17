package jp.lg.asp.accommodation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Data
public class TaxDeclarationForm {

    private String obligorId;

    private Boolean isCorrection;
    private String  correctionReason;

    /** 3ヶ月分の申告データ（インデックス 0=1ヶ月目, 1=2ヶ月目, 2=3ヶ月目） */
    @Valid
    private List<MonthlyData> months = createEmptyMonths();

    // -------------------------------------------------------------------------
    // インナークラス: 1ヶ月分の申告データ
    // -------------------------------------------------------------------------
    @Data
    public static class MonthlyData {

        @DateTimeFormat(pattern = "yyyy-MM")
        private YearMonth targetYearMonth;

        /** 区分1: 20,000円未満（200円/泊） */
        @Min(value = 0, message = "0以上の値を入力してください")
        private Integer guestCountTier1 = 0;
        private Long    taxAmountTier1  = 0L;

        /** 区分2: 20,000円以上50,000円未満（400円/泊） */
        @Min(value = 0, message = "0以上の値を入力してください")
        private Integer guestCountTier2 = 0;
        private Long    taxAmountTier2  = 0L;

        /** 区分3: 50,000円以上（1,000円/泊） */
        @Min(value = 0, message = "0以上の値を入力してください")
        private Integer guestCountTier3 = 0;
        private Long    taxAmountTier3  = 0L;

        /** 課税対象外（免税） */
        @Min(value = 0, message = "0以上の値を入力してください")
        private Integer exemptGuestCount = 0;

        /** 合計（自動計算） */
        private Integer totalGuestCount = 0;
        private Long    totalTaxAmount  = 0L;

        /** サーバー側で合計を再計算する */
        public void recalculate() {
            this.taxAmountTier1  = (long) nvl(guestCountTier1) * 200;
            this.taxAmountTier2  = (long) nvl(guestCountTier2) * 400;
            this.taxAmountTier3  = (long) nvl(guestCountTier3) * 1000;
            this.totalGuestCount = nvl(guestCountTier1) + nvl(guestCountTier2)
                                 + nvl(guestCountTier3) + nvl(exemptGuestCount);
            this.totalTaxAmount  = taxAmountTier1 + taxAmountTier2 + taxAmountTier3;
        }

        private int nvl(Integer v) { return v != null ? v : 0; }
    }

    // -------------------------------------------------------------------------
    // ユーティリティ
    // -------------------------------------------------------------------------

    /** 空の3ヶ月分リストを生成する */
    public static List<MonthlyData> createEmptyMonths() {
        List<MonthlyData> list = new ArrayList<>();
        for (int i = 0; i < 3; i++) list.add(new MonthlyData());
        return list;
    }

    /** 指定インデックスの月データを安全に取得（null安全） */
    public MonthlyData getMonth(int index) {
        if (months == null || index >= months.size()) return new MonthlyData();
        MonthlyData m = months.get(index);
        return m != null ? m : new MonthlyData();
    }
}

package jp.lg.asp.accommodation.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * 月計表（モーダル）の入力を保持するDTO
 */
@Data
public class FukaMonthlyTallyDto {

    // 1日〜31日分の日別データリスト
    private List<DailyItem> dailyItems = new ArrayList<>();

    /**
     * 1日分の入力項目
     */
    @Data
    public static class DailyItem {
        private Integer day;            // 日付 (1〜31)
     // index 0 -> 区分①(hakusu1), 1 -> 区分②(hakusu2)...
        private List<Integer> taxCategoryCounts = new ArrayList<>();
        private Integer exemptCount;    // 免除・対象外の宿泊数
    }

    // 初期化：リストに31日分の空のオブジェクトを詰めておく
    public FukaMonthlyTallyDto() {
        for (int i = 1; i <= 31; i++) {
            DailyItem item = new DailyItem();
            item.setDay(i);
            dailyItems.add(item);
        }
    }
    /**
     * 初期化メソッド
     * @param categoryCount 税区分の数（マスタから取得した数）
     */
    public void initialize(int categoryCount) {
        this.dailyItems = new ArrayList<>();
        for (int i = 1; i <= 31; i++) {
            DailyItem item = new DailyItem();
            item.setDay(i);
            // 💡 税区分の数だけ 0 で初期化しておく
            for (int j = 0; j < categoryCount; j++) {
                item.getTaxCategoryCounts().add(0);
            }
            item.setExemptCount(0);
            this.dailyItems.add(item);
        }
    }
}
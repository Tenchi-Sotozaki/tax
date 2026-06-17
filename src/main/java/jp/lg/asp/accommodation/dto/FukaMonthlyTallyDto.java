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
		private Integer day; // 日付 (1〜31)
		private List<Integer> hakusu = new ArrayList<>();
		private List<Long> ryokin = new ArrayList<>();
		private List<Long> sogaku = new ArrayList<>();
		private Integer menjoHakusu;
		private Long menjoRyokin;
		private Long zeigaku;
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
			for (int j = 0; j < categoryCount; j++) {
				item.getHakusu().add(0);
				item.getRyokin().add(0L);
				item.getSogaku().add(0L);
			}
			item.setMenjoHakusu(0);
			item.setZeigaku(0L);
			this.dailyItems.add(item);
		}
	}
}

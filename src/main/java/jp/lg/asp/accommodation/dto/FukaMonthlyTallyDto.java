package jp.lg.asp.accommodation.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;

import lombok.Data;

/**
 * 月計表（モーダル）の入力を保持するDTO
 */
@Data
public class FukaMonthlyTallyDto {

	// 1日〜31日分の日別データリスト
	@Valid
	private List<DailyItem> dailyItems = new ArrayList<>();

	/**
	 * 1日分の入力項目
	 */
	@Data
	public static class DailyItem {
		private Integer day; // 日付 (1〜31)
		private List<@Digits(integer = 8, fraction = 0, message = "8桁以内で入力してください") Integer> hakusu = new ArrayList<>();
		private List<@Digits(integer = 13, fraction = 0, message = "13桁以内で入力してください") Long> ryokin = new ArrayList<>();
		private List<@Digits(integer = 13, fraction = 0, message = "13桁以内で入力してください") Long> sogaku = new ArrayList<>();
		@Digits(integer = 8, fraction = 0, message = "8桁以内で入力してください")
		private Integer menjoHakusu;
		@Digits(integer = 13, fraction = 0, message = "13桁以内で入力してください")
		private Long menjoRyokin;
		@Digits(integer = 13, fraction = 0, message = "13桁以内で入力してください")
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
			item.setMenjoRyokin(0L);
			item.setZeigaku(0L);
			this.dailyItems.add(item);
		}
	}
}

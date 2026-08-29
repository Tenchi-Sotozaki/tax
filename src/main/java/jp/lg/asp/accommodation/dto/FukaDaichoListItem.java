package jp.lg.asp.accommodation.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class FukaDaichoListItem {
	// 表示用項目
	private String displayNengetsu; // カラム2: 年月 (例: "4月")[cite: 1]
	private Long amount; // カラム3: 金額[cite: 1]
	private Long totalZeigaku; // 宿泊税額
	private Long cityZeigaku; // 市区町村税額
	private Long kenZeigaku; // 	都道府県税額
	private boolean shinkokuZumi; // 申告済・未の判定フラグ
	private String nonyuStatus; // 納入状況 ("paid", "partial", "unpaid")
	private String displayKigen; // 申告・納入期限

	// 内部処理・遷移用
	private String nendo; // 年度
	private Integer kibetsu; // 期別
	private String targetYearMonth; // 登録画面等への遷移パラメータ用
	private LocalDate shinkokuYmd; // 申告日（判定用）
	private boolean gassanTarget; // 合算納入対象月フラグ
}

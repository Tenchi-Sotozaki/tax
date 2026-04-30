package jp.lg.asp.accommodation.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class FukaDaichoListItem {
    // 表示用項目
    private String displayNengetsu; // カラム2: 年月 (例: "4月")[cite: 1]
    private Long amount;            // カラム3: 金額[cite: 1]
    private String status;          // カラム4: ステータス (済/未)[cite: 1]
    private String displayNoki;     // 納期 (例: "5月末")[cite: 1]

    // 内部処理・遷移用
    private String nendo;           // 年度
    private Integer kibetsu;        // 期別
    private LocalDate targetYearMonth; // 登録画面等への遷移パラメータ用
    private LocalDate shinkokuYmd;  // 申告日（判定用）
    private boolean shinkokuZumi;   // 済・未の判定フラグ
    
    // 合計金額計算用（Serviceで使用）
    private Long totalZeigaku;
}
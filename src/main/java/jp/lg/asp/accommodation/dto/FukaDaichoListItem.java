package jp.lg.asp.accommodation.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FukaDaichoListItem {
    
    private Integer nendo;
    private Integer kibetsu;
    
    // 画面表示用項目
    private String displayNengetsu; // 例: "4月" または "4月〜6月" (kibetsuから計算)
    private Long totalZeigaku;      // 金額
    private boolean isShinkokuZumi; // 申告済みかどうか（shinkokuYmd != null）
    private LocalDate shinkokuYmd;  // 実際の申告日
    private String displayNoki;     // 納期 (m_nozei_shukiから計算)
}
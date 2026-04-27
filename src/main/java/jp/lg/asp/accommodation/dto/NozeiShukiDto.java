package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NozeiShukiDto {

    private BigDecimal seq;   // セレクトのvalue値
    private BigDecimal shuki; // 表示ラベル用（納税周期の月数）

    public String getLabel() {
        if (shuki == null) return "";
        return switch (shuki.intValue()) {
            case 1  -> "毎月";
            case 3  -> "3ヶ月";
            case 6  -> "6ヶ月";
            case 12 -> "年1回";
            default -> shuki.intValue() + "ヶ月";
        };
    }
}

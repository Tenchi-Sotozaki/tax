package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class NozeiShukiDto {

    private BigDecimal seq;
    private BigDecimal shuki;
    private String shinkokuKigen;

    public NozeiShukiDto(BigDecimal seq, BigDecimal shuki) {
        this.seq = seq;
        this.shuki = shuki;
    }

    public NozeiShukiDto(BigDecimal seq, BigDecimal shuki, int nendoStMonth) {
        this.seq = seq;
        this.shuki = shuki;
        this.shinkokuKigen = calcShinkokuKigen(nendoStMonth);
    }

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

    private String calcShinkokuKigen(int nendoStMonth) {
        if (shuki == null) return "";
        int s = shuki.intValue();
        if (s == 1) return "翌月末日";

        List<String> months = new ArrayList<>();
        // 年度開始月から周期ごとに申告期限月（対象期間終了月の翌月）を算出
        for (int i = 0; i < 12 / s; i++) {
            int endMonth = ((nendoStMonth - 1) + (i + 1) * s) % 12 + 1;
            months.add(endMonth + "月末日");
        }
        return String.join(",", months);
    }
}

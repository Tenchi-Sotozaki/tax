package jp.lg.asp.accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaxCycleInfoDto {
    private String cycleCategory; // 例: 毎月
    private String period;        // 例: 1日〜末日
    private String deadline;      // 例: 翌月末
}

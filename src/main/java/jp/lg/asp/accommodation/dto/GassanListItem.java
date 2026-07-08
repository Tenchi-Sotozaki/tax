package jp.lg.asp.accommodation.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GassanListItem {

    private String gassanShiteiNo;
    private String atenaName;
    private LocalDate tekiyoStYmd;
    private LocalDate tekiyoEdYmd;
    private int uchiCount;
}

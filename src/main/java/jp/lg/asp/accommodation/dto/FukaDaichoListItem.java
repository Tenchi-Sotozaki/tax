package jp.lg.asp.accommodation.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FukaDaichoListItem {
    private String nendo;
    private Integer kibetsu;
    
    // HTML側が求めている全ての名前を網羅する
    private String displayNengetsu;    
    private LocalDate targetYearMonth; 
    private Long amount;               // ${record.amount} 用
    private Long totalZeigaku;         // line 91: ${item.totalZeigaku} 用
    private String status;             
    
    private boolean isShinkokuZumi;
    private LocalDate shinkokuYmd;
    private String displayNoki;
}
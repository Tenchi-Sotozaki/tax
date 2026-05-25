package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShoreikinRenkeiDto {

    private String jichitaiCd;
    private String shiteiNo;
    private String atenaNo;
    private String name;
    private String nendo;
    private Long kofuZeigaku;
    private BigDecimal kofuRitsu;
    private Long kofuGaku;
    private LocalDate kofuYmd;
    
    // 振込口座情報
    private String bankCd;
    private String bankName;
    private String branchCd;
    private String branchName;
    private String shumoku;
    private String kozaNo;
    private String meigi;

    @Getter
    @Setter
    public static class Key {
        private String shiteiNo;
        private String nendo;
    }
}
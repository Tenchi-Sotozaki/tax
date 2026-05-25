package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShunoDto {

    private String jichitaiCd;
    private String shiteiNo;
    private String atenaNo;
    private String name;
    private String taishoYm; // formatted yyyy-MM
    private Long totalZeigaku;
    private LocalDate torokuYmd;
    private LocalDate shinkokuYmd;

    // additional fields
    private String nendo;
    private Integer kibetsu;
    private String kasanKbn;
    private BigDecimal kasanRitsu;
    private Long kasanGaku;
    private LocalDate nokigen;
    private Long cityZeigaku;
    private Long kenZeigaku;

    @Getter
    @Setter
    public static class Key {
        private String shiteiNo;
        private String nendo;
        private Integer kibetsu;
    }
}

package jp.lg.asp.accommodation.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KofukinFurikomiDto {

    private String jichitaiCd;
    private String shiteiNo;
    private String atenaNo;
    private String name;
    private String taishoYm; // formatted yyyy-MM
    private LocalDate torokuYmd;
    private LocalDate furikomiYmd;
    private Long furikomiGaku;
    private Long shiharaiGaku;
    private Long tesuryo;
    private String furikomiKbn;
    private String furikomiStatus;
    private String ginkoCd;
    private String ginkoName;
    private String shitenCd;
    private String shitenName;
    private String yokinShubetsu;
    private String kozaNo;
    private String kozaMeigi;
    private String biko;
    private Integer rno;

    @Getter
    @Setter
    public static class Key {
        private String shiteiNo;
        private String taishoYm;
        private Integer rno;
    }
}
package jp.lg.asp.accommodation.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "t_fuka")
@IdClass(FukaId.class) // 既存のFukaIdクラスを使用する[cite: 2, 3]
@Getter 
@Setter
public class Fuka {

    // === 主キー（PK）===
    @Id
    @Column(name = "jichitai_cd", length = 5)
    private String jichitaiCd;

    @Id 
    @Column(name = "shitei_no", length = 8)
    private String shiteiNo;

    @Id
    @Column(name = "rno")
    private Integer rno;

    @Id
    @Column(name = "nendo", length = 4)
    private String nendo; 

    @Id
    @Column(name = "kibetsu")
    private Integer kibetsu;

    // === データ項目（不足分を追記）===
    @Column(name = "toroku_ymd")
    private LocalDate torokuYmd;

    @Column(name = "shinkoku_ymd")
    private LocalDate shinkokuYmd;

    @Column(name = "taisho_ym", length = 6)
    private String taishoYm; // 納入年月

    @Column(name = "fuka_kbn", length = 1)
    private String fukaKbn;

    @Column(name = "henko_kbn", length = 1)
    private String henkoKbn;

    @Column(name = "henko_riyu", length = 400)
    private String henkoRiyu;

    @Column(name = "kazei_hakusu")
    private Integer kazeiHakusu;

    @Column(name = "kazei_ryokin")
    private Long kazeiRyokin;

    @Column(name = "zeigaku")
    private Long zeigaku;

    @Column(name = "menjo_hakusu")
    private Integer menjoHakusu; // 課税対象外

    @Column(name = "menjo_ryokin")
    private Long menjoRyokin;

    @Column(name = "total_hakusu")
    private Integer totalHakusu; // 合計宿泊数

    @Column(name = "total_zeigaku")
    private Long totalZeigaku; // 合計税額

    @Column(name = "city_zeigaku")
    private Long cityZeigaku;

    @Column(name = "ken_zeigaku")
    private Long kenZeigaku;

    @Column(name = "kasan_kbn", length = 1)
    private String kasanKbn;

    @Column(name = "kasan_ritsu")
    private BigDecimal kasanRitsu;

    @Column(name = "kasan_gaku")
    private Long kasanGaku;

    @Column(name = "nokigen")
    private LocalDate nokigen;

    // === システム制御項目 ===
    @Column(name = "new_flg", length = 1)
    private String newFlg;

    @Column(name = "del_flg", length = 1)
    private String delFlg;

    @Column(name = "add_dt")
    private LocalDateTime addDt;

    @Column(name = "add_user", length = 20)
    private String addUser;

    @Column(name = "upd_dt")
    private LocalDateTime updDt;

    @Column(name = "upd_user", length = 20)
    private String updUser;

    @Column(name = "version")
    private Integer version;
}
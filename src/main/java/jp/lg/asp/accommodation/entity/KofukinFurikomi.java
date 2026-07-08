package jp.lg.asp.accommodation.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "t_kofukin_furikomi")
@IdClass(KofukinFurikomiId.class)
@Getter
@Setter
public class KofukinFurikomi extends BaseEntity {

    @Id
    @Column(name = "jichitai_cd", length = 5)
    private String jichitaiCd;

    @Id
    @Column(name = "shitei_no", length = 8)
    private String shiteiNo;

    @Id
    @Column(name = "taisho_ym", length = 6)
    private String taishoYm;

    @Id
    @Column(name = "rno")
    private Integer rno;

    @Column(name = "toroku_ymd")
    private LocalDate torokuYmd;

    @Column(name = "furikomi_ymd")
    private LocalDate furikomiYmd;

    @Column(name = "furikomi_gaku")
    private Long furikomiGaku;

    @Column(name = "shiharai_gaku")
    private Long shiharaiGaku;

    @Column(name = "tesuryo")
    private Long tesuryo;

    @Column(name = "furikomi_kbn", length = 1)
    private String furikomiKbn;

    @Column(name = "furikomi_status", length = 1)
    private String furikomiStatus;

    @Column(name = "ginko_cd", length = 4)
    private String ginkoCd;

    @Column(name = "ginko_name", length = 100)
    private String ginkoName;

    @Column(name = "shiten_cd", length = 3)
    private String shitenCd;

    @Column(name = "shiten_name", length = 100)
    private String shitenName;

    @Column(name = "yokin_shubetsu", length = 1)
    private String yokinShubetsu;

    @Column(name = "koza_no", length = 8)
    private String kozaNo;

    @Column(name = "koza_meigi", length = 100)
    private String kozaMeigi;

    @Column(name = "biko", length = 400)
    private String biko;

    @Column(name = "new_flg", length = 1)
    private String newFlg;

    @Column(name = "del_flg", length = 1)
    private String delFlg;
}
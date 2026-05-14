package jp.lg.asp.accommodation.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import lombok.Data;

/**
 * 宿泊税内訳エンティティ
 */
@Entity
@Table(name = "t_fuka_uchi")
@IdClass(FukaUchiId.class)
@Data
public class FukaUchi {

    // 1. 複合主キー（PK）エリア
    @Id
    @Column(name = "jichitai_cd")
    private String jichitaiCd;

    @Id
    @Column(name = "shitei_no")
    private String shiteiNo;

    @Id
    @Column(name = "rno")
    private Integer rno;

    @Id
    @Column(name = "nendo")
    private String nendo;

    @Id
    @Column(name = "kibetsu")
    private Integer kibetsu;

    @Id
    @Column(name = "kazei_kbn")
    private Integer kazeiKbn;

    // 2. データエリア
    // numeric(8) なので Integer で OK だぜ
    @Column(name = "zeiritsu_seq")
    private Integer zeiritsuSeq;

    @Column(name = "fuka_kbn")
    private String fukaKbn;

    @Column(name = "ryokin_sogaku")
    private Long ryokinSogaku; // numeric(13)

    @Column(name = "hakusu")
    private Integer hakusu; // numeric(8)

    @Column(name = "ryokin")
    private Long ryokin; // numeric(13)

    @Column(name = "zei_ritsu")
    private BigDecimal zeiRitsu; // numeric(12,2)

    @Column(name = "zeigaku")
    private Long zeigaku; // numeric(13)

    @Column(name = "city_zeigaku")
    private Long cityZeigaku; // numeric(13)

    @Column(name = "ken_zeigaku")
    private Long kenZeigaku; // numeric(13)

    // 3. 共通監査項目 (Audit Fields)
    @Column(name = "add_dt")
    private LocalDateTime addDt;

    @Column(name = "add_user")
    private String addUser;

    @Column(name = "upd_dt")
    private LocalDateTime updDt;

    @Column(name = "upd_user")
    private String updUser;

    @Column(name = "version")
    private Integer version;

    @Transient
    private Integer teigakuSeq;
}
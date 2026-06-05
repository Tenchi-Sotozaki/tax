package jp.lg.asp.accommodation.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "t_fuka_uchi")
@IdClass(ChoshuGenboUchiId.class)
public class ChoshuGenboUchi extends BaseEntity {

    @Id
    @Column(name = "jichitai_cd", length = 5, nullable = false)
    private String jichitaiCd;

    @Id
    @Column(name = "shitei_no", length = 8, nullable = false)
    private String shiteiNo;

    @Id
    @Column(name = "rno", nullable = false)
    private Integer rno;

    @Id
    @Column(name = "nendo", length = 4, nullable = false)
    private String nendo;

    @Id
    @Column(name = "kibetsu", nullable = false)
    private Integer kibetsu;

    @Id
    @Column(name = "kazei_kbn", nullable = false)
    private Integer kazeiKbn;

    // --- その他のカラム ---

    @Column(name = "zeiritsu_seq", nullable = false)
    private Long zeiritsuSeq;

    @Column(name = "fuka_kbn", length = 1, nullable = false)
    private String fukaKbn;

    @Column(name = "ryokin_sogaku")
    private Long ryokinSogaku;

    @Column(name = "hakusu")
    private Long hakusu;

    @Column(name = "ryokin")
    private Long ryokin;

    @Column(name = "zei_ritsu", nullable = false, precision = 12, scale = 2)
    private BigDecimal zeiRitsu;

    @Column(name = "zeigaku", nullable = false)
    private Long zeigaku;

    @Column(name = "city_zeigaku")
    private Long cityZeigaku;

    @Column(name = "ken_zeigaku")
    private Long kenZeigaku;

    // バージョン等は BaseEntity に定義されているため、ここでは除外しました
}
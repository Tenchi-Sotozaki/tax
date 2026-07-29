package jp.lg.asp.accommodation.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 宛名連携詳細
 *
 * 宛名情報取込において、取り込んだ宛名1件ごとの処理結果を保持する。
 */
@Entity
@Table(name = "t_atena_renkei_def")
@IdClass(AtenaRenkeiDefId.class)
@Getter
@Setter
public class AtenaRenkeiDef extends BaseEntity {

    /** 区分：差異なし */
    public static final String KBN_SAI_NASHI = "1";
    /** 区分：取込 */
    public static final String KBN_TORIKOMI = "2";
    /** 区分：スキップ */
    public static final String KBN_SKIP = "3";

    @Id
    @Column(name = "jichitai_cd", length = 5)
    private String jichitaiCd;

    @Id
    @Column(name = "seq", precision = 8)
    private BigDecimal seq;

    @Id
    @Column(name = "atena_no", precision = 15)
    private BigDecimal atenaNo;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "kbn", nullable = false, length = 1)
    private String kbn;
}

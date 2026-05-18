package jp.lg.asp.accommodation.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "t_atena_renkei")
@IdClass(AtenaRenkeiId.class)
@Getter
@Setter
public class AtenaRenkei extends BaseEntity {

    @Id
    @Column(name = "jichitai_cd", length = 5)
    private String jichitaiCd;

    @Id
    @Column(name = "seq", precision = 8)
    private BigDecimal seq;

    @Column(name = "file_name", nullable = false, length = 256)
    private String fileName;

    @Column(name = "shori_dt")
    private LocalDateTime shoriDt;

    @Column(name = "shori_kensu", precision = 8)
    private BigDecimal shoriKensu;

    @Column(name = "shinki_kensu", precision = 8)
    private BigDecimal shinkiKensu;

    @Column(name = "koshin_kensu", precision = 8)
    private BigDecimal koshinKensu;
}

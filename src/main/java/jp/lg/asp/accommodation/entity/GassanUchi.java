package jp.lg.asp.accommodation.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "t_gassan_uchi")
@IdClass(GassanUchiId.class)
@Getter @Setter
public class GassanUchi extends BaseEntity {

    @Id
    @Column(name = "jichitai_cd", length = 5)
    private String jichitaiCd;

    @Id
    @Column(name = "gassan_shitei_no", length = 8)
    private String gassanShiteiNo;

    @Id
    @Column(name = "rno", precision = 3)
    private BigDecimal rno;

    @Id
    @Column(name = "shitei_no", length = 8)
    private String shiteiNo;

}
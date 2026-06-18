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
@Table(name = "t_gassan")
@IdClass(GassanId.class)
@Getter
@Setter
public class Gassan extends BaseEntity {

    @Id
    @Column(name = "jichitai_cd", length = 5)
    private String jichitaiCd;

    @Id
    @Column(name = "gassan_shitei_no", length = 8)
    private String gassanShiteiNo;

    @Id
    @Column(name = "rno", precision = 3)
    private BigDecimal rno;

    @Column(name = "toroku_ymd", nullable = false)
    private LocalDate torokuYmd;

    @Column(name = "shinkoku_ymd", nullable = false)
    private LocalDate shinkokuYmd;

    @Column(name = "atena_no", nullable = false, precision = 15)
    private BigDecimal atenaNo;

    @Column(name = "shitei_no", length = 8)
    private String shiteiNo;

    @Column(name = "tekiyo_st_ymd", nullable = false)
    private LocalDate tekiyoStYmd;

    @Column(name = "tekiyo_ed_ymd")
    private LocalDate tekiyoEdYmd;

    @Column(name = "new_flg", nullable = false, length = 1)
    private String newFlg;

    @Column(name = "del_flg", nullable = false, length = 1)
    private String delFlg;
}

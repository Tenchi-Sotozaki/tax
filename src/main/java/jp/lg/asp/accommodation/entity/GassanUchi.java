package jp.lg.asp.accommodation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_gassan_uchi")
@IdClass(GassanUchiId.class)
@Getter @Setter
public class GassanUchi {

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

    @Column(name = "add_dt", nullable = false)
    private LocalDateTime addDt;

    @Column(name = "add_user", nullable = false, length = 20)
    private String addUser;

    @Column(name = "upd_dt", nullable = false)
    private LocalDateTime updDt;

    @Column(name = "upd_user", nullable = false, length = 20)
    private String updUser;

    @Column(name = "version", nullable = false, precision = 5)
    private BigDecimal version;
}
package jp.lg.asp.accommodation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "m_atena")
@IdClass(AtenaId.class)
@Getter @Setter
public class Atena {

    @Id
    @Column(name = "jichitai_cd", length = 5)
    private String jichitaiCd;

    @Id
    @Column(name = "atena_no", precision = 15)
    private BigDecimal atenaNo;

    @Column(name = "kbn", nullable = false, length = 1)
    private String kbn;

    @Column(name = "kojin_no", length = 64)
    private String kojinNo;

    @Column(name = "hojin_no", length = 13)
    private String hojinNo;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "name_kana", length = 200)
    private String nameKana;

    @Column(name = "yubin_no", length = 10)
    private String yubinNo;

    @Column(name = "jusho", length = 200)
    private String jusho;

    @Column(name = "tel1", length = 20)
    private String tel1;

    @Column(name = "tel2", length = 20)
    private String tel2;

    @Column(name = "add_user", nullable = false, length = 20)
    private String addUser;

    @Column(name = "upd_dt", nullable = false)
    private LocalDateTime updDt;

    @Column(name = "upd_user", nullable = false, length = 20)
    private String updUser;

    @Column(name = "version", nullable = false, precision = 5)
    private BigDecimal version;
}
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
@Table(name = "m_atena")
@IdClass(AtenaId.class)
@Getter @Setter
public class Atena extends BaseEntity {

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

}
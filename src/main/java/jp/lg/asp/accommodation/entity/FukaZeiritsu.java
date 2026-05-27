package jp.lg.asp.accommodation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "m_zeiritsu")
@IdClass(FukaZeiritsuId.class)
@Getter
@Setter
public class FukaZeiritsu {

    @Id
    @Column(name = "jichitai_cd", length = 5)
    private String jichitaiCd;

    @Id
    @Column(name = "seq")
    private Integer seq;

    @Column(name = "taisho_kbn", length = 1)
    private String taishoKbn;

    @Column(name = "tekiyo_st_ym", length = 6)
    private String tekiyoStYm;

    @Column(name = "tekiyo_ed_ym", length = 6)
    private String tekiyoEdYm;

    @Column(name = "fuka_kbn", length = 1)
    private String fukaKbn;

    @Column(name = "del_flg", length = 1)
    private String delFlg;
}

package jp.lg.asp.accommodation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "t_furikomi_koza")
@IdClass(FurikomiKozaId.class)
@Getter
@Setter
public class FurikomiKoza extends BaseEntity {

    @Id
    @Column(name = "jichitai_cd", length = 5)
    private String jichitaiCd;

    @Id
    @Column(name = "shitei_no", length = 8)
    private String shiteiNo;

    @Column(name = "bank_cd", length = 4)
    private String bankCd;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "branch_cd", length = 3)
    private String branchCd;

    @Column(name = "branch_name", length = 100)
    private String branchName;

    @Column(name = "shumoku", length = 1)
    private String shumoku;

    @Column(name = "koza_no", length = 8)
    private String kozaNo;

    @Column(name = "meigi", length = 100)
    private String meigi;

    @Column(name = "new_flg", length = 1)
    private String newFlg;

    @Column(name = "del_flg", length = 1)
    private String delFlg;
}
package jp.lg.asp.accommodation.entity;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;
@Entity
@Table(name = "t_fuka")
@IdClass(FukaId.class) 
@Getter 
@Setter
public class Fuka extends BaseEntity {

    @Id
    @Column(name = "jichitai_cd", length = 5)
    private String jichitaiCd;


    @Column(name = "shitei_no", length = 8)
    private String shiteiNo;

    @Id
    @Column(name = "rno")
    private Integer rno;

    @Id
    @Column(name = "nendo", length = 4)
    private String nendo; 

    @Id
    @Column(name = "kibetsu")
    private Integer kibetsu;

    @Column(name = "shinkoku_ymd")
    private LocalDate shinkokuYmd;

    @Column(name = "total_zeigaku")
    private Long totalZeigaku;

    @Column(name = "new_flg", length = 1)
    private String newFlg;

    @Column(name = "del_flg", length = 1)
    private String delFlg;


}
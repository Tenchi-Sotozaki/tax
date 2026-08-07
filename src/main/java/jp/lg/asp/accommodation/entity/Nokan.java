package jp.lg.asp.accommodation.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 納税管理人エンティティ
 */
@Data
@Entity
@Table(name = "t_nokan")
@IdClass(NokanId.class)
public class Nokan {

    /** 自治体コード */
    @Id
    @Column(name = "jichitai_cd")
    private String jichitaiCd;

    /** 指定番号 */
    @Id
    @Column(name = "shitei_no")
    private String shiteiNo;

    /** 宛名番号 */
    @Column(name = "atena_no")
    private BigDecimal atenaNo;

    /** 郵便番号 */
    @Column(name = "yubin_no")
    private String yubinNo;

    /** 住所 */
    @Column(name = "jusho")
    private String jusho;

    /** 氏名 */
    @Column(name = "name")
    private String name;

    /** 削除フラグ */
    @Column(name = "del_flg")
    private String delFlg;
    
    /** 区分 */
    @Column(name = "kbn")
    private String kbn;
}
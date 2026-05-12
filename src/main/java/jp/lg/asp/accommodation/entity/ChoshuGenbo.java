package jp.lg.asp.accommodation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.Data;

@Entity
@Table(name = "t_choshu_genbo")
@IdClass(ChoshuGenboId.class) // 💡 複合主キーを指定
@Data
public class ChoshuGenbo {

    @Id private String jichitaiCd;
    @Id private String shiteiNo;
    @Id private Integer rno;
    @Id private String nendo;
    @Id private Integer kibetsu;

 @Column(name = "uchi_idx_1") private Long uchiIdx1;
 @Column(name = "uchi_idx_2") private Long uchiIdx2;
 @Column(name = "uchi_idx_3") private Long uchiIdx3;
 @Column(name = "uchi_idx_4") private Long uchiIdx4;
 @Column(name = "uchi_idx_5") private Long uchiIdx5;
 @Column(name = "uchi_idx_6") private Long uchiIdx6;
 @Column(name = "uchi_idx_7") private Long uchiIdx7;
 @Column(name = "uchi_idx_8") private Long uchiIdx8;
 @Column(name = "uchi_idx_9") private Long uchiIdx9;
 @Column(name = "uchi_idx_10") private Long uchiIdx10;
 @Column(name = "uchi_idx_11") private Long uchiIdx11;
 @Column(name = "uchi_idx_12") private Long uchiIdx12;
 @Column(name = "uchi_idx_13") private Long uchiIdx13;
 @Column(name = "uchi_idx_14") private Long uchiIdx14;
 @Column(name = "uchi_idx_15") private Long uchiIdx15;
 @Column(name = "uchi_idx_16") private Long uchiIdx16;
 @Column(name = "uchi_idx_17") private Long uchiIdx17;
 @Column(name = "uchi_idx_18") private Long uchiIdx18;
 @Column(name = "uchi_idx_19") private Long uchiIdx19;
 @Column(name = "uchi_idx_20") private Long uchiIdx20;
 @Column(name = "uchi_idx_21") private Long uchiIdx21;
 @Column(name = "uchi_idx_22") private Long uchiIdx22;
 @Column(name = "uchi_idx_23") private Long uchiIdx23;
 @Column(name = "uchi_idx_24") private Long uchiIdx24;
 @Column(name = "uchi_idx_25") private Long uchiIdx25;
 @Column(name = "uchi_idx_26") private Long uchiIdx26;
 @Column(name = "uchi_idx_27") private Long uchiIdx27;
 @Column(name = "uchi_idx_28") private Long uchiIdx28;
 @Column(name = "uchi_idx_29") private Long uchiIdx29;
 @Column(name = "uchi_idx_30") private Long uchiIdx30;
 @Column(name = "uchi_idx_31") private Long uchiIdx31;
    
    
    // 共通項目 (Audit Fields)
    @Column(name = "add_dt") private java.time.LocalDateTime addDt;
    @Column(name = "add_user") private String addUser;
    @Column(name = "upd_dt") private java.time.LocalDateTime updDt;
    @Column(name = "upd_user") private String updUser;
    @Version private Integer version;
}
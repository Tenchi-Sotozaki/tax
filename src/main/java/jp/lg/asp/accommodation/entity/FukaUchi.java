package jp.lg.asp.accommodation.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "t_fuka_uchi")
@IdClass(FukaUchiId.class)
@Data
public class FukaUchi {

    @Id
    private String jichitaiCd;
    @Id
    private String shiteiNo;
    @Id
    private Integer rno;
    // ⭕ DBの char(4) に合わせて String に修正
    @Id
    private String nendo;
    // ⭕ DBの numeric(2) に合わせて Integer に修正
    @Id
    private Integer kibetsu;

    @Id
    private Integer kazeiKbn;

    private Integer zeiritsuSeq;
    
    private BigDecimal zeiRitsu;

    private Integer hakusu;
    
    private Long zeigaku;

    // ⭕ LocalDateTime に統一（Timestamp から変更）
    private LocalDateTime addDt;
    
    private String addUser;
    
    private LocalDateTime updDt;
    
    private String updUser;
    
    private Integer version;
    
    private String fukaKbn;
    
    private Long cityZeigaku; // 市町村分税額
    
    private Long kenZeigaku;  // 都道府県分税額
}
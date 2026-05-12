package jp.lg.asp.accommodation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.Data;

@Entity
@Table(name = "t_choshu_genbo_uchi")
@Data
public class ChoshuGenboUchi {

    @Id
    @Column(name = "uchi_idx")
    private Long uchiIdx;

    @Column(name = "hakusu1") private Integer hakusu1; // 税区分① 宿泊数
    @Column(name = "hakusu2") private Integer hakusu2; // 税区分② 宿泊数
    @Column(name = "hakusu3") private Integer hakusu3; // 税区分③ 宿泊数
    @Column(name = "menjo_hakusu") private Integer menjoHakusu; // 免除宿泊数

    // 共通項目
    @Column(name = "add_dt") private java.time.LocalDateTime addDt;
    @Column(name = "add_user") private String addUser;
    @Column(name = "upd_dt") private java.time.LocalDateTime updDt;
    @Column(name = "upd_user") private String updUser;
    @Version private Integer version;
}
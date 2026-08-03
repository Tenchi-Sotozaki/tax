package jp.lg.asp.accommodation.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "t_koin_torikomi")
@IdClass(KoinTorikomiId.class)
@Data
public class KoinTorikomi {

    /**
     * 自治体コード
     */
    @Id
    @Column(name = "jichitai_cd", length = 5, nullable = false)
    private String jichitaiCd;

    /**
     * 管理番号
     */
    @Id
    @Column(name = "seq", nullable = false)
    private Integer seq;

    /**
     * ファイル名
     */
    @Column(name = "file_name", nullable = false, columnDefinition = "text")
    private String fileName;

    /**
     * 取込日時
     */
    @Column(name = "torikomi_dt", nullable = false)
    private LocalDateTime torikomiDt;

    /**
     * 取込者
     */
    @Column(name = "torikomi_user", nullable = false, columnDefinition = "text")
    private String torikomiUser;

    /**
     * 作成日時
     */
    @Column(name = "add_dt", nullable = false)
    private LocalDateTime addDt;

    /**
     * 作成者
     */
    @Column(name = "add_user", nullable = false, columnDefinition = "text")
    private String addUser;

    /**
     * 更新日時
     */
    @Column(name = "upd_dt", nullable = false)
    private LocalDateTime updDt;

    /**
     * 更新者
     */
    @Column(name = "upd_user", nullable = false, columnDefinition = "text")
    private String updUser;

    /**
     * バージョン
     */
    @Column(name = "version", nullable = false)
    private Integer version;

}
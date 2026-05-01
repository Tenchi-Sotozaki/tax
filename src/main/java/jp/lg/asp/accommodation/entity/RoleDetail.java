package jp.lg.asp.accommodation.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "m_role_dtl")
@Data
@IdClass(RoleDetailId.class)
public class RoleDetail {

    @Id
    @Column(name = "jichitai_cd", nullable = false)
    private String jichitaiCd;

    @Id
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Id
    @Column(name = "screen_id", nullable = false)
    private String screenId;

    @Column(name = "permission", nullable = false)
    private Integer permission; // 1:参照, 2:更新

    @Column(name = "add_user")
    private String addUser;

    @Column(name = "upd_dt")
    private LocalDateTime updDt;

    @Column(name = "upd_user")
    private String updUser;

    @Version
    @Column(name = "version")
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "jichitai_cd", referencedColumnName = "jichitai_cd", insertable = false, updatable = false),
        @JoinColumn(name = "role_id", referencedColumnName = "role_id", insertable = false, updatable = false)
    })
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "jichitai_cd", referencedColumnName = "jichitai_cd", insertable = false, updatable = false),
        @JoinColumn(name = "screen_id", referencedColumnName = "screen_id", insertable = false, updatable = false)
    })
    private Screen screen;
}

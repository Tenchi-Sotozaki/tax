package jp.lg.asp.accommodation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "m_role_dtl")
@Getter 
@Setter
@IdClass(RoleDetailId.class)
public class RoleDetail extends BaseEntity {

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
    private String permission; // 1:参照, 2:更新

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

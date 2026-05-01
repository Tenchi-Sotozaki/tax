package jp.lg.asp.accommodation.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "m_role")
@Data
@IdClass(RoleId.class)
public class Role {

    @Id
    @Column(name = "jichitai_cd", nullable = false)
    private String jichitaiCd;

    @Id
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "add_user")
    private String addUser;

    @Column(name = "upd_dt")
    private LocalDateTime updDt;

    @Column(name = "upd_user")
    private String updUser;

    @Version
    @Column(name = "version")
    private Long version;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RoleDetail> roleDetails;
}

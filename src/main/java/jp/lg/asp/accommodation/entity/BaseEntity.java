package jp.lg.asp.accommodation.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@EntityListeners(AuditEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    @Column(name = "add_dt", nullable = false)
    private LocalDateTime addDt;

    @Column(name = "add_user", nullable = false, length = 20)
    private String addUser;

    @Column(name = "upd_dt", nullable = false)
    private LocalDateTime updDt;

    @Column(name = "upd_user", nullable = false, length = 20)
    private String updUser;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
}

package jp.lg.asp.accommodation.entity;

import java.time.LocalDateTime;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuditEntityListener {

    @PrePersist
    public void prePersist(BaseEntity entity) {
        LocalDateTime now = LocalDateTime.now();
        String user = getCurrentUser();
        entity.setAddDt(now);
        entity.setAddUser(user);
        entity.setUpdDt(now);
        entity.setUpdUser(user);
        entity.setVersion(1);
    }

    @PreUpdate
    public void preUpdate(BaseEntity entity) {
        entity.setUpdDt(LocalDateTime.now());
        entity.setUpdUser(getCurrentUser());
        entity.setVersion(entity.getVersion() + 1);
    }

    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "system";
    }
}

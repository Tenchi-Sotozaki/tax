package jp.lg.asp.accommodation.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "m_screen")
@Data
@IdClass(ScreenId.class)
public class Screen {
    
    @Id
    @Column(name = "jichitai_cd")
    private String jichitaiCd;
    
    @Id
    @Column(name = "screen_id")
    private String screenId;
    
    @Column(name = "screen_name", nullable = false)
    private String screenName;
    
    @Column(name = "add_user")
    private String addUser;
    
    @Column(name = "upd_dt")
    private java.time.LocalDateTime updDt;
    
    @Column(name = "upd_user")
    private String updUser;
    
    @Column(name = "version")
    private Long version;
}
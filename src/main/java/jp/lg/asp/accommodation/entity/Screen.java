package jp.lg.asp.accommodation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "m_screen")
@Getter 
@Setter
@IdClass(ScreenId.class)
public class Screen extends BaseEntity {
    
    @Id
    @Column(name = "jichitai_cd")
    private String jichitaiCd;
    
    @Id
    @Column(name = "screen_id")
    private String screenId;
    
    @Column(name = "screen_name", nullable = false)
    private String screenName;

    /** 区分 */
    @Column(name = "kbn", nullable = false)
    private String kbn;

    /** 表示順 */
    @Column(name = "dsp_odr", nullable = false)
    private Integer dspOdr;

}
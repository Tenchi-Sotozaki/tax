package jp.lg.asp.accommodation.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "m_top_page")
@IdClass(TopPageContentId.class)
@Getter
@Setter
public class TopPageContent extends BaseEntity {

    @Id
    @Column(name = "jichitai_cd", length = 5)
    private String jichitaiCd;
       
    @Id
    @Column(name = "seq")
    private Integer seq;
    
    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @Column(name = "contents", columnDefinition = "TEXT")
    private String htmlContent;
    
    @Column(name = "up_st_ymd")
    private LocalDate postingStartDate;
    
    @Column(name = "up_ed_ymd")
    private LocalDate postingEndDate;    
    
    /**
    * 表示用HTML（DB保存対象外）
    */
    @Transient
    private String titleHtml;

    /**
    * 表示用HTML（DB保存対象外）
    */
    @Transient
    private String contentHtml;
    
}

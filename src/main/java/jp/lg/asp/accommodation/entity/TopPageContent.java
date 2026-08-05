package jp.lg.asp.accommodation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "m_top_page_content")
@IdClass(TopPageContentId.class)
@Getter
@Setter
public class TopPageContent extends BaseEntity {

    /** 区分: "0"=全自治体共有, "1"=自治体カスタマイズ */
    @Id
    @Column(name = "kbn", length = 1)
    private String kbn;

    @Id
    @Column(name = "jichitai_cd", length = 5)
    private String jichitaiCd;

    @Column(name = "html_content", columnDefinition = "TEXT")
    private String htmlContent;
}

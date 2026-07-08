package jp.lg.asp.accommodation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "m_nokigen")
@IdClass(NokigenId.class)
@Getter
@Setter
public class Nokigen extends BaseEntity {

    @Id
    @Column(name = "jichitai_cd", length = 5)
    private String jichitaiCd;

    @Id
    @Column(name = "nendo", length = 4)
    private String nendo;

    @Column(name = "nokigen_1st", length = 8, nullable = false)
    private String nokigen1st;

    @Column(name = "nokigen_2nd", length = 8, nullable = false)
    private String nokigen2nd;

    @Column(name = "nokigen_3rd", length = 8, nullable = false)
    private String nokigen3rd;

    @Column(name = "nokigen_4th", length = 8, nullable = false)
    private String nokigen4th;

    @Column(name = "nokigen_5th", length = 8, nullable = false)
    private String nokigen5th;

    @Column(name = "nokigen_6th", length = 8, nullable = false)
    private String nokigen6th;

    @Column(name = "nokigen_7th", length = 8, nullable = false)
    private String nokigen7th;

    @Column(name = "nokigen_8th", length = 8, nullable = false)
    private String nokigen8th;

    @Column(name = "nokigen_9th", length = 8, nullable = false)
    private String nokigen9th;

    @Column(name = "nokigen_10th", length = 8, nullable = false)
    private String nokigen10th;

    @Column(name = "nokigen_11th", length = 8, nullable = false)
    private String nokigen11th;

    @Column(name = "nokigen_12th", length = 8, nullable = false)
    private String nokigen12th;
}

package jp.lg.asp.accommodation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "m_holiday")
@IdClass(HolidayId.class)
@Getter
@Setter
public class Holiday extends BaseEntity {

    @Id
    @Column(name = "jichitai_cd", length = 5)
    private String jichitaiCd;

    @Id
    @Column(name = "nendo", length = 4)
    private String nendo;

    @Id
    @Column(name = "holiday_dt", length = 8)
    private String holidayDt;
}

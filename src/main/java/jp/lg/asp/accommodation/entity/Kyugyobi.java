package jp.lg.asp.accommodation.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "m_kyugyobi")
@IdClass(KyugyobiId.class)
@Getter
@Setter
public class Kyugyobi extends BaseEntity {

	@Id
	@Column(name = "jichitai_cd", length = 5)
	private String jichitaiCd;

	@Id
	@Column(name = "nen", length = 4)
	private String nen;

	@Id
	@Column(name = "kyugyobi")
	private LocalDate kyugyobi;
}

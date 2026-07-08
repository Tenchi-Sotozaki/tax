package jp.lg.asp.accommodation.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "t_shoreikin")
@IdClass(ShoreikinId.class)
@Getter
@Setter
public class Shoreikin extends BaseEntity {

	@Id
	@Column(name = "jichitai_cd", length = 5)
	private String jichitaiCd;

	@Id
	@Column(name = "shitei_no", length = 8)
	private String shiteiNo;

	@Id
	@Column(name = "nendo", length = 4)
	private String nendo;

	@Column(name = "kofu_zeigaku", nullable = false, precision = 14)
	private Long kofuZeigaku;

	@Column(name = "kofu_ritsu", nullable = false, precision = 5, scale = 2)
	private BigDecimal kofuRitsu;

	@Column(name = "kofu_gaku", nullable = false, precision = 13)
	private Long kofuGaku;

	@Column(name = "kofu_ymd")
	private LocalDate kofuYmd;
}

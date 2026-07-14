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
@Table(name = "t_shuno_rireki")
@IdClass(ShunoRirekiId.class)
@Getter
@Setter
public class ShunoRireki extends BaseEntity {

	@Id
	@Column(name = "jichitai_cd", length = 5)
	private String jichitaiCd;

	@Id
	@Column(name = "shitei_no", length = 8)
	private String shiteiNo;

	@Id
	@Column(name = "nendo", length = 4)
	private String nendo;

	@Id
	@Column(name = "kibetsu")
	private Integer kibetsu;

	@Id
	@Column(name = "rno")
	private Integer rno;

	@Column(name = "nonyugaku")
	private Integer nonyugaku;

	@Column(name = "nonyu_ymd")
	private LocalDate nonyuYmd;

}
package jp.lg.asp.accommodation.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "t_fuka_uchi")
@IdClass(FukaUchiId.class)
@Getter
@Setter
public class FukaUchi extends BaseEntity {

	@Id
	@Column(name = "jichitai_cd", length = 5)
	private String jichitaiCd;

	@Id
	@Column(name = "shitei_no", length = 8)
	private String shiteiNo;

	@Id
	@Column(name = "rno")
	private Integer rno;

	@Id
	@Column(name = "nendo", length = 4)
	private String nendo;

	@Id
	@Column(name = "kibetsu")
	private Integer kibetsu;

	@Id
	@Column(name = "kazei_kbn")
	private Integer kazeiKbn;

	@Column(name = "zeiritsu_seq", precision = 8)
	private BigDecimal zeiritsuSeq;

	@Column(name = "fuka_kbn", length = 1)
	private String fukaKbn;

	@Column(name = "ryokin_sogaku")
	private Long ryokinSogaku;

	@Column(name = "hakusu")
	private Long hakusu;

	@Column(name = "ryokin")
	private Long ryokin;

	@Column(name = "zei_ritsu", precision = 12, scale = 2)
	private BigDecimal zeiRitsu;

	@Column(name = "zeigaku")
	private Long zeigaku;

	@Column(name = "city_zeigaku")
	private Long cityZeigaku;

	@Column(name = "ken_zeigaku")
	private Long kenZeigaku;
}

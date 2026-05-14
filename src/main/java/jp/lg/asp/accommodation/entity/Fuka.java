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
@Table(name = "t_fuka")
@IdClass(FukaId.class)
@Getter
@Setter
public class Fuka extends BaseEntity {

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

	@Column(name = "toroku_ymd")
	private LocalDate torokuYmd;

	@Column(name = "shinkoku_ymd")
	private LocalDate shinkokuYmd;

	@Column(name = "taisho_ym", length = 6)
	private String taishoYm;

	@Column(name = "fuka_kbn", length = 1)
	private String fukaKbn;

	@Column(name = "henko_kbn", length = 1)
	private String henkoKbn;

	@Column(name = "henko_riyu")
	private String henkoRiyu;

	@Column(name = "kazei_hakusu")
	private Long kazeiHakusu;

	@Column(name = "kazei_ryokin")
	private Long kazeiRyokin;

	@Column(name = "zeigaku")
	private Long zeigaku;

	@Column(name = "menjo_hakusu")
	private Long menjoHakusu;

	@Column(name = "menjo_ryokin")
	private Long menjoRyokin;

	@Column(name = "total_hakusu")
	private Long totalHakusu;

	@Column(name = "total_zeigaku")
	private Long totalZeigaku;

	@Column(name = "city_zeigaku")
	private Long cityZeigaku;

	@Column(name = "ken_zeigaku")
	private Long kenZeigaku;

	@Column(name = "kasan_kbn", length = 1)
	private String kasanKbn;

	@Column(name = "kasan_ritsu", precision = 5, scale = 2)
	private BigDecimal kasanRitsu;

	@Column(name = "kasan_gaku")
	private Long kasanGaku;

	@Column(name = "nokigen")
	private LocalDate nokigen;

	@Column(name = "new_flg", length = 1)
	private String newFlg;

	@Column(name = "del_flg", length = 1)
	private String delFlg;
}
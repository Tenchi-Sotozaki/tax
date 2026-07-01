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
@Table(name = "t_kyodo_jigyosha")
@IdClass(KyodoJigyoshaId.class)
@Getter
@Setter
public class KyodoJigyosha extends BaseEntity {

	@Id
	@Column(name = "jichitai_cd", length = 5)
	private String jichitaiCd;

	@Id
	@Column(name = "shitei_no", length = 8)
	private String shiteiNo;

	@Id
	@Column(name = "rno", precision = 3)
	private BigDecimal rno;

	@Id
	@Column(name = "idx", precision = 3)
	private BigDecimal idx;

	@Column(name = "kyodo_jigyosha_name", nullable = false)
	private String kyodoJigyoshaName;

	@Column(name = "kyodo_jigyosha_name_kana", nullable = false)
	private String kyodoJigyoshaNameKana;

	@Column(name = "kyodo_jigyosha_yubin_no", length = 10)
	private String kyodoJigyoshaYubinNo;

	@Column(name = "kyodo_jigyosha_jusho")
	private String kyodoJigyoshaJusho;

	@Column(name = "kyodo_jigyosha_tel", length = 20)
	private String kyodoJigyoshaTel;

}
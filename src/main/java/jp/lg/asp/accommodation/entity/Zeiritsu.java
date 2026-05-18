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
@Table(name = "m_zeiritsu")
@IdClass(ZeiritsuId.class)
@Getter
@Setter
public class Zeiritsu extends BaseEntity {

	@Id
	@Column(name = "jichitai_cd", length = 5)
	private String jichitaiCd;

	@Id
	@Column(name = "seq", precision = 5)
	private BigDecimal seq;

	@Column(name = "taisho_kbn", nullable = false, length = 1)
	private String taishoKbn;

	@Column(name = "tekiyo_st_ym", nullable = false, length = 6)
	private String tekiyoStYm;

	@Column(name = "tekiyo_ed_ym", nullable = false, length = 6)
	private String tekiyoEdYm;

	@Column(name = "fuka_kbn", nullable = false, length = 1)
	private String fukaKbn;

	@Column(name = "del_flg", nullable = false, length = 1)
	private String delFlg;
}

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
@Table(name = "m_zeiritsu_teiritsu")
@IdClass(ZeiritsuTeiritsuId.class)
@Getter
@Setter
public class ZeiritsuTeiritsu extends BaseEntity {

	@Id
	@Column(name = "jichitai_cd", length = 5)
	private String jichitaiCd;

	@Id
	@Column(name = "seq", precision = 5)
	private BigDecimal seq;

	@Id
	@Column(name = "teiritsu_seq", precision = 8)
	private BigDecimal teiritsuSeq;

	@Column(name = "kbn_name", nullable = false, length = 20)
	private String kbnName;

	@Column(name = "zei_ritsu", nullable = false, precision = 3, scale = 2)
	private BigDecimal zeiRitsu;

	@Column(name = "del_flg", nullable = false, length = 1)
	private String delFlg;
}

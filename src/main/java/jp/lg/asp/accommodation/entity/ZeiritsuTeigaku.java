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
@Table(name = "m_zeiritsu_teigaku")
@IdClass(ZeiritsuTeigakuId.class)
@Getter
@Setter
public class ZeiritsuTeigaku extends BaseEntity {

	@Id
	@Column(name = "jichitai_cd", length = 5)
	private String jichitaiCd;

	@Id
	@Column(name = "seq", precision = 5)
	private BigDecimal seq;

	@Id
	@Column(name = "teigaku_seq", precision = 8)
	private BigDecimal teigakuSeq;

	@Column(name = "ryokin_st", nullable = false, precision = 13)
	private Long ryokinSt;

	@Column(name = "ryokin_ed", precision = 13)
	private Long ryokinEd;

	@Column(name = "zeigaku", nullable = false, precision = 13)
	private Long zeigaku;

	@Column(name = "del_flg", nullable = false, length = 1)
	private String delFlg;
}

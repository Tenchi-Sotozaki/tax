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
@Table(name = "m_nozei_shuki")
@IdClass(NozeiShukiId.class)
@Getter
@Setter

public class NozeiShuki extends BaseEntity {
	@Id
	@Column(name = "jichitai_cd", length = 5)
	private String jichitaiCd;

	@Column(name = "seq", precision = 3)
	private BigDecimal seq;

	@Column(name = "shuki", precision = 2, nullable = false)
	private BigDecimal shuki;

	@Column(name = "del_flg", length = 1, nullable = false)
	private String delFlg;



}

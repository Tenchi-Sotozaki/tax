package jp.lg.asp.accommodation.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "t_eltax_renkei")
@IdClass(EltaxRenkeiId.class)
@Getter
@Setter
public class EltaxRenkei extends BaseEntity {

	@Id
	@Column(name = "jichitai_cd", length = 5)
	private String jichitaiCd;

	@Id
	@Column(name = "seq", precision = 8)
	private BigDecimal seq;

	@Column(name = "file_name", length = 256, nullable = false)
	private String fileName;

	@Column(name = "shubetsu", length = 2)
	private String shubetsu;

	@Column(name = "shori_dt")
	private LocalDateTime shoriDt;

	@Column(name = "shori_kekka", length = 1)
	private String shoriKekka;

	@Column(name = "log")
	private byte[] log;
}

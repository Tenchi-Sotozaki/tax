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
@Table(name = "m_nozei_shuki")
@IdClass(NozeiShukiId.class)
@Getter
@Setter

public class NozeiShuki {
	@Id
	@Column(name = "jichitai_cd", length = 5)
	private String jichitaiCd;

	@Id
	@Column(name = "seq", precision = 3)
	private BigDecimal seq;

	@Column(name = "shuki", precision = 2, nullable = false)
	private BigDecimal shuki;

	@Column(name = "del_flg", length = 1, nullable = false)
	private String delFlg;

	@Column(name = "add_dt", nullable = false)
	private LocalDateTime addDt;

	@Column(name = "add_user", length = 20, nullable = false)
	private String addUser;

	@Column(name = "upd_dt", nullable = false)
	private LocalDateTime updDt;

	@Column(name = "upd_user", length = 20, nullable = false)
	private String updUser;

	@Column(name = "version", precision = 5, nullable = false)
	private BigDecimal version;

}

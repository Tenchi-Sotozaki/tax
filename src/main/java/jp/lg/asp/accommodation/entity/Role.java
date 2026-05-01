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
@Table(name = "m_role")
@IdClass(RoleId.class)
@Getter
@Setter
public class Role {

	@Id
	@Column(name = "jichitai_cd", length = 5)
	private String jichitaiCd;

	@Id
	@Column(name = "role_id", nullable = false, precision = 5)
	private BigDecimal roleId;

	@Column(name = "name", length = 200, nullable = false)
	private String name;

	@Column(name = "busho", length = 200, nullable = false)
	private String busho;

	@Column(name = "upd_dt", nullable = false)
	private LocalDateTime updDt;

	@Column(name = "upd_user", length = 20, nullable = false)
	private String updUser;

	@Column(name = "version", nullable = false, precision = 5)
	private BigDecimal version;
}

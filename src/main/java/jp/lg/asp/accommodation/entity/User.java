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
@Table(name = "m_user")
@IdClass(UserId.class)
@Getter
@Setter
public class User extends BaseEntity {

	@Id
	@Column(name = "jichitai_cd", length = 5)
	private String jichitaiCd;

	@Id
	@Column(name = "id", length = 100)
	private String id;

	@Column(name = "password", length = 64, nullable = false)
	private String password;

	@Column(name = "name", length = 200, nullable = false)
	private String name;

	@Column(name = "name_kana", length = 200, nullable = false)
	private String nameKana;

	@Column(name = "busho", length = 200, nullable = false)
	private String busho;

	@Column(name = "role_id", nullable = false, precision = 5)
	private BigDecimal roleId;

	@Column(name = "del_flg",  length = 1,nullable = false)
	private String delFlg = "0";

	@Column(name = "initial_password_flg", length = 1, nullable = false)
	private String initialPasswordFlg = "0";
}

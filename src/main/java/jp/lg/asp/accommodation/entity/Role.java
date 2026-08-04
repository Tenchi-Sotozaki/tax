package jp.lg.asp.accommodation.entity;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "m_role")
@Getter 
@Setter
@IdClass(RoleId.class)
public class Role extends BaseEntity {

	@Id
	@Column(name = "jichitai_cd", nullable = false)
	private String jichitaiCd;

	@Id
	@Column(name = "role_id", nullable = false)
	private Long roleId;

	@Column(name = "name", nullable = false)
	private String name;

	@OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private List<RoleDetail> roleDetails;
}

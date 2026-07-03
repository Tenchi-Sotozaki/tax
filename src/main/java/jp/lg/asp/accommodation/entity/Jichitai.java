package jp.lg.asp.accommodation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "m_jichitai")
@Getter
@Setter
public class Jichitai extends BaseEntity {

	@Id
	@Column(name = "jichitai_cd", length = 5)
	private String jichitaiCd;

	@Column(name = "name", nullable = false, length = 20)
	private String name;

	@Column(name = "kbn_name", nullable = false, length = 10)
	private String kbnName;

	@Column(name = "start_month")
	private Integer startMonth;

}
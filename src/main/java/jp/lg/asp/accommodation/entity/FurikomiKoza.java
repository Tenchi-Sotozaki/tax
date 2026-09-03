package jp.lg.asp.accommodation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "t_furikomi_koza")
@IdClass(FurikomiKozaId.class)
@Data
@EqualsAndHashCode(callSuper = false)
public class FurikomiKoza extends BaseEntity {

	@Id
	@Column(name = "jichitai_cd", length = 5, nullable = false)
	private String jichitaiCd;

	@Id
	@Column(name = "shitei_no", length = 8, nullable = false)
	private String shiteiNo;

	@Column(name = "bank_cd", length = 4, nullable = false)
	private String bankCd;

	@Column(name = "bank_name", length = 30, nullable = false)
	private String bankName;

	@Column(name = "branch_cd", length = 3, nullable = false)
	private String branchCd;

	@Column(name = "branch_name", length = 30, nullable = false)
	private String branchName;

	@Column(name = "shumoku", length = 1, nullable = false)
	private String shumoku;

	@Column(name = "koza_no", length = 8, nullable = false)
	private String kozaNo;

	@Column(name = "meigi", length = 30, nullable = false)
	private String meigi;
}
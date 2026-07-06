package jp.lg.asp.accommodation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * 帳票管理マスタ
 */
@Entity
@Table(name = "m_reports")
@IdClass(ReportsId.class)
@Getter
@Setter
public class Reports extends BaseEntity {

	/** 自治体コード */
	@Id
	@Column(name = "jichitai_cd", length = 5)
	private String jichitaiCd;

	/** 帳票ＩＤ */
	@Id
	@Column(name = "rpt_id", length = 10)
	private String rptId;

	/** 帳票名称 */
	@Column(name = "rpt_name", nullable = false, length = 100)
	private String rptName;
}

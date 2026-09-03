package jp.lg.asp.accommodation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * 帳票管理マスタ
 */
@Entity
@Table(name = "m_reports")
@Getter
@Setter
public class Reports extends BaseEntity {

	/** 帳票ＩＤ */
	@Id
	@Column(name = "rpt_id", length = 10)
	private String rptId;

	/** 帳票名称 */
	@Column(name = "rpt_name", nullable = false)
	private String rptName;
}

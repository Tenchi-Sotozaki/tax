package jp.lg.asp.accommodation.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * 帳票発行状況
 */
@Entity
@Table(name = "t_rpt_status")
@IdClass(RptStatusId.class)
@Getter
@Setter
public class RptStatus extends BaseEntity {

	/** 自治体コード */
	@Id
	@Column(name = "jichitai_cd", length = 5)
	private String jichitaiCd;

	/** 指定番号 */
	@Id
	@Column(name = "shitei_no", length = 8)
	private String shiteiNo;

	/** 年度 */
	@Id
	@Column(name = "nendo", length = 4)
	private String nendo;

	/** 帳票ＩＤ */
	@Id
	@Column(name = "rpt_id", length = 10)
	private String rptId;

	/** 帳票作成日時 */
	@Column(name = "create_dt", nullable = false)
	private LocalDateTime createDt;
}

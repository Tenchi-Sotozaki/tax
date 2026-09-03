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
 * 帳票ログ
 */
@Entity
@Table(name = "t_reports_log")
@IdClass(ReportsLogId.class)
@Getter
@Setter
public class ReportsLog extends BaseEntity {

	/** 自治体コード */
	@Id
	@Column(name = "jichitai_cd", length = 5)
	private String jichitaiCd;

	/** 管理番号 */
	@Id
	@Column(name = "seq", nullable = false)
	private Long seq;

	/** 帳票ＩＤ */
	@Column(name = "rpt_id", nullable = false, columnDefinition = "char(10)")
	private String rptId;

	/** 操作 */
	@Column(name = "sousa", nullable = false, length = 1)
	private String sousa;

	/** 指定番号 */
	@Column(name = "shitei_no", length = 8)
	private String shiteiNo;

	/** 操作者 */
	@Column(name = "ope_user", nullable = false, length = 20)
	private String opeUser;

	/** 操作日時 */
	@Column(name = "ope_dt", nullable = false)
	private LocalDateTime opeDt;
}

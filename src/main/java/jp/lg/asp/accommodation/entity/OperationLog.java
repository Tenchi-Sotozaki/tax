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
 * 操作ログ
 */
@Entity
@Table(name = "t_operation_log")
@IdClass(OperationLogId.class)
@Getter
@Setter
public class OperationLog extends BaseEntity {

	/** 自治体コード */
	@Id
	@Column(name = "jichitai_cd", length = 5)
	private String jichitaiCd;

	/** 管理番号 */
	@Id
	@Column(name = "seq", nullable = false)
	private Long seq;

	/** 画面ＩＤ */
	@Column(name = "screen_id", nullable = false, length = 10)
	private String screenId;

	/** 操作 */
	@Column(name = "sousa", nullable = false)
	private String sousa;

	/** メソッド */
	@Column(name = "method")
	private String method;

	/** PATH */
	@Column(name = "path")
	private String path;

	/** ステータス */
	@Column(name = "status")
	private String status;

	/** リクエストパラメータ */
	@Column(name = "param")
	private String param;

	/** 操作者 */
	@Column(name = "ope_user", nullable = false)
	private String opeUser;

	/** 操作日時 */
	@Column(name = "ope_dt", nullable = false)
	private LocalDateTime opeDt;
}

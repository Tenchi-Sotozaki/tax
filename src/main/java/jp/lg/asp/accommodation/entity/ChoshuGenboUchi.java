package jp.lg.asp.accommodation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.Data;

@Entity
@Table(name = "t_choshu_genbo_uchi")
@Data
@IdClass(ChoshuGenboUchiId.class)
public class ChoshuGenboUchi {

	@Id
	@Column(name = "uchi_idx")
	private Long uchiIdx;

	@Id
	@Column(name = "jichitai_cd")
	private String jichitaiCd;

	@Column(name = "hakusu1")
	private Integer hakusu1; // 税区分① 宿泊数
	@Column(name = "hakusu2")
	private Integer hakusu2; // 税区分② 宿泊数
	@Column(name = "hakusu3")
	private Integer hakusu3; // 税区分③ 宿泊数
	@Column(name = "ryokin1")
	private Long ryokin1; // 税区分① 宿泊料金（定率制用）
	@Column(name = "ryokin2")
	private Long ryokin2; // 税区分② 宿泊料金（定率制用）
	@Column(name = "ryokin3")
	private Long ryokin3; // 税区分③ 宿泊料金（定率制用）
	@jakarta.persistence.Transient
	private Long menjoRyokin; // 免除宿泊料金（定率制用・DBカラム未存在のためマッピング除外）
	@Column(name = "menjo_hakusu")
	private Integer menjoHakusu; // 免除宿泊数

	// 共通項目
	@Column(name = "add_dt")
	private java.time.LocalDateTime addDt;
	@Column(name = "add_user")
	private String addUser;
	@Column(name = "upd_dt")
	private java.time.LocalDateTime updDt;
	@Column(name = "upd_user")
	private String updUser;
	@Version
	private Integer version;
}
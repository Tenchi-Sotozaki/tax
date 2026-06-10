package jp.lg.asp.accommodation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "t_choshu_genbo_uchi")
@Data
@IdClass(ChoshuGenboUchiId.class)
public class ChoshuGenboUchi extends BaseEntity {

	@Id
	@Column(name = "jichitai_cd", length = 5)
	private String jichitaiCd;

	@Id
	@Column(name = "uchi_idx")
	private Long uchiIdx;

	@Column(name = "sogaku1")
	private Long sogaku1;
	@Column(name = "hakusu1")
	private Integer hakusu1;
	@Column(name = "ryokin1")
	private Long ryokin1;

	@Column(name = "sogaku2")
	private Long sogaku2;
	@Column(name = "hakusu2")
	private Integer hakusu2;
	@Column(name = "ryokin2")
	private Long ryokin2;

	@Column(name = "sogaku3")
	private Long sogaku3;
	@Column(name = "hakusu3")
	private Integer hakusu3;
	@Column(name = "ryokin3")
	private Long ryokin3;

	@Column(name = "sogaku4")
	private Long sogaku4;
	@Column(name = "hakusu4")
	private Integer hakusu4;
	@Column(name = "ryokin4")
	private Long ryokin4;

	@Column(name = "sogaku5")
	private Long sogaku5;
	@Column(name = "hakusu5")
	private Integer hakusu5;
	@Column(name = "ryokin5")
	private Long ryokin5;

	@Column(name = "sogaku6")
	private Long sogaku6;
	@Column(name = "hakusu6")
	private Integer hakusu6;
	@Column(name = "ryokin6")
	private Long ryokin6;

	@Column(name = "sogaku7")
	private Long sogaku7;
	@Column(name = "hakusu7")
	private Integer hakusu7;
	@Column(name = "ryokin7")
	private Long ryokin7;

	@Column(name = "sogaku8")
	private Long sogaku8;
	@Column(name = "hakusu8")
	private Integer hakusu8;
	@Column(name = "ryokin8")
	private Long ryokin8;

	@Column(name = "sogaku9")
	private Long sogaku9;
	@Column(name = "hakusu9")
	private Integer hakusu9;
	@Column(name = "ryokin9")
	private Long ryokin9;

	@Column(name = "sogaku10")
	private Long sogaku10;
	@Column(name = "hakusu10")
	private Integer hakusu10;
	@Column(name = "ryokin10")
	private Long ryokin10;

	@Column(name = "menjo_hakusu")
	private Integer menjoHakusu;

	@Column(name = "zeigaku")
	private Long zeigaku;

}

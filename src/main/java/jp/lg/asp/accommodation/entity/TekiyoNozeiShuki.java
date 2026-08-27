package jp.lg.asp.accommodation.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "t_nozei_shuki")
@IdClass(TekiyoNozeiShukiId.class)
@Getter
@Setter
public class TekiyoNozeiShuki extends BaseEntity {

	@Id
	@Column(name = "jichitai_cd", length = 5, nullable = false)
	private String jichitaiCd;

	@Id
	@Column(name = "shitei_no", length = 8, nullable = false)
	private String shiteiNo;

	@Id
	@Column(name = "rno", nullable = false)
	private Integer rno;

	@Column(name = "tekiyo_st_ymd")
	private LocalDate tekiyoStYmd;

	@Column(name = "tekiyo_ed_ymd")
	private LocalDate tekiyoEdYmd;

	@Column(name = "del_flg", length = 1, nullable = false)
	private String delFlg;
}

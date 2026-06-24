package jp.lg.asp.accommodation.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "m_kofu_ritsu")
@IdClass(KofuRitsuId.class)
@Getter
@Setter
public class KofuRitsu extends BaseEntity {

	@Id
	@Column(name = "jichitai_cd", length = 5)
	private String jichitaiCd;

	@Id
	@Column(name = "rno", precision = 10)
	private BigDecimal rno;

	@Column(name = "kofu_ritsu", precision = 5, scale = 2)
	private BigDecimal kofuRitsu;

	@Column(name = "tekiyo_st_ymd")
	private LocalDate tekiyoStYmd;

	@Column(name = "tekiyo_ed_ymd")
	private LocalDate tekiyoEdYmd;

	@Column(name = "new_flg", precision = 1)
	private Integer newFlg;
}

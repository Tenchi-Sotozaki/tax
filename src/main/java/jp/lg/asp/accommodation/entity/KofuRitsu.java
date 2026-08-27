package jp.lg.asp.accommodation.entity;

import java.math.BigDecimal;

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

	@Column(name = "sanshutsu")
	private Integer sanshutsu;

	@Column(name = "kbn")
	private String kbn;

	@Column(name = "saiteigaku", precision = 15, scale = 2)
	private BigDecimal saiteigaku;

	@Column(name = "tekiyo_st_nendo")
	private Integer tekiyoStNendo;

	@Column(name = "new_flg", length = 1)
	private String newFlg;
}

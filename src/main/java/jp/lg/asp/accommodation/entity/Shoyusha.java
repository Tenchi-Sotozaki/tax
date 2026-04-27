package jp.lg.asp.accommodation.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "t_shoyusha")
@IdClass(ShoyushaId.class)
@Getter
@Setter
public class Shoyusha {

	@Id
	@Column(name = "jichitai_cd", length = 5)
	private String jichitaiCd;

	@Id
	@Column(name = "shitei_no", length = 8)
	private String shiteiNo;

	@Id
	@Column(name = "rno", precision = 3)
	private BigDecimal rno;

	@Id
	@Column(name = "idx", precision = 3)
	private BigDecimal idx;

	@Column(name = "shoyusha_name", nullable = false, length = 200)
	private String shoyushaName;

	@Column(name = "shoyusha_name_kana", nullable = false, length = 200)
	private String shoyushaNameKana;

	@Column(name = "shoyusha_yubin_no", length = 10)
	private String shoyushaYubinNo;

	@Column(name = "shoyusha_jusho", length = 200)
	private String shoyushaJusho;

	@Column(name = "shoyusha_tel", length = 20)
	private String shoyushaTel;

	@Column(name = "add_dt", nullable = false)
	private LocalDateTime addDt;

	@Column(name = "add_user", nullable = false, length = 20)
	private String assUser;

	@Column(name = "upd_dt", nullable = false)
	private LocalDateTime updDt;

	@Column(name = "upd_user", nullable = false, length = 20)
	private String updUser;

	@Column(name = "version", nullable = false)
	private BigDecimal version;
}

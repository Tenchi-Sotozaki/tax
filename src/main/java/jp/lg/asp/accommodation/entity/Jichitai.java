package jp.lg.asp.accommodation.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "m_jichitai")
@Getter
@Setter
public class Jichitai extends BaseEntity {

	@Id
	@Column(name = "jichitai_cd", length = 5)
	private String jichitaiCd;

	@Column(name = "name", nullable = false, length = 20)
	private String name;

	@Column(name = "kbn_name", nullable = false, length = 10)
	private String kbnName;

	@Column(name = "nendo_st_month", length = 2)
	private String nendoStMonth;

	@Column(name = "nozei_shuki", length = 2)
	private String nozeiShuki;

	@Column(name = "shitei_st_char", length = 3)
	private String shiteiStChar;

	@Column(name = "gassan_st_char", length = 3)
	private String gassanStChar;

	@Column(name = "atena_st_no", precision = 15)
	private BigDecimal atenaStNo;

}

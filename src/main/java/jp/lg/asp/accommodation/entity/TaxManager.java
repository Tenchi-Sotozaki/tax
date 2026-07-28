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
@Table(name = "t_nokan")
@Getter
@Setter
@IdClass(TaxManagerId.class)
public class TaxManager extends BaseEntity {

	@Id
	@Column(name = "jichitai_cd", length = 5, nullable = false)
	private String jichitaiCd;

	@Id
	@Column(name = "shitei_no", length = 8, nullable = false)
	private String shiteiNo;

	@Id
	@Column(name = "rno", nullable = false)
	private Integer rno;

	@Column(name = "kbn", length = 1, nullable = false)
	private String kbn;

	@Column(name = "toroku_ymd", nullable = false)
	private LocalDate torokuYmd;

	@Column(name = "shinkoku_ymd", nullable = false)
	private LocalDate shinkokuYmd;

	@Column(name = "atena_no", length = 15)
	private String atenaNo;

	@Column(name = "name", length = 200)
	private String name;

	@Column(name = "name_kana", length = 200)
	private String nameKana;

	@Column(name = "yubin_no", length = 10)
	private String yubinNo;

	@Column(name = "jusho", length = 200)
	private String jusho;

	@Column(name = "tel", length = 20)
	private String tel;

	@Column(name = "riyu", length = 400)
	private String riyu;

	@Column(name = "new_flg", length = 1, nullable = false)
	private String newFlg;

	@Column(name = "del_flg", length = 1, nullable = false)
	private String delFlg;

}
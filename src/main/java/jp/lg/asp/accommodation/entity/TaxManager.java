package jp.lg.asp.accommodation.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "t_nokan")
@Data
@IdClass(TaxManagerId.class)
public class TaxManager {

	@Id
	@Column(name = "jichitai_cd", length = 5, nullable = false)
	private String jichitaiCd;

	@Id
	@Column(name = "shitei_no", length = 8, nullable = false)
	private String shiteiNo;

	@Id
	@Column(name = "rno", nullable = false)
	private Integer rno;

	@Column(name = "menjo_kbn", length = 1, nullable = false)
	private String menjoKbn; // 選任免除区分

	@Column(name = "toroku_ymd", nullable = false)
	private LocalDate torokuYmd; // 登録年月日

	@Column(name = "shinkoku_ymd", nullable = false)
	private LocalDate shinkokuYmd; // 申告年月日

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

	@Column(name = "menjo_riyu", length = 400)
	private String menjoRiyu; // 専任免除理由

	@Column(name = "new_flg", length = 1, nullable = false)
	private String newFlg;

	@Column(name = "del_flg", length = 1, nullable = false)
	private String delFlg;

	// --- 以下、共通カラム（定義書に基づく） ---
	@Column(name = "add_dt", nullable = false)
	private LocalDateTime addDt;

	@Column(name = "add_user", length = 20, nullable = false)
	private String addUser;

	@Column(name = "upd_dt", nullable = false)
	private LocalDateTime updDt;

	@Column(name = "upd_user", length = 20, nullable = false)
	private String updUser;

	@Column(name = "version", nullable = false)
	private Integer version;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumns({
			@JoinColumn(name = "jichitai_cd", referencedColumnName = "jichitai_cd", insertable = false, updatable = false),
			@JoinColumn(name = "atena_no", referencedColumnName = "atena_no", insertable = false, updatable = false)
	})
	private Atena atena;
}
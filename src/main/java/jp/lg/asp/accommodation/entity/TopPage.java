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

/**
 * トップページマスタ
 * <p>
 * トップページに掲載する項目を1件ずつ保持する。
 * 掲載期間を持ち、期間内の項目のみトップページに表示する。
 */
@Entity
@Table(name = "m_top_page")
@IdClass(TopPageId.class)
@Getter
@Setter
public class TopPage extends BaseEntity {

	/**
	 * 全自治体共有のレコードに用いる自治体コード。
	 * 自治体ごとのカスタマイズは画面設計書の書き込みにより対象外のため、現状はこの値のみを使用する。
	 */
	public static final String COMMON_JICHITAI_CD = "00000";

	@Id
	@Column(name = "jichitai_cd", length = 5)
	private String jichitaiCd;

	/** 連番 */
	@Id
	@Column(name = "seq")
	private BigDecimal seq;

	/** 掲載内容（タグ付きテキスト） */
	@Column(name = "contents")
	private String contents;

	/** 掲載開始日 */
	@Column(name = "keisai_st_ymd")
	private LocalDate keisaiStYmd;

	/** 掲載終了日。未設定の場合は終了日なしとして扱う */
	@Column(name = "keisai_ed_ymd")
	private LocalDate keisaiEdYmd;

}

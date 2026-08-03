package jp.lg.asp.accommodation.entity;

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
 * トップページに差し込むタグ付きテキストを保持する。
 */
@Entity
@Table(name = "m_top_page")
@IdClass(TopPageId.class)
@Getter
@Setter
public class TopPage extends BaseEntity {

	/** 表示区分：全自治体共有 */
	public static final String KBN_COMMON = "1";

	/** 表示区分：自治体カスタマイズ */
	public static final String KBN_CUSTOM = "2";

	/** 全自治体共有のレコードに用いる自治体コード */
	public static final String COMMON_JICHITAI_CD = "00000";

	@Id
	@Column(name = "kbn", length = 1)
	private String kbn;

	/** 自治体コード。全自治体共有の場合は {@link #COMMON_JICHITAI_CD} を設定する */
	@Id
	@Column(name = "jichitai_cd", length = 5)
	private String jichitaiCd;

	/** タグ付きテキスト */
	@Column(name = "contents")
	private String contents;

}

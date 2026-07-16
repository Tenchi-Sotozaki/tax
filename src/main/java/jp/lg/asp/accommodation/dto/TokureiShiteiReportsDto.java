package jp.lg.asp.accommodation.dto;

import lombok.Data;

/**
 * 納入申告書の提出期限等の特例適用者指定通知帳票DTO
 */
@Data
public class TokureiShiteiReportsDto {

	/** 住所 */
	private String jusho;

	/** 名称 */
	private String name;

	/** 施設所在地 */
	private String shisetsu_jusho;

	/** 施設名称 */
	private String shisetsu_name;

	/** 指定番号 */
	private String shitei_no;

	/** 申請備考 */
	private String biko;
	
	/** 公印 */
	private byte[] koin;
}

package jp.lg.asp.accommodation.dto;

import java.sql.Date;

import lombok.Data;

/**
 * 合算申告納入承認通知書帳票DTO
 */
@Data
public class GassanNonyuTsuchiReportsDto {

	/** 住所 */
	private String jusho;

	/** 名称 */
	private String name;

	/** 合算指定番号 */
	private String gassan_shitei_no;

	/** 適用開始年月日 */
	private Date tekiyo_st_ymd;
	
	/** 公印 */
	private byte[] koin;
}

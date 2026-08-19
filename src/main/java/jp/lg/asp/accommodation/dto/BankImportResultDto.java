package jp.lg.asp.accommodation.dto;

import lombok.Data;

/**
 * 金融機関コード取込の結果
 */
@Data
public class BankImportResultDto {

	/** 取り込んだ金融機関の件数 */
	private int bankCount;

	/** 取り込んだ支店の件数 */
	private int branchCount;

	/** 親（金融機関マスタ）が存在せずスキップした金融機関コードの数 */
	private int skippedBankCount;

	/** 親が存在せずスキップした支店の件数 */
	private int skippedBranchCount;

	/** 取り込んだデータの版（zip内 data/updated_at の値） */
	private String updatedAt;
}

package jp.lg.asp.accommodation.dto;

import lombok.Data;

/**
 * 納税管理人選任免除認定（不認定）通知書フィールド用DTO
 */
@Data
public class NozeiKanrininNinteiReportsDto {
	
	/** 特別徴収義務者郵便番号 */
	private String yubin;

	/** 特別徴収義務者住所 */
    private String jusho;

    /** 特別徴収義務者名 */
    private String name;
    
    /** 施設郵便番号 */
    private String shisetsuYubin;

    /** 所在地（施設住所） */
    private String shisetsuJusho;

    /** 名称（施設名） */
    private String shisetsuName;
    
    /** 公印 */
    private byte[] koin;
}

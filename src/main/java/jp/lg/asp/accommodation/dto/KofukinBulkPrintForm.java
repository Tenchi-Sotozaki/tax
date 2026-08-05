package jp.lg.asp.accommodation.dto;

import lombok.Data;

@Data
public class KofukinBulkPrintForm {

	private String hakkoYmd;
	private String nendo;
	private boolean kofuShinsei;
	private boolean kofuKetteiTsuchi;
}

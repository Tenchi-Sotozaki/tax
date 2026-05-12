package jp.lg.asp.accommodation.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EltaxRenkeiKakuninDto {

	private String shiteiNo;
	private String shisetsuName;
	private String shisetsuJusho;
	private String atenaName;
	private String atenaJusho;
	private String fileName;
	private String shubetsu;
	private String shubetsuName;

	private List<DiffRow> diffRows;

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class DiffRow {
		private String itemName;
		private String beforeValue;
		private String afterValue;
	}
}

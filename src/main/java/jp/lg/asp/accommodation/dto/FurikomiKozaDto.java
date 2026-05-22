package jp.lg.asp.accommodation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FurikomiKozaDto {

	// 特別徴収義務者情報（リードオンリー）
	private String shiteiNo;
	private String shisetsuName;
	private String shimei;

	// 振込先口座情報
	@NotBlank(message = "金融機関コードは必須です")
	@Size(max = 4, message = "金融機関コードは4桁以内で入力してください")
	@Pattern(regexp = "^[0-9]*$", message = "金融機関コードは数字で入力してください")
	private String bankCd;

	@NotBlank(message = "金融機関名は必須です")
	@Size(max = 30, message = "金融機関名は30文字以内で入力してください")
	private String bankName;

	@NotBlank(message = "支店コードは必須です")
	@Size(max = 3, message = "支店コードは3桁以内で入力してください")
	@Pattern(regexp = "^[0-9]*$", message = "支店コードは数字で入力してください")
	private String branchCd;

	@NotBlank(message = "支店名は必須です")
	@Size(max = 30, message = "支店名は30文字以内で入力してください")
	private String branchName;

	@NotBlank(message = "預金種目は必須です")
	@Pattern(regexp = "^[12]$", message = "預金種目は1（普通）または2（当座）を選択してください")
	private String shumoku;

	@NotBlank(message = "口座番号は必須です")
	@Size(max = 8, message = "口座番号は8桁以内で入力してください")
	@Pattern(regexp = "^[0-9]*$", message = "口座番号は数字で入力してください")
	private String kozaNo;

	@NotBlank(message = "口座名義は必須です")
	@Size(max = 30, message = "口座名義は30文字以内で入力してください")
	private String meigi;

	// 画面制御用
	private String mode; // view, edit, register
}
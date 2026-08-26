package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JichitaiConfigDto {

	@NotBlank(message = "自治体コードは必須です。")
	private String jichitaiCd;

	@NotBlank(message = "自治体名称は必須です。")
	private String name;

	@NotBlank(message = "自治体種別名は必須です。")
	private String kbnName;

	@NotBlank(message = "年度開始月は必須です。")
	private String nendoStMonth;

	@NotBlank(message = "デフォルト納税周期は必須です。")
	private String nozeiShuki;

	@NotBlank(message = "指定番号は必須です。")
	private String shiteiStChar;

	@NotBlank(message = "合算指定番号は必須です。")
	private String gassanStChar;

	@NotNull(message = "宛名番号は必須です。")
	private BigDecimal atenaStNo;
	
	@NotNull(message = "自治体識別名は必須です。")
	private String param;
	
	@NotNull(message = "ユーザー名は必須です。")
	private String userName;
}

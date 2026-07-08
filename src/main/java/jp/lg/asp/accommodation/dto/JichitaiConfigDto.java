package jp.lg.asp.accommodation.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JichitaiConfigDto {

	@NotBlank(message = "年度開始月は必須です。")
	private String nendoStMonth;
}

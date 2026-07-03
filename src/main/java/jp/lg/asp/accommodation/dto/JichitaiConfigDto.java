package jp.lg.asp.accommodation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JichitaiConfigDto {

	@NotNull(message = "年度開始月を入力してください。")
	@Min(value = 1, message = "年度開始月は1〜12の範囲で入力してください。")
	@Max(value = 12, message = "年度開始月は1〜12の範囲で入力してください。")
	private Integer startMonth;
}

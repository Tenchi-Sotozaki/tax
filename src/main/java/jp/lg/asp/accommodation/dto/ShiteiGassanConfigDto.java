package jp.lg.asp.accommodation.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShiteiGassanConfigDto {

	@Size(max = 3, message = "指定番号は3文字以内で入力してください")
	private String shiteiStChar;

	@Size(max = 3, message = "合算指定番号は3文字以内で入力してください")
	private String gassanStChar;

	private Integer version;
}

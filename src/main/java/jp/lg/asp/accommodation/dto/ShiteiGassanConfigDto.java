package jp.lg.asp.accommodation.dto;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShiteiGassanConfigDto {

	@Size(min = 3, max = 3, message = "指定番号は3文字で入力してください")
	private String shiteiStChar;

	@Size(min = 3, max = 3, message = "合算指定番号は3文字で入力してください")
	private String gassanStChar;

	private Integer version;

	public static Map<String, String> validate(ShiteiGassanConfigDto f) {
		Map<String, String> errors = new LinkedHashMap<>();
		if (f.getShiteiStChar() == null || f.getShiteiStChar().isBlank())
			errors.put("shiteiStChar", "指定番号は必須です");
		else if (f.getShiteiStChar().length() != 3)
			errors.put("shiteiStChar", "指定番号は3文字で入力してください");
		if (f.getGassanStChar() == null || f.getGassanStChar().isBlank())
			errors.put("gassanStChar", "合算指定番号は必須です");
		else if (f.getGassanStChar().length() != 3)
			errors.put("gassanStChar", "合算指定番号は3文字で入力してください");
		if (errors.isEmpty() && f.getShiteiStChar().equals(f.getGassanStChar()))
			errors.put("duplicate", "同じ文字列は登録できません。それぞれ別の文字列を指定してください");
		return errors;
	}
}

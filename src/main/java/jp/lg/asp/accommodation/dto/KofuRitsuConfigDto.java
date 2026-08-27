package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KofuRitsuConfigDto {

	@NotNull(message = "交付率は必須です")
	@DecimalMin(value = "0", message = "交付率は0～999.99の範囲で入力してください")
	@DecimalMax(value = "999.99", message = "交付率は0～999.99の範囲で入力してください")
	private BigDecimal kofuRitsu;

	@NotNull(message = "算出単位は必須です")
	private Integer sanshutsu;

	@NotNull(message = "区分は必須です")
	private String kbn;

	@NotNull(message = "最低額は必須です")
	private BigDecimal saiteigaku;

	@NotNull(message = "適用開始年度は必須です")
	private String tekiyoStNendo;

	public static Map<String, String> validate(KofuRitsuConfigDto f) {
		Map<String, String> errors = new LinkedHashMap<>();
		if (f.getKofuRitsu() == null)
			errors.put("kofuRitsu", "交付率は必須です");
		else if (f.getKofuRitsu().compareTo(BigDecimal.ZERO) < 0 || f.getKofuRitsu().compareTo(new BigDecimal("999.99")) > 0)
			errors.put("kofuRitsu", "交付率は0～999.99の範囲で入力してください");
		if (f.getSanshutsu() == null)
			errors.put("sanshutsu", "算出単位は必須です");
		if (f.getKbn() == null || f.getKbn().isBlank())
			errors.put("kbn", "区分は必須です");
		if (f.getSaiteigaku() == null)
			errors.put("saiteigaku", "最低額は必須です");
		if (f.getTekiyoStNendo() == null)
			errors.put("tekiyoStNendo", "適用開始年度は必須です");
		return errors;
	}
}

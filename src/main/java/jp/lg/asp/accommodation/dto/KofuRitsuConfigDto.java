package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;

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

	@NotNull(message = "適用期間（FROM）は必須です")
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate tekiyoStYmd;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate tekiyoEdYmd;

	public static Map<String, String> validate(KofuRitsuConfigDto f) {
		Map<String, String> errors = new LinkedHashMap<>();
		if (f.getKofuRitsu() == null)
			errors.put("kofuRitsu", "交付率は必須です");
		else if (f.getKofuRitsu().compareTo(BigDecimal.ZERO) < 0 || f.getKofuRitsu().compareTo(new BigDecimal("999.99")) > 0)
			errors.put("kofuRitsu", "交付率は0～999.99の範囲で入力してください");
		if (f.getTekiyoStYmd() == null)
			errors.put("tekiyoStYmd", "適用期間（FROM）は必須です");
		return errors;
	}
}

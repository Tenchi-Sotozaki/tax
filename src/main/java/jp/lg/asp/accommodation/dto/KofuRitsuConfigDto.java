package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KofuRitsuConfigDto {

	@NotNull(message = "交付率を入力してください")
	@DecimalMin(value = "0", message = "交付率の入力値が不正です。0～999.99の間で入力してください")
	@DecimalMax(value = "999.99", message = "交付率の入力値が不正です。0～999.99の間で入力してください")
	private BigDecimal kofuRitsu;

	@NotNull(message = "適用期間（FROM）を入力してください")
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate tekiyoStYmd;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate tekiyoEdYmd;
}

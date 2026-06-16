package jp.lg.asp.accommodation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

/**
 * Web MVCの設定クラス
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

	private final CommaFormattedStringToLongConverter commaFormattedStringToLongConverter;

	@Override
	public void addFormatters(FormatterRegistry registry) {
		registry.addConverter(commaFormattedStringToLongConverter);
	}
}
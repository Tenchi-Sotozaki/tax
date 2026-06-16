package jp.lg.asp.accommodation.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * カンマ区切りの数値文字列をLongに変換するコンバータ
 */
@Component
public class CommaFormattedStringToLongConverter implements Converter<String, Long> {

	@Override
	public Long convert(String source) {
		if (!StringUtils.hasText(source)) {
			return null;
		}

		// カンマを除去して数値に変換
		String cleanedSource = source.replaceAll(",", "");

		try {
			return Long.valueOf(cleanedSource);
		} catch (NumberFormatException e) {
			// 変換できない場合はnullを返すか例外を投げる
			throw new IllegalArgumentException("Invalid number format: " + source, e);
		}
	}
}
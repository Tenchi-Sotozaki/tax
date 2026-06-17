package jp.lg.asp.accommodation.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * カンマ区切りの数値文字列をIntegerに変換するコンバータ
 */
@Component
public class CommaFormattedStringToIntegerConverter implements Converter<String, Integer> {

	@Override
	public Integer convert(String source) {
		if (!StringUtils.hasText(source)) {
			return null;
		}

		// カンマを除去して数値に変換
		String cleanedSource = source.replaceAll(",", "");

		try {
			return Integer.valueOf(cleanedSource);
		} catch (NumberFormatException e) {
			// 変換できない場合はnullを返すか例外を投げる
			throw new IllegalArgumentException("Invalid number format: " + source, e);
		}
	}
}
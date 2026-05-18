package jp.lg.asp.accommodation.constant;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ZeiritsuConstants {

	private final String value;
	private final String name;

	// 対象区分
	public static final ZeiritsuConstants CITY = new ZeiritsuConstants("1", "市区町村");
	public static final ZeiritsuConstants KEN = new ZeiritsuConstants("2", "都道府県");
}

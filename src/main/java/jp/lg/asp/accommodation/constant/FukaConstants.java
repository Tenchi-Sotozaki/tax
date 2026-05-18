package jp.lg.asp.accommodation.constant;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class FukaConstants {

	private final String value;
	private final String name;

	// 賦課方式
	public static final FukaConstants TEIGAKU  = new FukaConstants("1", "定額");
	public static final FukaConstants TEIRITSU = new FukaConstants("2", "定率");

	// 変更区分
	public static final FukaConstants SHINKI  = new FukaConstants("1", "新規");
	public static final FukaConstants SHUESEI = new FukaConstants("2", "修正");
	public static final FukaConstants KOSEI   = new FukaConstants("3", "更正");
}

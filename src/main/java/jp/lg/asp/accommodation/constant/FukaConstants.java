package jp.lg.asp.accommodation.constant;

import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class FukaConstants {

	private final String value;
	private final String name;

	// =========================================================
	// 定数定義
	// =========================================================

	// 賦課方式
	public static final FukaConstants TEIGAKU = new FukaConstants("1", "定額");
	public static final FukaConstants TEIRITSU = new FukaConstants("2", "定率");

	// 変更区分
	public static final FukaConstants SHINKOKU = new FukaConstants("1", "申告");
	public static final FukaConstants KOSEI = new FukaConstants("2", "更正");
	public static final FukaConstants KETTEI = new FukaConstants("3", "決定");
	public static final FukaConstants KANPU = new FukaConstants("4", "還付");
	public static final FukaConstants MENJO = new FukaConstants("5", "免除");

	public static final List<FukaConstants> HENKO_KUBUN_LIST = List.of(SHINKOKU, KOSEI, KETTEI, KANPU, MENJO);
}
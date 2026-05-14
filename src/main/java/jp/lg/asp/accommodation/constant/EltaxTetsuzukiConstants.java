package jp.lg.asp.accommodation.constant;

import java.util.Map;

public final class EltaxTetsuzukiConstants {

	private EltaxTetsuzukiConstants() {
	}

	public static final Map<String, String> TETSUZUKI_SHUBETSU_MAP = Map.of(
			"R0402N05", "02",
			"R0402N06", "03",
			"R0402N17", "04",
			"R0402N18", "05",
			"R0402N08", "01");

	public static final Map<String, String> TETSUZUKI_YOSHIKI_MAP = Map.of(
			"R0402N05", "yoshiki/nonyuShinkoku_teigaku.csv",
			"R0402N06", "yoshiki/nonyuShinkoku_teiritsu.csv",
			"R0402N17", "yoshiki/nonyuShinkoku_tokurei_teigaku.csv",
			"R0402N18", "yoshiki/nonyuShinkoku_tokurei_teiritsu.csv",
			"R0402N08", "yoshiki/tokubetsuChoshuShinseiSho.csv");

	public static final Map<String, String> SHUBETSU_NAME_MAP = Map.of(
			"01", "特別徴収義務者",
			"02", "納入申告（定額）",
			"03", "納入申告（定率）",
			"04", "特例納入申告（定額）",
			"05", "特例納入申告（定率）");

}

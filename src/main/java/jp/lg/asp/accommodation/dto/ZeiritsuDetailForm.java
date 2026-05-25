package jp.lg.asp.accommodation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ZeiritsuDetailForm {

	/** 税額（定額）または税率（定率） */
	private String zeiValue;

	/** 〇〇円以上（ryokin_st） - 定額用 */
	private String ryokinSt;

	/** 〇〇円未満（ryokin_ed） - 定額用 */
	private String ryokinEd;

	/** 区分名（kbn_name） - 定率用 */
	private String kbnName;
}

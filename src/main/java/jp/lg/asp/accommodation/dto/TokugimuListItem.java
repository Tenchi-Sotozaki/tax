package jp.lg.asp.accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokugimuListItem {

	private Long id;
	private String registrationNo;
	private String obligorName;
	private String facilityName;
	private String businessType;
	private String businessTypeLabel;
	private String consolidationTarget; // "target" or "non-target"
	/** ステータス: 1=営業中 / 2=休止 / 3=廃止 */
	private String status;
	/** 個人番号 */
	private String kojinNo;
	/** 法人番号 */
	private String hojinNo;

	// コンストラクタ（既存のコードとの互換性のため）
	public TokugimuListItem(Long id, String registrationNo, String obligorName, String facilityName,
			String businessType, String businessTypeLabel, String consolidationTarget, String status) {
		this.id = id;
		this.registrationNo = registrationNo;
		this.obligorName = obligorName;
		this.facilityName = facilityName;
		this.businessType = businessType;
		this.businessTypeLabel = businessTypeLabel;
		this.consolidationTarget = consolidationTarget;
		this.status = status;
	}
}

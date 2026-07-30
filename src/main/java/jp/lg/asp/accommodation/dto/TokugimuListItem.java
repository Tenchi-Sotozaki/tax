package jp.lg.asp.accommodation.dto;

import java.time.LocalDate;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TokugimuListItem {

	private Long id;
	private String shiteiNo;
	private String name;
	private String shisetsuName;
	private String businessType;
	private String businessTypeLabel;
	private String consolidationTarget; // "target" or "non-target"
	/** ステータス: 1=営業中 / 2=休止 / 3=廃止 */
	private String status;
	/** 個人番号 */
	private String kojinNo;
	/** 法人番号 */
	private String hojinNo;

	/** 合算指定番号（合算対象でない場合は null） */
	private String gassanShiteiNo;

	/** 最終申告日（申告実績が無い場合は null） */
	private LocalDate lastShinkokuYmd;

	/**
	 * 最終申告分の納付状況。
	 * paid=完納 / partial=一部納付 / unpaid=未納。
	 * 申告実績が無い場合は null（画面では「-」を表示する）。
	 */
	private String lastNonyuStatus;

	// コンストラクタ（既存のコードとの互換性のため）
	public TokugimuListItem(Long id, String shiteiNo, String name, String shisetsuName,
			String businessType, String businessTypeLabel, String consolidationTarget, String status,
			String kojinNo, String hojinNo) {
		this(id, shiteiNo, name, shisetsuName, businessType, businessTypeLabel, consolidationTarget, status);
		this.kojinNo = kojinNo;
		this.hojinNo = hojinNo;
	}

	public TokugimuListItem(Long id, String shiteiNo, String name, String shisetsuName,
			String businessType, String businessTypeLabel, String consolidationTarget, String status) {
		this.id = id;
		this.shiteiNo = shiteiNo;
		this.name = name;
		this.shisetsuName = shisetsuName;
		this.businessType = businessType;
		this.businessTypeLabel = businessTypeLabel;
		this.consolidationTarget = consolidationTarget;
		this.status = status;
	}
}

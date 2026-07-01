package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class TokugimuForm {

	// 編集時のID保持用（新規登録時は null）
	private Long id;

	// 宛名番号（宛名検索で選択した m_atena.atena_no）
	private Long atenaNo;

	// ===== 特別徴収義務者情報 (m_atena) =====

	@NotNull(message = "登録日は必須です")
	private LocalDate registrationDate;

	@Size(max = 10)
	@NotBlank(message = "郵便番号は必須です")
	private String tokugimuAddressNo; // Atena.yubinNo（読取専用・宛名検索で自動入力）

	@NotBlank(message = "住所は必須です")
	private String tokugimuAddress; // Atena.jusho（読取専用・宛名検索で自動入力）

	@NotBlank(message = "氏名または名称は必須です")
	private String name; // Atena.name（読取専用・宛名検索で自動入力）

	@NotBlank(message = "氏名(ふりがな)は必須です")
	@Size(max = 200)
	private String nameKana; // Atena.nameKana（読取専用・宛名検索で自動入力）

	private String personalNumber; // Atena.kojinNo（読取専用・宛名検索で自動入力）
	private String corporateNumber; // Atena.hojinNo（読取専用・宛名検索で自動入力）

	@NotBlank(message = "電話番号は必須です")
	private String tokugimuPhone; // Atena.tel1（読取専用・宛名検索で自動入力）

	// ===== 宿泊施設情報 (t_tokugimu) =====

	@Size(max = 10)
	private String facilityAddressNo; // Tokugimu.shisetsuYubinNo

	@Size(max = 200)
	private String facilityAddress; // Tokugimu.shisetsuJusho

	@NotBlank(message = "施設名称は必須です")
	@Size(max = 200)
	private String facilityName; // Tokugimu.shisetsuName

	@NotBlank(message = "施設名称(ふりがな)は必須です")
	@Size(max = 200)
	private String facilityNameKana; // Tokugimu.shisetsuNameKana

	@Size(max = 20)
	private String facilityPhone; // Tokugimu.shisetsuTel

	private BigDecimal floorArea; // Tokugimu.yukaMenseki

	private String aboveGroundFloor; // Tokugimu.chijoKai

	private String basementFloor; // Tokugimu.chikaKai

	private Integer roomCount; // Tokugimu.kyakushitsuSu

	private Integer capacity; // Tokugimu.shuyoSu

	@NotNull(message = "営業開始(予定)日は必須です")
	private LocalDate businessStartDate; // Tokugimu.eigyoStYmd

	// ===== 営業許可等情報 (t_tokugimu) =====

	@Size(max = 10)
	private String licenseAddressNo; // Tokugimu.kyokaYubinNo

	@Size(max = 200)
	private String licenseAddress; // Tokugimu.kyokaJusho

	@NotBlank(message = "営業許可の氏名は必須です")
	@Size(max = 200)
	private String licenseName; // Tokugimu.kyokaName

	@NotBlank(message = "営業許可の氏名(ふりがな)は必須です")
	@Size(max = 200)
	private String licenseNameKana; // Tokugimu.kyokaNameKana

	@Size(max = 20)
	private String licensePhone; // Tokugimu.kyokaTel

	private String businessType; // Tokugimu.kyokaShu

	@Size(max = 200)
	private String licenseNumber; // Tokugimu.kyokaNo

	// ===== 施設所有者情報 (t_shoyusha) =====

	@Size(max = 10)
	private String ownerAddressNo;

	@Size(max = 200)
	private String ownerAddress;

	@NotBlank(message = "所有者の氏名(ふりがな)は必須です")
	@Size(max = 200)
	private String ownerNameKana;

	@NotBlank(message = "所有者の氏名は必須です")
	@Size(max = 200)
	private String ownerName;

	@Size(max = 20)
	private String ownerPhone;

	// ===== 書類送付先情報 (t_tokugimu) =====

	@Size(max = 10)
	private String mailAddressNo; // Tokugimu.soufusakiYubinNo

	@Size(max = 200)
	private String mailAddress; // Tokugimu.soufusakiJusho

	@NotBlank(message = "書類送付先の氏名(ふりがな)は必須です")
	@Size(max = 200)
	private String mailNameKana; // Tokugimu.soufusakiNameKana

	@NotBlank(message = "書類送付先の氏名は必須です")
	@Size(max = 200)
	private String mailName; // Tokugimu.soufusakiName

	@Size(max = 20)
	private String mailPhone; // Tokugimu.soufusakiTel

	// ===== 共同事業者情報 (t_kyodo_jigyosha) =====
	private boolean kyodoFlg;
	private List<KyodoJigyoshaDto> kyodoList = new ArrayList<>();

	@AssertTrue(message = "共同事業者の氏名は必須です")
	public boolean isKyodoNameValid() {
		if (!kyodoFlg) return true;
		return kyodoList.stream().allMatch(k -> k.getKyodoName() != null && !k.getKyodoName().isBlank());
	}

	@AssertTrue(message = "共同事業者の氏名(ふりがな)は必須です")
	public boolean isKyodoNameKanaValid() {
		if (!kyodoFlg) return true;
		return kyodoList.stream().allMatch(k -> k.getKyodoNameKana() != null && !k.getKyodoNameKana().isBlank());
	}

	// ===== その他の情報 (t_tokugimu) =====

	private String eltaxUmu; // Tokugimu.eltaxUmu
	private BigDecimal taxCycle; // Tokugimu.nokigen（NozeiShuki.seq）
	private String remarks; // Tokugimu.biko

	// ===== 施設営業休止/再開/廃止情報（編集時のみ使用） =====

	private String declarationCategory;
	private LocalDate suspensionStartDate; // Tokugimu.kyushiStYmd
	private LocalDate suspensionEndDate; // Tokugimu.kyushiEdYmd
	private boolean suspensionEndDateUndecided;
	private LocalDate resumptionOrAbolitionDate; // Tokugimu.eigyoEdYmd
	private String suspensionOrAbolitionReason; // Tokugimu.kyuhaishiRiyu

	private String shiteiNo;

	// 納入書用の特別徴収義務者郵便番号を取得
	public String getTokugimuYubinNo() {
		// 送付先郵便番号が優先、なければ特別徴収義務者郵便番号
		return mailAddressNo != null && !mailAddressNo.isBlank()
				? mailAddressNo
				: tokugimuAddressNo;
	}
}

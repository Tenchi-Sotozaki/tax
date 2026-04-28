package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    @Size(max = 200)
    private String tokugimuAddress;         // Atena.jusho（読取専用・宛名検索で自動入力）

    @NotBlank(message = "氏名または名称は必須です")
    @Size(max = 200)
    private String name;                    // Atena.name

    private String personalNumber;          // Atena.kojinNo（読取専用）
    private String corporateNumber;         // Atena.hojinNo（読取専用）

    @NotBlank(message = "電話番号は必須です")
    @Size(max = 20)
    private String tokugimuPhone;           // Atena.tel1

    // ===== 宿泊施設情報 (t_tokugimu) =====

    @NotBlank(message = "所在地(郵便番号)は必須です")
    @Size(max = 10)
    private String facilityAddressNo;       // Tokugimu.shisetsuYubinNo

    @NotBlank(message = "所在地(住所)は必須です")
    @Size(max = 200)
    private String facilityAddress;         // Tokugimu.shisetsuJusho

    @NotBlank(message = "施設名称は必須です")
    @Size(max = 200)
    private String facilityName;            // Tokugimu.shisetsuName

    @NotBlank(message = "施設名称(ふりがな)は必須です")
    @Size(max = 200)
    private String facilityNameKana;        // Tokugimu.shisetsuNameKana

    @NotBlank(message = "施設の電話番号は必須です")
    @Size(max = 20)
    private String facilityPhone;           // Tokugimu.shisetsuTel

    @NotNull(message = "延床面積は必須です")
    private BigDecimal floorArea;           // Tokugimu.yukaMenseki

    @NotBlank(message = "地上階は必須です")
    private String aboveGroundFloor;        // Tokugimu.chijoKai

    @NotBlank(message = "地下階は必須です")
    private String basementFloor;           // Tokugimu.chikaKai

    @NotNull(message = "客室数は必須です")
    private Integer roomCount;              // Tokugimu.kyakushitsuSu

    @NotNull(message = "収容人数は必須です")
    private Integer capacity;               // Tokugimu.shuyoSu

    @NotNull(message = "営業開始(予定)日は必須です")
    private LocalDate businessStartDate;    // Tokugimu.eigyoStYmd

    // ===== 営業許可等情報 (t_tokugimu) =====

    @NotBlank(message = "営業許可の住所(郵便番号)は必須です")
    @Size(max = 10)
    private String licenseAddressNo;        // Tokugimu.kyokaYubinNo

    @NotBlank(message = "営業許可の住所は必須です")
    @Size(max = 200)
    private String licenseAddress;          // Tokugimu.kyokaJusho

    @NotBlank(message = "営業許可の氏名は必須です")
    @Size(max = 200)
    private String licenseName;             // Tokugimu.kyokaName

    @NotBlank(message = "営業許可のふりがなは必須です")
    @Size(max = 200)
    private String licenseNameKana;         // Tokugimu.kyokaNameKana

    @NotBlank(message = "営業許可の電話番号は必須です")
    @Size(max = 20)
    private String licensePhone;            // Tokugimu.kyokaTel

    @NotBlank(message = "営業種別は必須です")
    private String businessType;            // Tokugimu.kyokaShu

    @NotBlank(message = "許可番号は必須です")
    @Size(max = 200)
    private String licenseNumber;           // Tokugimu.kyokaNo

    // ===== 施設所有者情報 (t_shoyusha) =====

    @NotBlank(message = "所有者の住所(郵便番号)は必須です")
    @Size(max = 10)
    private String ownerAddressNo;

    @NotBlank(message = "所有者の住所は必須です")
    @Size(max = 200)
    private String ownerAddress;

    @NotBlank(message = "所有者のふりがなは必須です")
    @Size(max = 200)
    private String ownerNameKana;

    @NotBlank(message = "所有者の氏名は必須です")
    @Size(max = 200)
    private String ownerName;

    @NotBlank(message = "所有者の電話番号は必須です")
    @Size(max = 20)
    private String ownerPhone;

    // ===== 書類送付先情報 (t_tokugimu) =====

    @NotBlank(message = "書類送付先の住所(郵便番号)は必須です")
    @Size(max = 10)
    private String mailAddressNo;           // Tokugimu.soufusakiYubinNo

    @NotBlank(message = "書類送付先の住所は必須です")
    @Size(max = 200)
    private String mailAddress;             // Tokugimu.soufusakiJusho

    @NotBlank(message = "書類送付先のふりがな氏名は必須です")
    @Size(max = 200)
    private String mailNameKana;            // Tokugimu.soufusakiNameKana

    @NotBlank(message = "書類送付先の氏名は必須です")
    @Size(max = 200)
    private String mailName;                // Tokugimu.soufusakiName

    @NotBlank(message = "書類送付先の電話番号は必須です")
    @Size(max = 20)
    private String mailPhone;               // Tokugimu.soufusakiTel

    // ===== その他の情報 (t_tokugimu) =====

    private String eltaxApplication;        // Tokugimu.eltaxUmu
    private String taxCycle;                // Tokugimu.nokigen（NozeiShuki.seq）
    private String remarks;                 // Tokugimu.biko

    // ===== 施設営業休止/再開/廃止情報（編集時のみ使用） =====

    private String declarationCategory;
    private LocalDate suspensionStartDate;          // Tokugimu.kyushiStYmd
    private LocalDate suspensionEndDate;            // Tokugimu.kyushiEdYmd
    private boolean suspensionEndDateUndecided;
    private LocalDate resumptionOrAbolitionDate;    // Tokugimu.eigyoEdYmd
    private String suspensionOrAbolitionReason;     // Tokugimu.kyuhaishiRiyu
}

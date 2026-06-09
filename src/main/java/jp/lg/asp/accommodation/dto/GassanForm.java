package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GassanForm {

    private String gassanShiteiNo;

    /** 遷移元指定番号（画面保持用） */
    private String fromShiteiNo;

    @NotNull(message = "宛名番号は必須です")
    private BigDecimal atenaNo;

    /** 特別徴収義務者名（表示用） */
    private String atenaName;

    @NotNull(message = "適用時期は必須です")
    private LocalDate tekiyoStYmd;

    @NotNull(message = "登録年月日は必須です")
    private LocalDate torokuYmd;

    /** 選択可能な施設一覧（表示用） */
    private List<FacilityItem> facilityList = new ArrayList<>();

    /** 適用時期選択リスト（照会画面用） */
    private List<GassanListItem> gassanList = new ArrayList<>();

    /** チェックされた指定番号リスト（登録対象） */
    @Size(min = 2, message = "合算対象施設を2件以上選択してください")
    private List<String> shiteiNoList;

    @Data
    public static class FacilityItem {
        private String shiteiNo;
        private String shisetsuName;
        private boolean checked;

        public FacilityItem(String shiteiNo, String shisetsuName, boolean checked) {
            this.shiteiNo = shiteiNo;
            this.shisetsuName = shisetsuName;
            this.checked = checked;
        }
    }
}

package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GassanForm {

    private String gassanShiteiNo;
    private String fromShiteiNo;

    @NotNull(message = "登録年月日は必須です")
    private LocalDate torokuYmd;

    @NotNull(message = "申告日は必須です")
    private LocalDate shinkokuYmd;

    @NotNull(message = "宛名番号は必須です")
    private BigDecimal atenaNo;

    private String atenaName;

    @NotNull(message = "適用時期は必須です")
    private LocalDate tekiyoStYmd;

    private LocalDate tekiyoEdYmd;

    private List<FacilityItem> facilityList = new ArrayList<>();
    private List<GassanListItem> gassanList = new ArrayList<>();

    @Size(min = 2, message = "合算対象施設を2件以上選択してください")
    private List<String> shiteiNoList;

    private String daihyoShiteiNo;

    public static Map<String, String> validate(GassanForm f) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (f.getTorokuYmd() == null) errors.put("torokuYmd", "登録年月日は必須です");
        if (f.getShinkokuYmd() == null) errors.put("shinkokuYmd", "申告日は必須です");
        if (f.getAtenaNo() == null) errors.put("atenaNo", "宛名番号は必須です");
        if (f.getTekiyoStYmd() == null) errors.put("tekiyoStYmd", "適用時期は必須です");
        if (f.getShiteiNoList() == null || f.getShiteiNoList().size() < 2) errors.put("shiteiNoList", "合算対象施設を2件以上選択してください");
        return errors;
    }

    @Data
    public static class FacilityItem {
        private String shiteiNo;
        private String shisetsuName;
        private String choshuGimushaName;
        private boolean checked;
        private boolean daihyo;

        public FacilityItem() {}

        public FacilityItem(String shiteiNo, String shisetsuName, String choshuGimushaName, boolean checked) {
            this.shiteiNo = shiteiNo;
            this.shisetsuName = shisetsuName;
            this.choshuGimushaName = choshuGimushaName;
            this.checked = checked;
            this.daihyo = false;
        }
    }

    @Data
    public static class GassanListItem {
        private String gassanShiteiNo;
        private LocalDate tekiyoStYmd;

        public GassanListItem() {}

        public GassanListItem(String gassanShiteiNo, LocalDate tekiyoStYmd) {
            this.gassanShiteiNo = gassanShiteiNo;
            this.tekiyoStYmd = tekiyoStYmd;
        }
    }
}

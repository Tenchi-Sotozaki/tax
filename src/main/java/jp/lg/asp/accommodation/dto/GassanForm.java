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
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class GassanForm {

    public interface RegisterGroup {}

    private String gassanShiteiNo;
    private String fromShiteiNo;

    @NotNull(message = "登録年月日は必須です", groups = RegisterGroup.class)
    private LocalDate torokuYmd;

    @NotNull(message = "申告日は必須です", groups = RegisterGroup.class)
    private LocalDate shinkokuYmd;

    @NotNull(message = "宛名番号は必須です", groups = RegisterGroup.class)
    private BigDecimal atenaNo;

    private String atenaName;

    @NotNull(message = "適用時期は必須です", groups = RegisterGroup.class)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate tekiyoStYmd;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate tekiyoEdYmd;

    private List<FacilityItem> facilityList = new ArrayList<>();
    private List<GassanListItem> gassanList = new ArrayList<>();

    @Size(min = 2, message = "合算対象施設を2件以上選択してください", groups = RegisterGroup.class)
    private List<String> shiteiNoList;

    private String daihyoShiteiNo;

    private BigDecimal rno;
    private BigDecimal maxRno;
    private BigDecimal minRno;
    private BigDecimal prevRno;
    private BigDecimal nextRno;
    private BigDecimal currentNo;

    public static Map<String, String> validate(GassanForm f) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (f.getTorokuYmd() == null) errors.put("torokuYmd", "登録年月日は必須です");
        if (f.getShinkokuYmd() == null) errors.put("shinkokuYmd", "申告日は必須です");
        if (f.getAtenaNo() == null) errors.put("atenaNo", "宛名番号は必須です");
        if (f.getTekiyoStYmd() == null) errors.put("tekiyoStYmd", "適用時期は必須です");
        if (f.getShiteiNoList() == null || f.getShiteiNoList().size() < 2) errors.put("shiteiNoList", "合算対象施設を2件以上選択してください");
        return errors;
    }

    public static Map<String, String> validateForEdit(GassanForm f) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (f.getTorokuYmd() == null) errors.put("torokuYmd", "登録年月日は必須です");
        if (f.getShinkokuYmd() == null) errors.put("shinkokuYmd", "申告日は必須です");
        if (f.getAtenaNo() == null) errors.put("atenaNo", "宛名番号は必須です");
        if (f.getTekiyoStYmd() == null) errors.put("tekiyoStYmd", "適用時期は必須です");
        return errors;
    }

    @Data
    public static class FacilityItem {
        private String shiteiNo;
        private String shisetsuName;
        private String choshuGimushaName;
        private boolean checked;
        private boolean daihyo;
        private boolean disabled;
        private String gassanShiteiNo;

        public FacilityItem() {}

        public FacilityItem(String shiteiNo, String shisetsuName, String choshuGimushaName, boolean checked) {
            this.shiteiNo = shiteiNo;
            this.shisetsuName = shisetsuName;
            this.choshuGimushaName = choshuGimushaName;
            this.checked = checked;
            this.daihyo = false;
            this.disabled = false;
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

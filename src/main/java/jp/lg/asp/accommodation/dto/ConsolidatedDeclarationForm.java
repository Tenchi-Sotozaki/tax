package jp.lg.asp.accommodation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class ConsolidatedDeclarationForm {

    @NotNull(message = "登録日は必須です")
    private LocalDate registrationDate;

    private String obligorId;
    private String obligorName;

    @NotNull(message = "適用時期は必須です")
    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth applicablePeriod;

    @NotEmpty(message = "施設は必須です")
    private List<String> facilities;

    public static Map<String, String> validate(ConsolidatedDeclarationForm f) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (f.getRegistrationDate() == null) errors.put("registrationDate", "登録日は必須です");
        if (f.getApplicablePeriod() == null) errors.put("applicablePeriod", "適用時期は必須です");
        if (f.getFacilities() == null || f.getFacilities().isEmpty()) errors.put("facilities", "施設は必須です");
        return errors;
    }
}

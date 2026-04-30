package jp.lg.asp.accommodation.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TaxManagerValid 
public class TaxManagerForm {

    private Long collectorId;
    private String obligorName;
    private String facilityName;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate registrationDate;

    // 動的にチェックするため、個別の @NotBlank は削除、または
    // 常に必須な項目（登録日など）にだけ残します。
    private String managerAddress;
    private String managerName;
    private String managerNameKana;
    private String managerPhone;

    private boolean exemptionFlag;
    private String exemptionReason;
    
    private boolean edit;
}
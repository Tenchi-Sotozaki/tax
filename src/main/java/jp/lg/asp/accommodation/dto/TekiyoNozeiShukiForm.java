package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

@Data
public class TekiyoNozeiShukiForm {

    private String shiteiNo;
    private String obligorName;
    private String facilityName;
    private boolean edit;

    private BigDecimal seq;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate tekiyoStYmd;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate tekiyoEdYmd;
}

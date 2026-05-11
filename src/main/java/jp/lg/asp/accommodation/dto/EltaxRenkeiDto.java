package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EltaxRenkeiDto {

    private BigDecimal seq;
    private String fileName;
    private String shubetsu;
    private LocalDateTime shoriDt;
    private String shoriKekka;
}

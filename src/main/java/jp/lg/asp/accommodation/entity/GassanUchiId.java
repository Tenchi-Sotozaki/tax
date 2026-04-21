package jp.lg.asp.accommodation.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GassanUchiId implements Serializable {
    private String jichitaiCd;
    private String gassanShiteiNo;
    private BigDecimal rno;
    private String shiteiNo;
}
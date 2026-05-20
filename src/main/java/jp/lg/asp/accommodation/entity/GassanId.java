package jp.lg.asp.accommodation.entity;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GassanId implements Serializable {
    private String jichitaiCd;
    private String gassanShiteiNo;
    private BigDecimal rno;
}

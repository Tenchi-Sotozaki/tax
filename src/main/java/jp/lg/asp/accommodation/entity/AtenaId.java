package jp.lg.asp.accommodation.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AtenaId implements Serializable {
    private String jichitaiCd;
    private BigDecimal atenaNo;
}
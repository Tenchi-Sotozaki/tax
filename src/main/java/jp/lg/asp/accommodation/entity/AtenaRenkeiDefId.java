package jp.lg.asp.accommodation.entity;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Data;

@Data
public class AtenaRenkeiDefId implements Serializable {
    private String jichitaiCd;
    private BigDecimal seq;
    private BigDecimal atenaNo;
}

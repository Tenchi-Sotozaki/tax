package jp.lg.asp.accommodation.entity;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZeiritsuTeigakuId implements Serializable {
	private String jichitaiCd;
	private BigDecimal seq;
	private BigDecimal teigakuSeq;
}

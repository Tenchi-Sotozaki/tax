package jp.lg.asp.accommodation.entity;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShoyushaId implements Serializable {
	private String jichitaiCd;
	private String shiteiNo;
	private BigDecimal rno;
	private BigDecimal idx;
}

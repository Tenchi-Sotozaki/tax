package jp.lg.asp.accommodation.entity;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Data;

@Data
public class RoleId implements Serializable {
	private String jichitaiCd;
	private BigDecimal roleId;
}

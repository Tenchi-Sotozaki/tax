package jp.lg.asp.accommodation.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RptStatusId implements Serializable {
	private String jichitaiCd;
	private String shiteiNo;
	private String nendo;
	private String rptId;
}

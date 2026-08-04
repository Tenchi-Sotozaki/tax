package jp.lg.asp.accommodation.entity;

import java.io.Serializable;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KyugyobiId implements Serializable {
	private String jichitaiCd;
	private String nen;
	private LocalDate kyugyobi;
}

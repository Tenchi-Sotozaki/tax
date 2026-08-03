package jp.lg.asp.accommodation.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopPageId implements Serializable {
	private String kbn;
	private String jichitaiCd;
}

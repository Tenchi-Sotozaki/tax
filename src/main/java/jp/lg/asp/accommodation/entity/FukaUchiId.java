package jp.lg.asp.accommodation.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FukaUchiId implements Serializable {

	private static final long serialVersionUID = 1L;

	private String jichitaiCd;
	private String shiteiNo;
	private Integer rno;
	private String nendo;
	private Integer kibetsu;
	private Integer kazeiKbn;
}

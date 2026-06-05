package jp.lg.asp.accommodation.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChoshuGenboUchiId implements Serializable {
    private String jichitaiCd;
    private String shiteiNo;
    private Integer rno;
    private String nendo;
    private Integer kibetsu;
    private Integer kazeiKbn;
}
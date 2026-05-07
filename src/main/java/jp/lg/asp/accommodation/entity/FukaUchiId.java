package jp.lg.asp.accommodation.entity;

import java.io.Serializable;

import lombok.Data;

@Data
public class FukaUchiId implements Serializable {
    private String jichitaiCd;
    private String shiteiNo;
    private String nendo;
    private Integer rno;
    private Integer kibetsu; 
    private Integer kazeiKbn;
}
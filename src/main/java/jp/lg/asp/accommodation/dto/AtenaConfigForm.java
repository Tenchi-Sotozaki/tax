package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AtenaConfigForm {

    private BigDecimal atenaNo;
    private String kojinNo;
    private String hojinNo;
    private String name;
    private String nameKana;
    private String yubinNo;
    private String jusho;
    private String tel1;
    private String tel2;
    private Integer version;
}

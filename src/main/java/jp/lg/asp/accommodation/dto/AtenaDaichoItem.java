package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;

import jp.lg.asp.accommodation.entity.Atena;
import lombok.Getter;

@Getter
public class AtenaDaichoItem {

    private final Atena atena;
    private final boolean manual;

    public AtenaDaichoItem(Atena atena, BigDecimal atenaStNo) {
        this.atena = atena;
        this.manual = atenaStNo != null && atena.getAtenaNo().compareTo(atenaStNo) >= 0;
    }
}

package jp.lg.asp.accommodation.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RptStatusSearchFormTest {

    // =====================================================================
    // #29 既定値
    // =====================================================================

    @Test
    @DisplayName("#29 既定値 一致区分の既定値が \"partial\" であること")
    void 一致区分の既定値がpartial() {
        RptStatusSearchForm form = new RptStatusSearchForm();

        assertEquals("partial", form.getNameMatchType());
        assertEquals("partial", form.getShisetsuNameMatchType());

        assertNull(form.getShiteiNo());
        assertNull(form.getName());
        assertNull(form.getShisetsuName());
        assertNull(form.getKojinNo());
        assertNull(form.getHojinNo());
    }
}

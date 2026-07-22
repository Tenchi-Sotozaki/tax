package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.constant.FukaConstants;
import jp.lg.asp.accommodation.dto.FukaDeclarationForm;
import jp.lg.asp.accommodation.dto.FukaMonthlyDeclarationDto;
import jp.lg.asp.accommodation.dto.FukaTaxDetailDto;
import jp.lg.asp.accommodation.repository.ZeiritsuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeigakuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeiritsuRepository;
import jp.lg.asp.accommodation.service.impl.FukaValidatorServiceImpl;

@ExtendWith(MockitoExtension.class)
class FukaValidatorServiceImplTest {

    @Mock FukaService fukaService;
    @Mock ZeiritsuRepository zeiritsuRepository;
    @Mock ZeiritsuTeiritsuRepository zeiritsuTeiritsuRepository;
    @Mock ZeiritsuTeigakuRepository zeiritsuTeigakuRepository;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks FukaValidatorServiceImpl service;

    private FukaDeclarationForm buildTeigakuForm(long hakusu, long zeigaku, long totalStayCount, long totalPayment) {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setFukaKbn(FukaConstants.TEIGAKU.getValue());
        FukaMonthlyDeclarationDto detail = new FukaMonthlyDeclarationDto();
        FukaTaxDetailDto d = new FukaTaxDetailDto();
        d.setHakusu(hakusu);
        d.setZeigaku(zeigaku);
        d.setTaxRate(BigDecimal.valueOf(200));
        detail.setTaxDetails(List.of(d));
        detail.setTotalStayCount(totalStayCount);
        detail.setTotalPaymentAmount(totalPayment);
        form.setMonthlyDetail(detail);
        return form;
    }

    @Test
    void getDiscrepancyMessages_不整合なし() {
        when(fukaService.calculateTax(any(), anyLong(), any(), any())).thenReturn(200L);
        FukaDeclarationForm form = buildTeigakuForm(1L, 200L, 1L, 200L);

        List<String> messages = service.getDiscrepancyMessages(form);

        assertThat(messages).isEmpty();
    }

    @Test
    void getDiscrepancyMessages_宿泊数不一致() {
        when(fukaService.calculateTax(any(), anyLong(), any(), any())).thenReturn(200L);
        FukaDeclarationForm form = buildTeigakuForm(1L, 200L, 99L, 200L);

        List<String> messages = service.getDiscrepancyMessages(form);

        assertThat(messages).anyMatch(m -> m.contains("宿泊数"));
    }

    @Test
    void getDiscrepancyMessages_税額不一致() {
        when(fukaService.calculateTax(any(), anyLong(), any(), any())).thenReturn(200L);
        FukaDeclarationForm form = buildTeigakuForm(1L, 999L, 1L, 999L);

        List<String> messages = service.getDiscrepancyMessages(form);

        assertThat(messages).anyMatch(m -> m.contains("税額"));
    }

    @Test
    void hasDiscrepancy_不整合あり() {
        when(fukaService.calculateTax(any(), anyLong(), any(), any())).thenReturn(200L);
        FukaDeclarationForm form = buildTeigakuForm(1L, 999L, 1L, 999L);

        assertThat(service.hasDiscrepancy(form)).isTrue();
    }

    @Test
    void hasDiscrepancy_不整合なし() {
        when(fukaService.calculateTax(any(), anyLong(), any(), any())).thenReturn(200L);
        FukaDeclarationForm form = buildTeigakuForm(1L, 200L, 1L, 200L);

        assertThat(service.hasDiscrepancy(form)).isFalse();
    }

    @Test
    void calculateExpectedTotal_定額() {
        when(fukaService.calculateTax(any(), anyLong(), any(), any())).thenReturn(200L);
        FukaDeclarationForm form = buildTeigakuForm(1L, 200L, 1L, 200L);

        long total = service.calculateExpectedTotal(form);

        assertThat(total).isEqualTo(200L);
    }

    @Test
    void calculateExpectedTotal_定率() {
        when(fukaService.calculateTax(any(), anyLong(), any(), any())).thenReturn(500L);
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setFukaKbn(FukaConstants.TEIRITSU.getValue());
        FukaMonthlyDeclarationDto detail = new FukaMonthlyDeclarationDto();
        FukaTaxDetailDto d = new FukaTaxDetailDto();
        d.setRyokin(10000L);
        d.setTaxRate(BigDecimal.valueOf(5));
        detail.setTaxDetails(List.of(d));
        form.setMonthlyDetail(detail);

        long total = service.calculateExpectedTotal(form);

        assertThat(total).isEqualTo(500L);
    }

    @Test
    void getDiscrepancyMessages_monthlyDetailがnullの場合は空リスト() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setMonthlyDetail(null);

        assertThat(service.getDiscrepancyMessages(form)).isEmpty();
    }
}

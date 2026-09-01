package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.ArrayList;
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
import jp.lg.asp.accommodation.dto.FukaMonthlyTallyDto;
import jp.lg.asp.accommodation.dto.FukaMonthlyTallyDto.DailyItem;
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

    // ===== hasDiscrepancy (No.85-87) =====

    // No.85 不整合なしの場合、falseを返す
    @Test
    void hasDiscrepancy_不整合なし_falseを返す() {
        when(fukaService.calculateTax(any(), anyLong(), any(), any())).thenReturn(200L);
        FukaDeclarationForm form = buildTeigakuForm(1L, 200L, 1L, 200L);

        assertThat(service.hasDiscrepancy(form)).isFalse();
    }

    // No.86 不整合ありの場合、trueを返す
    @Test
    void hasDiscrepancy_不整合あり_trueを返す() {
        when(fukaService.calculateTax(any(), anyLong(), any(), any())).thenReturn(200L);
        FukaDeclarationForm form = buildTeigakuForm(1L, 999L, 1L, 999L);

        assertThat(service.hasDiscrepancy(form)).isTrue();
    }

    // No.87 monthlyDetailがnullの場合、falseを返す
    @Test
    void hasDiscrepancy_monthlyDetailがnull_falseを返す() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setMonthlyDetail(null);

        assertThat(service.hasDiscrepancy(form)).isFalse();
    }

    // ===== getDiscrepancyMessages (No.88-105) =====

    // No.88 monthlyDetailがnullの場合、空リストを返す
    @Test
    void getDiscrepancyMessages_monthlyDetailがnull_空リストを返す() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setMonthlyDetail(null);

        assertThat(service.getDiscrepancyMessages(form)).isEmpty();
    }

    // No.89 全項目一致の場合（定額）、空リストを返す
    @Test
    void getDiscrepancyMessages_全項目一致定額_空リストを返す() {
        when(fukaService.calculateTax(any(), anyLong(), any(), any())).thenReturn(200L);
        FukaDeclarationForm form = buildTeigakuForm(1L, 200L, 1L, 200L);

        assertThat(service.getDiscrepancyMessages(form)).isEmpty();
    }

    // No.90 全項目一致の場合（定率）、空リストを返す
    @Test
    void getDiscrepancyMessages_全項目一致定率_空リストを返す() {
        when(fukaService.calculateTax(any(), anyLong(), any(), any())).thenReturn(300L);
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setFukaKbn(FukaConstants.TEIRITSU.getValue());
        FukaMonthlyDeclarationDto detail = new FukaMonthlyDeclarationDto();
        FukaTaxDetailDto d = new FukaTaxDetailDto();
        d.setHakusu(2L); d.setZeigaku(300L); d.setRyokin(15000L);
        d.setTaxRate(BigDecimal.valueOf(2));
        detail.setTaxDetails(List.of(d));
        detail.setTotalStayCount(2L);
        detail.setTotalPaymentAmount(300L);
        detail.setKazeiRyokin(15000L);
        form.setMonthlyDetail(detail);

        assertThat(service.getDiscrepancyMessages(form)).isEmpty();
    }

    // No.91 宿泊数合計不一致の場合、メッセージを返す
    @Test
    void getDiscrepancyMessages_宿泊数合計不一致_メッセージを返す() {
        when(fukaService.calculateTax(any(), anyLong(), any(), any())).thenReturn(200L);
        FukaDeclarationForm form = buildTeigakuForm(1L, 200L, 99L, 200L);

        List<String> messages = service.getDiscrepancyMessages(form);

        assertThat(messages).anyMatch(m -> m.contains("宿泊数"));
    }

    // No.92 税額合計不一致（checkTotalCount）の場合、メッセージを返す
    @Test
    void getDiscrepancyMessages_税額合計不一致checkTotalCount_メッセージを返す() {
        when(fukaService.calculateTax(any(), anyLong(), any(), any())).thenReturn(200L);
        FukaDeclarationForm form = buildTeigakuForm(1L, 999L, 1L, 999L);

        List<String> messages = service.getDiscrepancyMessages(form);

        assertThat(messages).anyMatch(m -> m.contains("税額"));
    }

    // No.93 料金合計不一致（定率）の場合、メッセージを返す
    @Test
    void getDiscrepancyMessages_料金合計不一致定率_メッセージを返す() {
        when(fukaService.calculateTax(any(), anyLong(), any(), any())).thenReturn(300L);
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setFukaKbn(FukaConstants.TEIRITSU.getValue());
        FukaMonthlyDeclarationDto detail = new FukaMonthlyDeclarationDto();
        FukaTaxDetailDto d = new FukaTaxDetailDto();
        d.setHakusu(2L); d.setZeigaku(300L); d.setRyokin(15000L);
        d.setTaxRate(BigDecimal.valueOf(2));
        detail.setTaxDetails(List.of(d));
        detail.setTotalStayCount(2L);
        detail.setTotalPaymentAmount(300L);
        detail.setKazeiRyokin(99999L); // 不一致
        form.setMonthlyDetail(detail);

        List<String> messages = service.getDiscrepancyMessages(form);

        assertThat(messages).anyMatch(m -> m.contains("料金"));
    }

    // No.94 料金合計チェックは定額の場合スキップされる
    @Test
    void getDiscrepancyMessages_料金合計チェックは定額の場合スキップ() {
        when(fukaService.calculateTax(any(), anyLong(), any(), any())).thenReturn(200L);
        FukaDeclarationForm form = buildTeigakuForm(1L, 200L, 1L, 200L);
        // ryokin不一致でも定額なのでチェックされない

        List<String> messages = service.getDiscrepancyMessages(form);

        assertThat(messages).noneMatch(m -> m.contains("料金"));
    }

    // No.95 税額算出不一致（checkTaxTotal）の場合、メッセージを返す
    @Test
    void getDiscrepancyMessages_税額算出不一致checkTaxTotal_メッセージを返す() {
        when(fukaService.calculateTax(any(), anyLong(), any(), any())).thenReturn(999L);
        FukaDeclarationForm form = buildTeigakuForm(1L, 200L, 1L, 200L);

        List<String> messages = service.getDiscrepancyMessages(form);

        assertThat(messages).anyMatch(m -> m.contains("税額"));
    }

    // No.96 taxDetailsが空の場合、税額算出チェックをスキップする
    @Test
    void getDiscrepancyMessages_taxDetailsが空_税額算出チェックをスキップ() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setFukaKbn(FukaConstants.TEIGAKU.getValue());
        FukaMonthlyDeclarationDto detail = new FukaMonthlyDeclarationDto();
        detail.setTaxDetails(new ArrayList<>());
        detail.setTotalStayCount(0L);
        detail.setTotalPaymentAmount(0L);
        form.setMonthlyDetail(detail);

        assertThat(service.getDiscrepancyMessages(form)).isEmpty();
    }

    // No.97 monthlyTallyがnullの場合、月計表突合チェックをスキップする
    @Test
    void getDiscrepancyMessages_monthlyTallyがnull_月計表突合チェックをスキップ() {
        when(fukaService.calculateTax(any(), anyLong(), any(), any())).thenReturn(200L);
        FukaDeclarationForm form = buildTeigakuForm(1L, 200L, 1L, 200L);
        form.setMonthlyTally(null);

        assertThat(service.getDiscrepancyMessages(form)).isEmpty();
    }

    // No.98 dailyItemsが空の場合、月計表突合チェックをスキップする
    @Test
    void getDiscrepancyMessages_dailyItemsが空_月計表突合チェックをスキップ() {
        when(fukaService.calculateTax(any(), anyLong(), any(), any())).thenReturn(200L);
        FukaDeclarationForm form = buildTeigakuForm(1L, 200L, 1L, 200L);
        FukaMonthlyTallyDto tally = new FukaMonthlyTallyDto();
        tally.getDailyItems().clear();
        form.setMonthlyTally(tally);

        assertThat(service.getDiscrepancyMessages(form)).isEmpty();
    }

    // No.99 dailyItemsに全0データの場合、月計表突合チェックをスキップする
    @Test
    void getDiscrepancyMessages_dailyItemsが全0_月計表突合チェックをスキップ() {
        when(fukaService.calculateTax(any(), anyLong(), any(), any())).thenReturn(200L);
        FukaDeclarationForm form = buildTeigakuForm(1L, 200L, 1L, 200L);
        FukaMonthlyTallyDto tally = new FukaMonthlyTallyDto();
        tally.initialize(1);
        // 全0のまま
        form.setMonthlyTally(tally);

        assertThat(service.getDiscrepancyMessages(form)).isEmpty();
    }

    // No.100 月計表と宿泊数が不一致の場合、メッセージを返す
    @Test
    void getDiscrepancyMessages_月計表と宿泊数不一致_メッセージを返す() {
        when(fukaService.calculateTax(any(), anyLong(), any(), any())).thenReturn(200L);
        FukaDeclarationForm form = buildTeigakuForm(5L, 200L, 5L, 200L);
        FukaMonthlyTallyDto tally = new FukaMonthlyTallyDto();
        tally.initialize(1);
        // 1日目の宿泊数を10に設定（合計10 vs 入力値5で不一致）
        tally.getDailyItems().get(0).getHakusu().set(0, 10);
        form.setMonthlyTally(tally);

        List<String> messages = service.getDiscrepancyMessages(form);

        assertThat(messages).anyMatch(m -> m.contains("宿泊数"));
    }

    // No.103 月計表と免除泊数が不一致の場合、メッセージを返す
    @Test
    void getDiscrepancyMessages_月計表と免除泊数不一致_メッセージを返す() {
        when(fukaService.calculateTax(any(), anyLong(), any(), any())).thenReturn(200L);
        FukaDeclarationForm form = buildTeigakuForm(1L, 200L, 1L, 200L);
        form.getMonthlyDetail().setExemptStayCount(0L);
        FukaMonthlyTallyDto tally = new FukaMonthlyTallyDto();
        tally.initialize(1);
        // 1日目の免除泊数を5に設定（合計5 vs 入力値0で不一致）
        tally.getDailyItems().get(0).setMenjoHakusu(5);
        form.setMonthlyTally(tally);

        List<String> messages = service.getDiscrepancyMessages(form);

        assertThat(messages).anyMatch(m -> m.contains("免除"));
    }

    // No.105 月計表と税額が不一致の場合、メッセージを返す
    @Test
    void getDiscrepancyMessages_月計表と税額不一致_メッセージを返す() {
        when(fukaService.calculateTax(any(), anyLong(), any(), any())).thenReturn(200L);
        FukaDeclarationForm form = buildTeigakuForm(1L, 200L, 1L, 200L);
        FukaMonthlyTallyDto tally = new FukaMonthlyTallyDto();
        tally.initialize(1);
        // 1日目の税額を999に設定（合計999 vs 入力値200で不一致）
        tally.getDailyItems().get(0).setZeigaku(999L);
        form.setMonthlyTally(tally);

        List<String> messages = service.getDiscrepancyMessages(form);

        assertThat(messages).anyMatch(m -> m.contains("税額"));
    }

    // ===== calculateExpectedTotal (No.106-111) =====

    // No.106 monthlyDetailがnullの場合、0を返す
    @Test
    void calculateExpectedTotal_monthlyDetailがnull_0を返す() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setMonthlyDetail(null);

        assertThat(service.calculateExpectedTotal(form)).isEqualTo(0L);
    }

    // No.107 taxDetailsが空の場合、0を返す
    @Test
    void calculateExpectedTotal_taxDetailsが空_0を返す() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setFukaKbn(FukaConstants.TEIGAKU.getValue());
        form.getMonthlyDetail().setTaxDetails(new ArrayList<>());

        assertThat(service.calculateExpectedTotal(form)).isEqualTo(0L);
    }

    // No.108 定額・1区分の場合、hakusuで税額を算出して返す
    @Test
    void calculateExpectedTotal_定額1区分_hakusuで税額を算出() {
        when(fukaService.calculateTax(any(), anyLong(), any(), any())).thenReturn(1000L);
        FukaDeclarationForm form = buildTeigakuForm(10L, 1000L, 10L, 1000L);

        assertThat(service.calculateExpectedTotal(form)).isEqualTo(1000L);
    }

    // No.109 定率・1区分の場合、ryokinで税額を算出して返す
    @Test
    void calculateExpectedTotal_定率1区分_ryokinで税額を算出() {
        when(fukaService.calculateTax(any(), anyLong(), any(), any())).thenReturn(2000L);
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setFukaKbn(FukaConstants.TEIRITSU.getValue());
        FukaMonthlyDeclarationDto detail = new FukaMonthlyDeclarationDto();
        FukaTaxDetailDto d = new FukaTaxDetailDto();
        d.setRyokin(100000L);
        d.setTaxRate(BigDecimal.valueOf(2));
        detail.setTaxDetails(List.of(d));
        form.setMonthlyDetail(detail);

        assertThat(service.calculateExpectedTotal(form)).isEqualTo(2000L);
    }

    // No.110 複数区分の場合、各区分の税額を合算して返す
    @Test
    void calculateExpectedTotal_複数区分_各区分の税額を合算() {
        when(fukaService.calculateTax(any(), anyLong(), any(), any()))
                .thenReturn(1000L).thenReturn(2000L);
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setFukaKbn(FukaConstants.TEIGAKU.getValue());
        FukaMonthlyDeclarationDto detail = new FukaMonthlyDeclarationDto();
        FukaTaxDetailDto d1 = new FukaTaxDetailDto(); d1.setHakusu(5L); d1.setTaxRate(BigDecimal.valueOf(200));
        FukaTaxDetailDto d2 = new FukaTaxDetailDto(); d2.setHakusu(10L); d2.setTaxRate(BigDecimal.valueOf(200));
        detail.setTaxDetails(List.of(d1, d2));
        detail.setTotalStayCount(15L);
        detail.setTotalPaymentAmount(3000L);
        form.setMonthlyDetail(detail);

        assertThat(service.calculateExpectedTotal(form)).isEqualTo(3000L);
    }

    // No.111 hakusu/ryokinがnullの場合、0として計算する
    @Test
    void calculateExpectedTotal_hakusuがnull_0として計算() {
        when(fukaService.calculateTax(any(), anyLong(), any(), any())).thenReturn(0L);
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setFukaKbn(FukaConstants.TEIGAKU.getValue());
        FukaMonthlyDeclarationDto detail = new FukaMonthlyDeclarationDto();
        FukaTaxDetailDto d = new FukaTaxDetailDto();
        d.setHakusu(null);
        d.setTaxRate(BigDecimal.valueOf(200));
        detail.setTaxDetails(List.of(d));
        form.setMonthlyDetail(detail);

        assertThat(service.calculateExpectedTotal(form)).isEqualTo(0L);
    }
}

package jp.lg.asp.accommodation.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.RptStatusListItem;
import jp.lg.asp.accommodation.dto.RptStatusSearchForm;
import jp.lg.asp.accommodation.entity.Reports;
import jp.lg.asp.accommodation.service.RptStatusService;

@ExtendWith(MockitoExtension.class)
class RptStatusControllerTest {

    private static final String SCREEN_ID = ScreenManagement.TOKUGIMU_STATUS_VIEW;
    private static final String VIEW = "tokugimu/tTokugimuReportsStatus";

    @InjectMocks
    private RptStatusController controller;

    @Mock
    private RptStatusService rptStatusService;

    @Mock
    private ScreenAccessChecker accessChecker;

    // =====================================================================
    // #1 init 正常系
    // =====================================================================

    @Test
    @DisplayName("#1 init 正常系 初期表示：検索フォーム・帳票リスト・空の一覧が設定される")
    void init_初期表示() {
        Reports r1 = new Reports();
        r1.setRptId("R001");
        r1.setRptName("指定通知書");
        Reports r2 = new Reports();
        r2.setRptId("R002");
        r2.setRptName("受理通知書");

        when(rptStatusService.findAllReports()).thenReturn(List.of(r1, r2));

        Model model = new ExtendedModelMap();
        String result = controller.init(model);

        assertEquals(VIEW, result);

        RptStatusSearchForm form = (RptStatusSearchForm) model.asMap().get("searchForm");
        assertNotNull(form);
        assertNull(form.getShiteiNo());
        assertNull(form.getName());
        assertEquals("partial", form.getNameMatchType());
        assertNull(form.getShisetsuName());
        assertEquals("partial", form.getShisetsuNameMatchType());
        assertNull(form.getKojinNo());
        assertNull(form.getHojinNo());

        @SuppressWarnings("unchecked")
        List<Reports> reports = (List<Reports>) model.asMap().get("reports");
        assertEquals(2, reports.size());
        assertEquals("R001", reports.get(0).getRptId());

        @SuppressWarnings("unchecked")
        List<?> items = (List<?>) model.asMap().get("items");
        assertTrue(items.isEmpty());

        assertEquals(false, model.asMap().get("isSearched"));

        verify(rptStatusService, times(1)).findAllReports();
        verify(rptStatusService, never()).search(any());
        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #2 init 異常系
    // =====================================================================

    @Test
    @DisplayName("#2 init 異常系 帳票マスタが0件の場合")
    void init_帳票マスタが0件() {
        when(rptStatusService.findAllReports()).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        String result = controller.init(model);

        assertEquals(VIEW, result);

        @SuppressWarnings("unchecked")
        List<?> reports = (List<?>) model.asMap().get("reports");
        assertNotNull(reports);
        assertTrue(reports.isEmpty());

        @SuppressWarnings("unchecked")
        List<?> items = (List<?>) model.asMap().get("items");
        assertTrue(items.isEmpty());

        assertEquals(false, model.asMap().get("isSearched"));

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #3 search 正常系
    // =====================================================================

    @Test
    @DisplayName("#3 search 正常系 検索結果1件の場合")
    void search_検索結果1件() {
        RptStatusListItem item = new RptStatusListItem();
        item.setShiteiNo("S001");
        item.setName("山田太郎");
        item.setShisetsuName("ホテルA");
        item.setRptStatusMap(Map.of("R001", LocalDateTime.of(2026, 4, 1, 10, 0)));

        Reports r1 = new Reports();
        r1.setRptId("R001");

        when(rptStatusService.search(any())).thenReturn(List.of(item));
        when(rptStatusService.findAllReports()).thenReturn(List.of(r1));

        RptStatusSearchForm form = new RptStatusSearchForm();
        form.setShiteiNo("S001");
        form.setName("山田");
        form.setNameMatchType("partial");
        form.setShisetsuName("ホテル");
        form.setShisetsuNameMatchType("partial");

        Model model = new ExtendedModelMap();
        String result = controller.search(form, model);

        assertEquals(VIEW, result);

        @SuppressWarnings("unchecked")
        List<RptStatusListItem> items = (List<RptStatusListItem>) model.asMap().get("items");
        assertEquals(1, items.size());
        assertEquals("S001", items.get(0).getShiteiNo());
        assertEquals(LocalDateTime.of(2026, 4, 1, 10, 0), items.get(0).getRptStatusMap().get("R001"));

        assertSame(form, model.asMap().get("searchForm"));

        @SuppressWarnings("unchecked")
        List<?> reports = (List<?>) model.asMap().get("reports");
        assertEquals(1, reports.size());

        assertEquals(true, model.asMap().get("isSearched"));

        verify(rptStatusService, times(1)).search(form);
        verify(rptStatusService, times(1)).findAllReports();
        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #4 search 正常系
    // =====================================================================

    @Test
    @DisplayName("#4 search 正常系 検索結果が0件の場合")
    void search_検索結果0件() {
        Reports r1 = new Reports();
        r1.setRptId("R001");

        when(rptStatusService.search(any())).thenReturn(List.of());
        when(rptStatusService.findAllReports()).thenReturn(List.of(r1));

        RptStatusSearchForm form = new RptStatusSearchForm();
        form.setShiteiNo("S001");
        form.setName("山田");
        form.setNameMatchType("partial");
        form.setShisetsuName("ホテル");
        form.setShisetsuNameMatchType("partial");

        Model model = new ExtendedModelMap();
        String result = controller.search(form, model);

        assertEquals(VIEW, result);

        @SuppressWarnings("unchecked")
        List<?> items = (List<?>) model.asMap().get("items");
        assertNotNull(items);
        assertTrue(items.isEmpty());

        assertEquals(true, model.asMap().get("isSearched"));

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #5 search 正常系
    // =====================================================================

    @Test
    @DisplayName("#5 search 正常系 フォームの入力値が加工されずサービスに渡る")
    void search_フォームの入力値が加工されずサービスに渡る() {
        when(rptStatusService.search(any())).thenReturn(List.of());
        when(rptStatusService.findAllReports()).thenReturn(List.of());

        RptStatusSearchForm form = new RptStatusSearchForm();
        form.setShiteiNo("S001");
        form.setName("山田");
        form.setNameMatchType("exact");
        form.setShisetsuName("ホテル");
        form.setShisetsuNameMatchType("prefix");
        form.setKojinNo("123456789012");
        form.setHojinNo("1234567890123");

        ArgumentCaptor<RptStatusSearchForm> captor = ArgumentCaptor.forClass(RptStatusSearchForm.class);

        Model model = new ExtendedModelMap();
        controller.search(form, model);

        verify(rptStatusService).search(captor.capture());
        RptStatusSearchForm captured = captor.getValue();
        assertEquals("S001", captured.getShiteiNo());
        assertEquals("山田", captured.getName());
        assertEquals("exact", captured.getNameMatchType());
        assertEquals("ホテル", captured.getShisetsuName());
        assertEquals("prefix", captured.getShisetsuNameMatchType());
        assertEquals("123456789012", captured.getKojinNo());
        assertEquals("1234567890123", captured.getHojinNo());

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #6 search 異常系
    // =====================================================================

    @Test
    @DisplayName("#6 search 異常系 検索条件がすべて未入力の場合")
    void search_検索条件がすべて未入力() {
        when(rptStatusService.search(any())).thenReturn(List.of());
        when(rptStatusService.findAllReports()).thenReturn(List.of());

        RptStatusSearchForm form = new RptStatusSearchForm();
        // nameMatchType / shisetsuNameMatchType は既定値 "partial"

        Model model = new ExtendedModelMap();
        String result = controller.search(form, model);

        assertEquals(VIEW, result);

        @SuppressWarnings("unchecked")
        List<?> items = (List<?>) model.asMap().get("items");
        assertNotNull(items);
        assertTrue(items.isEmpty());

        assertEquals(true, model.asMap().get("isSearched"));

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #7 search 異常系
    // =====================================================================

    @Test
    @DisplayName("#7 search 異常系 サービスが例外をスローした場合")
    void search_サービスが例外をスロー() {
        when(rptStatusService.search(any())).thenThrow(new RuntimeException("DB error"));

        RptStatusSearchForm form = new RptStatusSearchForm();
        form.setShiteiNo("S001");
        form.setName("山田");
        form.setNameMatchType("partial");
        form.setShisetsuName("ホテル");
        form.setShisetsuNameMatchType("partial");

        Model model = new ExtendedModelMap();

        assertThrows(RuntimeException.class, () -> controller.search(form, model));

        verify(rptStatusService, never()).findAllReports();
        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #29 RptStatusSearchForm 既定値
    // =====================================================================

    @Test
    @DisplayName("#29 既定値 一致区分の既定値が \"partial\" であること")
    void searchForm_一致区分の既定値がpartial() {
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

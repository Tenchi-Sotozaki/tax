package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

/**
 * 特別徴収義務者状況照会（ACCOMMODATION_TAX-361）の Controller 単体テスト。
 *
 * サービスをモックに差し替え、初期表示と検索でモデルに載る内容を検証する。
 */
@ExtendWith(MockitoExtension.class)
class RptStatusControllerTest {

    @Mock RptStatusService rptStatusService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks RptStatusController controller;

    private static final String VIEW = "tokugimu/tTokugimuReportsStatus";

    private Reports reports() {
        Reports r = new Reports();
        r.setRptId("RPT0000001");
        return r;
    }

    private RptStatusListItem item() {
        RptStatusListItem i = new RptStatusListItem();
        i.setShiteiNo("00100001");
        return i;
    }

    // ===================================================================
    // init — 初期表示
    // ===================================================================

    @Test
    void init_検索フォームと帳票一覧が載り明細は空になる() {
        when(rptStatusService.findAllReports()).thenReturn(List.of(reports()));
        Model model = new ExtendedModelMap();

        String view = controller.init(model);

        assertThat(view).isEqualTo(VIEW);
        assertThat(model.asMap().get("searchForm")).isInstanceOf(RptStatusSearchForm.class);
        assertThat((List<?>) model.asMap().get("reports")).hasSize(1);
        assertThat((List<?>) model.asMap().get("items")).isEmpty();
    }

    /** 初期表示では検索を走らせない */
    @Test
    void init_検索は実行しない() {
        when(rptStatusService.findAllReports()).thenReturn(List.of());

        controller.init(new ExtendedModelMap());

        verify(rptStatusService, never()).search(any());
    }

    @Test
    void init_参照権限を確認する() {
        when(rptStatusService.findAllReports()).thenReturn(List.of());

        controller.init(new ExtendedModelMap());

        verify(accessChecker).checkAccess(ScreenManagement.TOKUGIMU_STATUS_VIEW);
    }

    // ===================================================================
    // search — 検索
    // ===================================================================

    @Test
    void search_検索結果と入力したフォームがモデルに載る() {
        RptStatusSearchForm form = new RptStatusSearchForm();
        form.setShiteiNo("00100001");
        when(rptStatusService.search(form)).thenReturn(List.of(item()));
        when(rptStatusService.findAllReports()).thenReturn(List.of(reports()));
        Model model = new ExtendedModelMap();

        String view = controller.search(form, model);

        assertThat(view).isEqualTo(VIEW);
        assertThat(model.asMap().get("searchForm")).isSameAs(form);
        assertThat((List<?>) model.asMap().get("items")).hasSize(1);
        assertThat((List<?>) model.asMap().get("reports")).hasSize(1);
    }

    @Test
    void search_該当が無ければ明細は空になる() {
        RptStatusSearchForm form = new RptStatusSearchForm();
        when(rptStatusService.search(form)).thenReturn(List.of());
        when(rptStatusService.findAllReports()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        controller.search(form, model);

        assertThat((List<?>) model.asMap().get("items")).isEmpty();
    }

    @Test
    void search_参照権限を確認する() {
        RptStatusSearchForm form = new RptStatusSearchForm();
        when(rptStatusService.search(form)).thenReturn(List.of());
        when(rptStatusService.findAllReports()).thenReturn(List.of());

        controller.search(form, new ExtendedModelMap());

        verify(accessChecker).checkAccess(ScreenManagement.TOKUGIMU_STATUS_VIEW);
    }
}

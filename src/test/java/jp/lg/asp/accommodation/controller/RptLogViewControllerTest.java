package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.RptLogViewDto;
import jp.lg.asp.accommodation.service.RptLogViewService;

@ExtendWith(MockitoExtension.class)
class RptLogViewControllerTest {

    @Mock RptLogViewService rptLogViewService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks RptLogViewController controller;

    @Test
    void init_初期表示() {
        when(rptLogViewService.findAllReports()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.init(model);

        assertThat(view).isEqualTo("log/rptLogView");
        assertThat(model.asMap()).containsKeys("form", "reports", "items");
    }

    @Test
    void search_検索結果を返す() {
        RptLogViewDto form = new RptLogViewDto();
        BindingResult result = mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(false);
        when(rptLogViewService.findAllReports()).thenReturn(List.of());
        when(rptLogViewService.search(form)).thenReturn(List.of(new RptLogViewDto()));
        Model model = new ExtendedModelMap();

        String view = controller.search(form, result, model);

        assertThat(view).isEqualTo("log/rptLogView");
        assertThat(((List<?>) model.asMap().get("items"))).hasSize(1);
    }

    @Test
    void search_バリデーションエラー時は空リストを返す() {
        RptLogViewDto form = new RptLogViewDto();
        BindingResult result = mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(true);
        when(rptLogViewService.findAllReports()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.search(form, result, model);

        assertThat(view).isEqualTo("log/rptLogView");
        assertThat(((List<?>) model.asMap().get("items"))).isEmpty();
    }
}

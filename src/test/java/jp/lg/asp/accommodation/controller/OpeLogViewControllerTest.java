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

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.OpeLogViewDto;
import jp.lg.asp.accommodation.service.OpeLogViewService;

@ExtendWith(MockitoExtension.class)
class OpeLogViewControllerTest {

    @Mock OpeLogViewService opeLogViewService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks OpeLogViewController controller;

    @Test
    void init_初期表示() {
        when(opeLogViewService.findAllScreens()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.init(model);

        assertThat(view).isEqualTo("log/opeLogView");
        assertThat(model.asMap()).containsKeys("form", "screens", "items");
    }

    @Test
    void search_検索結果を返す() {
        OpeLogViewDto form = new OpeLogViewDto();
        when(opeLogViewService.findAllScreens()).thenReturn(List.of());
        when(opeLogViewService.search(form)).thenReturn(List.of(new OpeLogViewDto()));
        Model model = new ExtendedModelMap();

        String view = controller.search(form, model);

        assertThat(view).isEqualTo("log/opeLogView");
        assertThat(((List<?>) model.asMap().get("items"))).hasSize(1);
    }
}

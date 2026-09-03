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
import jp.lg.asp.accommodation.entity.Screen;
import jp.lg.asp.accommodation.service.OpeLogViewService;

@ExtendWith(MockitoExtension.class)
class OpeLogViewControllerTest {

    @Mock OpeLogViewService opeLogViewService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks OpeLogViewController controller;

    // ===== No.1: init 正常系 - 正常に初期表示される =====
    @Test
    void init_正常に初期表示される() {
        Screen screen = new Screen();
        when(opeLogViewService.findAllScreens()).thenReturn(List.of(screen));
        Model model = new ExtendedModelMap();

        String view = controller.init(model);

        assertThat(view).isEqualTo("log/opeLogView");
        assertThat(model.asMap().get("form")).isInstanceOf(OpeLogViewDto.class);
        assertThat((List<?>) model.asMap().get("screens")).hasSize(1);
        assertThat((List<?>) model.asMap().get("items")).isEmpty();
    }

    // ===== No.2: init 正常系 - 画面マスタが0件 → 初期表示される =====
    @Test
    void init_画面マスタが0件_初期表示される() {
        when(opeLogViewService.findAllScreens()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.init(model);

        assertThat(view).isEqualTo("log/opeLogView");
        assertThat((List<?>) model.asMap().get("screens")).isEmpty();
        assertThat((List<?>) model.asMap().get("items")).isEmpty();
    }

    // ===== No.3: init 異常系 - accessCheckerで例外 → 例外がスローされる =====
    @Test
    void init_accessCheckerで例外_例外がスローされる() {
        doThrow(new RuntimeException("アクセス拒否")).when(accessChecker).checkAccess(any());

        assertThatThrownBy(() -> controller.init(new ExtendedModelMap()))
                .isInstanceOf(RuntimeException.class);
    }

    // ===== No.4: search 正常系 - 検索条件あり・結果あり → 一覧が返る =====
    @Test
    void search_検索条件あり_結果あり_一覧が返る() {
        Screen screen = new Screen();
        OpeLogViewDto dto = new OpeLogViewDto();
        when(opeLogViewService.findAllScreens()).thenReturn(List.of(screen));
        when(opeLogViewService.search(any())).thenReturn(List.of(dto));

        OpeLogViewDto form = new OpeLogViewDto();
        form.setScreenId("S001");
        form.setSousa("検索");
        form.setOpeUser("user01");
        form.setOpeDtFrom("2024-01-01");
        form.setOpeDtTo("2024-12-31");
        Model model = new ExtendedModelMap();

        String view = controller.search(form, model);

        assertThat(view).isEqualTo("log/opeLogView");
        assertThat((List<?>) model.asMap().get("screens")).hasSize(1);
        assertThat((List<?>) model.asMap().get("items")).hasSize(1);
    }

    // ===== No.5: search 正常系 - 検索条件がすべてnull → 全件検索 =====
    @Test
    void search_検索条件がすべてnull_全件検索() {
        OpeLogViewDto dto1 = new OpeLogViewDto();
        OpeLogViewDto dto2 = new OpeLogViewDto();
        when(opeLogViewService.findAllScreens()).thenReturn(List.of(new Screen()));
        when(opeLogViewService.search(any())).thenReturn(List.of(dto1, dto2));

        OpeLogViewDto form = new OpeLogViewDto(); // 全フィールドnull
        Model model = new ExtendedModelMap();

        String view = controller.search(form, model);

        assertThat(view).isEqualTo("log/opeLogView");
        assertThat((List<?>) model.asMap().get("items")).hasSize(2);
    }

    // ===== No.6: search 正常系 - 検索結果が0件 → itemsが空リスト =====
    @Test
    void search_検索結果が0件_itemsが空リスト() {
        when(opeLogViewService.findAllScreens()).thenReturn(List.of(new Screen()));
        when(opeLogViewService.search(any())).thenReturn(List.of());

        OpeLogViewDto form = new OpeLogViewDto();
        form.setScreenId("S999");
        Model model = new ExtendedModelMap();

        String view = controller.search(form, model);

        assertThat(view).isEqualTo("log/opeLogView");
        assertThat((List<?>) model.asMap().get("items")).isEmpty();
    }

    // ===== No.7: search 異常系 - accessCheckerで例外 → 例外がスローされる =====
    @Test
    void search_accessCheckerで例外_例外がスローされる() {
        doThrow(new RuntimeException("アクセス拒否")).when(accessChecker).checkAccess(any());

        assertThatThrownBy(() -> controller.search(new OpeLogViewDto(), new ExtendedModelMap()))
                .isInstanceOf(RuntimeException.class);
    }
}

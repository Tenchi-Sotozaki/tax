package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.HolidayConfigForm;
import jp.lg.asp.accommodation.service.HolidayConfigService;

@ExtendWith(MockitoExtension.class)
class HolidayConfigControllerTest {

    @Mock HolidayConfigService holidayConfigService;
    @Mock ScreenAccessChecker accessChecker;
    @InjectMocks HolidayConfigController controller;

    private static final String SCREEN_ID = jp.lg.asp.accommodation.config.ScreenManagement.HOLIDAY_CONFIG;

    // ── index ─────────────────────────────────────────────────────

    @Test
    void index_年リストがあれば最後の年へリダイレクト() {
        when(holidayConfigService.findNendoList()).thenReturn(List.of("2024", "2025", "2026"));

        String view = controller.index(new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/admin/holiday/view/2026");
        verify(accessChecker).checkAccess(SCREEN_ID);
    }

    @Test
    void index_年リストが空なら今年へリダイレクト() {
        when(holidayConfigService.findNendoList()).thenReturn(List.of());

        String view = controller.index(new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/admin/holiday/view/" + LocalDate.now().getYear());
        verify(accessChecker).checkAccess(SCREEN_ID);
    }

    // ── view ──────────────────────────────────────────────────────

    @Test
    void view_フォームと年リストがモデルに設定されmode_viewになる() {
        HolidayConfigForm form = new HolidayConfigForm();
        when(holidayConfigService.findByNendo("2026")).thenReturn(form);
        when(holidayConfigService.findNendoList()).thenReturn(List.of("2026"));
        Model model = new ExtendedModelMap();

        String view = controller.view("2026", model);

        assertThat(view).isEqualTo("admin/holidayConfig");
        assertThat(model.asMap().get("mode")).isEqualTo("view");
        assertThat(model.asMap().get("form")).isNotNull();
        assertThat(model.asMap().get("nenList")).isEqualTo(List.of("2026"));
        verify(accessChecker).checkAccess(SCREEN_ID);
    }

    // ── edit ──────────────────────────────────────────────────────

    @Test
    void edit_フォームと年リストがモデルに設定されmode_editになる() {
        HolidayConfigForm form = new HolidayConfigForm();
        when(holidayConfigService.findByNendo("2026")).thenReturn(form);
        when(holidayConfigService.findNendoList()).thenReturn(List.of("2026"));
        Model model = new ExtendedModelMap();

        String view = controller.edit("2026", model);

        assertThat(view).isEqualTo("admin/holidayConfig");
        assertThat(model.asMap().get("mode")).isEqualTo("edit");
        assertThat(model.asMap().get("form")).isNotNull();
        assertThat(model.asMap().get("nenList")).isEqualTo(List.of("2026"));
        verify(accessChecker).checkWriteAccess(SCREEN_ID);
    }

    // ── getInitialHolidays ────────────────────────────────────────

    @Test
    void getInitialHolidays_サービスの戻り値をそのまま返しaccessCheckerは呼ばれない() {
        when(holidayConfigService.getInitialHolidays("2026"))
                .thenReturn(List.of("20260101", "20260102"));

        List<String> result = controller.getInitialHolidays("2026");

        assertThat(result).containsExactly("20260101", "20260102");
        verifyNoInteractions(accessChecker);
    }

    // ── save ──────────────────────────────────────────────────────

    @Test
    void save_正常保存でリダイレクト() {
        HolidayConfigForm form = new HolidayConfigForm();
        form.setNendo("2026");
        form.setHolidayDts(List.of("20260101"));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.save(form, new ExtendedModelMap(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/holiday/view/2026");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("successMessage");
        verify(accessChecker).checkWriteAccess(SCREEN_ID);
    }

    @Test
    void save_nendoがnullの場合はエラーメッセージ付きで編集画面を返す() {
        HolidayConfigForm form = new HolidayConfigForm();
        form.setNendo(null);
        Model model = new ExtendedModelMap();

        String view = controller.save(form, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/holidayConfig");
        assertThat(model.asMap().get("mode")).isEqualTo("edit");
        assertThat(model.asMap().get("errorMessage").toString()).contains("年は必須です。");
        verify(holidayConfigService, never()).save(any());
        verify(accessChecker).checkWriteAccess(SCREEN_ID);
    }

    @Test
    void save_nendoが空白のみの場合はエラーメッセージ付きで編集画面を返す() {
        HolidayConfigForm form = new HolidayConfigForm();
        form.setNendo(" ");
        Model model = new ExtendedModelMap();

        String view = controller.save(form, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/holidayConfig");
        assertThat(model.asMap().get("errorMessage").toString()).contains("年は必須です。");
        verify(holidayConfigService, never()).save(any());
        verify(accessChecker).checkWriteAccess(SCREEN_ID);
    }

    @Test
    void save_サービスが例外をスローした場合はエラーメッセージ付きで編集画面を返す() {
        HolidayConfigForm form = new HolidayConfigForm();
        form.setNendo("2026");
        doThrow(new RuntimeException("DB接続エラー")).when(holidayConfigService).save(any());
        Model model = new ExtendedModelMap();

        String view = controller.save(form, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/holidayConfig");
        assertThat(model.asMap().get("mode")).isEqualTo("edit");
        String errorMessage = model.asMap().get("errorMessage").toString();
        assertThat(errorMessage).contains("保存に失敗しました").contains("DB接続エラー");
        assertThat(model.asMap().get("form")).isNotNull();
        verify(accessChecker).checkWriteAccess(SCREEN_ID);
    }
}

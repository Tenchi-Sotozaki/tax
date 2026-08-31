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
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.HolidayConfigForm;
import jp.lg.asp.accommodation.service.HolidayConfigService;

@ExtendWith(MockitoExtension.class)
class HolidayConfigControllerTest {

    @Mock HolidayConfigService holidayConfigService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks HolidayConfigController controller;

    // ===== edit =====

    @Test
    void edit_編集画面表示() {
        HolidayConfigForm form = new HolidayConfigForm();
        form.setNendo("2026");
        when(holidayConfigService.findByNendo("2026")).thenReturn(form);
        when(holidayConfigService.findNendoList()).thenReturn(List.of("2026"));
        Model model = new ExtendedModelMap();

        String view = controller.edit("2026", model);

        assertThat(view).isEqualTo("admin/holidayConfig");
        assertThat(model.asMap().get("form")).isNotNull();
        assertThat(model.asMap().get("nenList")).isEqualTo(List.of("2026"));
        assertThat(model.asMap().get("mode")).isEqualTo("edit");
        verify(accessChecker).checkWriteAccess(anyString());
    }

    // ===== getInitialHolidays =====

    @Test
    void getInitialHolidays_サービスの戻り値をそのまま返しaccessCheckerは呼ばれない() {
        when(holidayConfigService.getInitialHolidays("2026")).thenReturn(List.of("20260101", "20260102"));

        List<String> result = controller.getInitialHolidays("2026");

        assertThat(result).containsExactly("20260101", "20260102");
        verifyNoInteractions(accessChecker);
    }

    // ===== save =====

    @Test
    void save_正常保存() {
        HolidayConfigForm form = new HolidayConfigForm();
        form.setNendo("2026");
        form.setHolidayDts(List.of("20260101"));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.save(form, new ExtendedModelMap(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/holiday/view/2026");
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage")).isEqualTo("休業日設定を更新しました。");
        verify(accessChecker).checkWriteAccess(anyString());
    }

    @Test
    void save_nendoがnullの場合_エラーメッセージ付きで編集画面を返しサービスを呼ばない() {
        HolidayConfigForm form = new HolidayConfigForm();
        form.setNendo(null);
        Model model = new ExtendedModelMap();

        String view = controller.save(form, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/holidayConfig");
        assertThat(model.asMap().get("mode")).isEqualTo("edit");
        assertThat(model.asMap().get("errorMessage").toString()).contains("年は必須です。");
        verify(holidayConfigService, never()).save(any());
        verify(accessChecker).checkWriteAccess(anyString());
    }

    @Test
    void save_nendoが空白のみの場合_エラーメッセージ付きで編集画面を返しサービスを呼ばない() {
        HolidayConfigForm form = new HolidayConfigForm();
        form.setNendo(" ");
        Model model = new ExtendedModelMap();

        String view = controller.save(form, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/holidayConfig");
        assertThat(model.asMap().get("errorMessage").toString()).contains("年は必須です。");
        verify(holidayConfigService, never()).save(any());
        verify(accessChecker).checkWriteAccess(anyString());
    }

    @Test
    void save_サービスが例外をスローした場合_エラーメッセージ付きで編集画面を返す() {
        HolidayConfigForm form = new HolidayConfigForm();
        form.setNendo("2026");
        doThrow(new RuntimeException("DB接続エラー")).when(holidayConfigService).save(any());
        Model model = new ExtendedModelMap();

        String view = controller.save(form, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/holidayConfig");
        assertThat(model.asMap().get("mode")).isEqualTo("edit");
        assertThat(model.asMap().get("errorMessage").toString()).contains("保存に失敗しました").contains("DB接続エラー");
        assertThat(model.asMap().get("form")).isNotNull();
        verify(accessChecker).checkWriteAccess(anyString());
    }
}

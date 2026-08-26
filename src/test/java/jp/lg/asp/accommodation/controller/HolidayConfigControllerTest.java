package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.HolidayConfigForm;
import jp.lg.asp.accommodation.service.HolidayConfigService;

/**
 * 休業日設定 照会/編集（ACCOMMODATION_TAX-380 / 388）の Controller 単体テスト。
 *
 * サービスをモックに差し替え、画面表示の分岐と保存時の入力チェックを検証する。
 */
@ExtendWith(MockitoExtension.class)
class HolidayConfigControllerTest {

    @Mock HolidayConfigService holidayConfigService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks HolidayConfigController controller;

    private static final String VIEW = "admin/holidayConfig";
    private static final String NEN = "2026";

    private HolidayConfigForm form(String nendo) {
        HolidayConfigForm f = new HolidayConfigForm();
        f.setNendo(nendo);
        f.setHolidayDts(List.of("20260101"));
        return f;
    }

    private RedirectAttributes redirectAttributes() {
        return new RedirectAttributesModelMap();
    }

    // ===================================================================
    // index — 初期遷移
    // ===================================================================

    @Test
    void index_年の一覧があれば最後の年へリダイレクトする() {
        when(holidayConfigService.findNendoList()).thenReturn(List.of("2024", "2025", "2026"));

        String view = controller.index(redirectAttributes());

        assertThat(view).isEqualTo("redirect:/admin/holiday/view/2026");
        verify(accessChecker).checkAccess(ScreenManagement.HOLIDAY_CONFIG);
    }

    /**
     * 期待値が本体と同じ式になっている。
     * 厳密にやるなら本体へ Clock を注入する必要があるため、
     * ここでは年をまたぐ瞬間だけ落ちうることを承知で現状の形にしている。
     */
    @Test
    void index_年の一覧が空なら今年へリダイレクトする() {
        when(holidayConfigService.findNendoList()).thenReturn(List.of());

        String view = controller.index(redirectAttributes());

        assertThat(view).isEqualTo("redirect:/admin/holiday/view/" + LocalDate.now().getYear());
        verify(accessChecker).checkAccess(ScreenManagement.HOLIDAY_CONFIG);
    }

    // ===================================================================
    // view / edit — 画面表示
    // ===================================================================

    @Test
    void view_フォームと年の一覧が載り照会モードになる() {
        when(holidayConfigService.findByNendo(NEN)).thenReturn(form(NEN));
        when(holidayConfigService.findNendoList()).thenReturn(List.of(NEN));
        Model model = new ExtendedModelMap();

        String view = controller.view(NEN, model);

        assertThat(view).isEqualTo(VIEW);
        assertThat(model.asMap()).containsEntry("mode", "view");
        assertThat(model.asMap().get("form")).isNotNull();
        assertThat(model.asMap()).containsEntry("nenList", List.of(NEN));
    }

    @Test
    void edit_編集モードになり書き込み権限を確認する() {
        when(holidayConfigService.findByNendo(NEN)).thenReturn(form(NEN));
        when(holidayConfigService.findNendoList()).thenReturn(List.of(NEN));
        Model model = new ExtendedModelMap();

        String view = controller.edit(NEN, model);

        assertThat(view).isEqualTo(VIEW);
        assertThat(model.asMap()).containsEntry("mode", "edit");
        verify(accessChecker).checkWriteAccess(ScreenManagement.HOLIDAY_CONFIG);
        verify(accessChecker, never()).checkAccess(any());
    }

    @Test
    void view_参照権限を確認する() {
        when(holidayConfigService.findByNendo(NEN)).thenReturn(form(NEN));
        when(holidayConfigService.findNendoList()).thenReturn(List.of(NEN));

        controller.view(NEN, new ExtendedModelMap());

        verify(accessChecker).checkAccess(ScreenManagement.HOLIDAY_CONFIG);
        verify(accessChecker, never()).checkWriteAccess(any());
    }

    // ===================================================================
    // getInitialHolidays — 初期化ボタン（JSON）
    // ===================================================================

    /**
     * 本体の getInitialHolidays は accessChecker を呼んでいない。
     * 他の4エンドポイントは全て権限チェックを通しているため、
     * 実装漏れの可能性がある。ここでは現状の挙動をそのまま固定している。
     */
    @Test
    void getInitialHolidays_サービスの戻り値をそのまま返す() {
        when(holidayConfigService.getInitialHolidays(NEN)).thenReturn(List.of("20260101", "20260102"));

        assertThat(controller.getInitialHolidays(NEN)).containsExactly("20260101", "20260102");
        verifyNoInteractions(accessChecker);
    }

    // ===================================================================
    // save — 更新
    // ===================================================================

    @Test
    void save_正常なら年の照会画面へリダイレクトする() {
        RedirectAttributes redirectAttributes = redirectAttributes();

        String view = controller.save(form(NEN), new ExtendedModelMap(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/holiday/view/" + NEN);
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
                .isEqualTo("休業日設定を更新しました。");
        ArgumentCaptor<HolidayConfigForm> captor =
                ArgumentCaptor.forClass(HolidayConfigForm.class);
        verify(holidayConfigService).save(captor.capture());
        assertThat(captor.getValue().getNendo()).isEqualTo(NEN);
        verify(accessChecker).checkWriteAccess(ScreenManagement.HOLIDAY_CONFIG);
    }

    @Test
    void save_年が未入力ならエラーメッセージ付きで画面を返しサービスを呼ばない() {
        Model model = new ExtendedModelMap();

        String view = controller.save(form(null), model, redirectAttributes());

        assertThat(view).isEqualTo(VIEW);
        assertThat(model.asMap()).containsEntry("mode", "edit");
        assertThat(model.asMap().get("errorMessage").toString()).contains("年は必須です。");
        verify(holidayConfigService, never()).save(any());
        verify(accessChecker).checkWriteAccess(ScreenManagement.HOLIDAY_CONFIG);
    }

    @Test
    void save_年が空白だけでも未入力として扱う() {
        Model model = new ExtendedModelMap();

        String view = controller.save(form("   "), model, redirectAttributes());

        assertThat(view).isEqualTo(VIEW);
        assertThat(model.asMap().get("errorMessage").toString()).contains("年は必須です。");
        verify(holidayConfigService, never()).save(any());
        verify(accessChecker).checkWriteAccess(ScreenManagement.HOLIDAY_CONFIG);
    }

    @Test
    void save_サービスが例外を投げてもエラーメッセージ付きで画面を返す() {
        doThrow(new RuntimeException("DB接続エラー")).when(holidayConfigService).save(any());
        Model model = new ExtendedModelMap();

        String view = controller.save(form(NEN), model, redirectAttributes());

        assertThat(view).isEqualTo(VIEW);
        assertThat(model.asMap()).containsEntry("mode", "edit");
        assertThat(model.asMap().get("errorMessage").toString())
                .contains("保存に失敗しました").contains("DB接続エラー");
        assertThat(model.asMap().get("form")).isNotNull();
        verify(accessChecker).checkWriteAccess(ScreenManagement.HOLIDAY_CONFIG);
    }
}

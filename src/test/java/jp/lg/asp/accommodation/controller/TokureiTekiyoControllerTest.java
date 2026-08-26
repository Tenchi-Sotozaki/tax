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

import jakarta.servlet.http.HttpSession;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.TokureiTekiyoForm;
import jp.lg.asp.accommodation.dto.TokureiTekiyoHistoryDto;
import jp.lg.asp.accommodation.service.TokureiTekiyoService;

@ExtendWith(MockitoExtension.class)
class TokureiTekiyoControllerTest {

    @Mock TokureiTekiyoService tokureiTekiyoService;
    @Mock ScreenAccessChecker accessChecker;
    @Mock HttpSession session;

    @InjectMocks TokureiTekiyoController controller;

    private static final String SHITEI_NO = "00100001";

    @Test
    void list_一覧画面を返す() {
        when(tokureiTekiyoService.getHistories(SHITEI_NO)).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.list(SHITEI_NO, model);

        assertThat(view).isEqualTo("tokugimu/tTokureiTekiyoList");
        assertThat(model.asMap()).containsKey("histories");
    }

    @Test
    void view_照会画面を返す() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        when(tokureiTekiyoService.getForView(SHITEI_NO, 1)).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.view(SHITEI_NO, 1, model);

        assertThat(view).isEqualTo("tokugimu/tTokureiTekiyoConfig");
        assertThat(model.asMap()).containsEntry("isView", true);
        assertThat(model.asMap()).containsEntry("isEdit", false);
    }

    @Test
    void registerForm_登録画面を返す() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        when(tokureiTekiyoService.getForRegister(SHITEI_NO)).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.registerForm(SHITEI_NO, model);

        assertThat(view).isEqualTo("tokugimu/tTokureiTekiyoConfig");
        assertThat(model.asMap()).containsEntry("isView", false);
        assertThat(model.asMap()).containsEntry("isEdit", false);
    }

    @Test
    void register_正常登録後リダイレクト() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        Model model = new ExtendedModelMap();

        String view = controller.register(SHITEI_NO, form, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/tekiyo-nozei-shuki/list/" + SHITEI_NO);
        verify(tokureiTekiyoService).save(SHITEI_NO, form);
    }

    @Test
    void register_例外時はエラー表示() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        doThrow(new RuntimeException("重複エラー")).when(tokureiTekiyoService).save(any(), any());
        Model model = new ExtendedModelMap();

        String view = controller.register(SHITEI_NO, form, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("tokugimu/tTokureiTekiyoConfig");
        assertThat(model.asMap()).containsKey("errorMessage");
    }

    @Test
    void editForm_編集画面を返す() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        when(tokureiTekiyoService.getForView(SHITEI_NO, 1)).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.editForm(SHITEI_NO, 1, model);

        assertThat(view).isEqualTo("tokugimu/tTokureiTekiyoConfig");
        assertThat(model.asMap()).containsEntry("isView", false);
        assertThat(model.asMap()).containsEntry("isEdit", true);
    }

    @Test
    void edit_正常更新後リダイレクト() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        Model model = new ExtendedModelMap();

        String view = controller.edit(SHITEI_NO, 1, form, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/tekiyo-nozei-shuki/list/" + SHITEI_NO);
        verify(tokureiTekiyoService).update(SHITEI_NO, 1, form);
    }

    @Test
    void delete_削除後リダイレクト() {
        String view = controller.delete(SHITEI_NO, 1, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/tekiyo-nozei-shuki/list/" + SHITEI_NO);
        verify(tokureiTekiyoService).delete(SHITEI_NO, 1);
    }
}

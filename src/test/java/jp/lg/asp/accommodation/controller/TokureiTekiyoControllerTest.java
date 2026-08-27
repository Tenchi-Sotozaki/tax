package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
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
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.service.TokureiTekiyoService;
import jp.lg.asp.accommodation.util.SessionHelper;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class TokureiTekiyoControllerTest {

    @Mock TokureiTekiyoService tokureiTekiyoService;
    @Mock ScreenAccessChecker accessChecker;
    @Mock HttpSession session;

    @InjectMocks TokureiTekiyoController controller;

    private static final String SHITEI_NO = "00100001";

    @BeforeEach
    void setUp() {
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setShiteiNo(SHITEI_NO);
        when(session.getAttribute(SessionHelper.SHITEI_GASSAN_KEY)).thenReturn(dto);
    }

    @Test
    void listFromMenu_セッションあり_リダイレクト() {
        String view = controller.listFromMenu(session, new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/tekiyo-nozei-shuki/list");
    }

    @Test
    void listFromMenu_セッションなし_モーダル表示() {
        when(session.getAttribute(SessionHelper.SHITEI_GASSAN_KEY)).thenReturn(null);
        Model model = new ExtendedModelMap();

        String view = controller.listFromMenu(session, model);

        assertThat(view).isEqualTo("tokugimu/tTokureiTekiyoList");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
    }

    @Test
    void list_セッションあり_一覧画面を返す() {
        when(tokureiTekiyoService.getHistories()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.list(session, model);

        assertThat(view).isEqualTo("tokugimu/tTokureiTekiyoList");
        assertThat(model.asMap()).containsKey("histories");
    }

    @Test
    void list_セッションなし_モーダル表示() {
        when(session.getAttribute(SessionHelper.SHITEI_GASSAN_KEY)).thenReturn(null);
        Model model = new ExtendedModelMap();

        String view = controller.list(session, model);

        assertThat(view).isEqualTo("tokugimu/tTokureiTekiyoList");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
    }

    @Test
    void view_照会画面を返す() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        when(tokureiTekiyoService.getForView(1)).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.view(1, model);

        assertThat(view).isEqualTo("tokugimu/tTokureiTekiyoConfig");
        assertThat(model.asMap()).containsEntry("isView", true);
        assertThat(model.asMap()).containsEntry("isEdit", false);
    }

    @Test
    void registerForm_登録画面を返す() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        when(tokureiTekiyoService.getForRegister()).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.registerForm(model);

        assertThat(view).isEqualTo("tokugimu/tTokureiTekiyoConfig");
        assertThat(model.asMap()).containsEntry("isView", false);
        assertThat(model.asMap()).containsEntry("isEdit", false);
    }

    @Test
    void register_正常登録後リダイレクト() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        Model model = new ExtendedModelMap();

        String view = controller.register(form, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/tekiyo-nozei-shuki/list");
        verify(tokureiTekiyoService).save(form);
    }

    @Test
    void register_例外時はエラー表示() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        doThrow(new RuntimeException("重複エラー")).when(tokureiTekiyoService).save(any());
        Model model = new ExtendedModelMap();

        String view = controller.register(form, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("tokugimu/tTokureiTekiyoConfig");
        assertThat(model.asMap()).containsKey("errorMessage");
    }

    @Test
    void editForm_編集画面を返す() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        when(tokureiTekiyoService.getForView(1)).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.editForm(1, model);

        assertThat(view).isEqualTo("tokugimu/tTokureiTekiyoConfig");
        assertThat(model.asMap()).containsEntry("isView", false);
        assertThat(model.asMap()).containsEntry("isEdit", true);
    }

    @Test
    void edit_正常更新後リダイレクト() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        Model model = new ExtendedModelMap();

        String view = controller.edit(1, form, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/tekiyo-nozei-shuki/list");
        verify(tokureiTekiyoService).update(1, form);
    }

    @Test
    void delete_削除後リダイレクト() {
        String view = controller.delete(1, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/tekiyo-nozei-shuki/list");
        verify(tokureiTekiyoService).delete(1);
    }
}

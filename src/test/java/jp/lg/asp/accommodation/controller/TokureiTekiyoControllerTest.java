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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jakarta.servlet.http.HttpSession;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.dto.TokureiTekiyoForm;
import jp.lg.asp.accommodation.dto.TokureiTekiyoHistoryDto;
import jp.lg.asp.accommodation.exception.AccessDeniedException;
import jp.lg.asp.accommodation.service.TokureiTekiyoService;
import jp.lg.asp.accommodation.util.SessionHelper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TokureiTekiyoControllerTest {

    @Mock TokureiTekiyoService tokureiTekiyoService;
    @Mock ScreenAccessChecker accessChecker;
    @Mock HttpSession session;

    @InjectMocks TokureiTekiyoController controller;

    private static final String SHITEI_NO = "00100001";
    private static final String GASSAN_NO = "G0000001";
    private static final String LIST_VIEW = "tokugimu/tTokureiTekiyoList";
    private static final String FORM_VIEW = "tokugimu/tTokureiTekiyoConfig";

    @BeforeEach
    void setUp() {
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setShiteiNo(SHITEI_NO);
        when(session.getAttribute(SessionHelper.SHITEI_GASSAN_KEY)).thenReturn(dto);
    }

    // ========== listFromMenu ==========

    @Test
    void listFromMenu_セッションに指定番号あり_checkAccessが呼ばれリダイレクト() {
        String view = controller.listFromMenu(session, new ExtendedModelMap());

        verify(accessChecker).checkAccess(any());
        assertThat(view).isEqualTo("redirect:/tokurei-tekiyo/list");
    }

    @Test
    void listFromMenu_セッションに指定番号なし合算指定番号あり_モーダル表示() {
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setGassanShiteiNo(GASSAN_NO);
        when(session.getAttribute(SessionHelper.SHITEI_GASSAN_KEY)).thenReturn(dto);
        Model model = new ExtendedModelMap();

        String view = controller.listFromMenu(session, model);

        verify(accessChecker).checkAccess(any());
        assertThat(view).isEqualTo(LIST_VIEW);
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
        assertThat((List<?>) model.asMap().get("histories")).isEmpty();
    }

    @Test
    void listFromMenu_セッションに指定番号なし合算指定番号なし_モーダル表示() {
        when(session.getAttribute(SessionHelper.SHITEI_GASSAN_KEY)).thenReturn(null);
        Model model = new ExtendedModelMap();

        String view = controller.listFromMenu(session, model);

        verify(accessChecker).checkAccess(any());
        assertThat(view).isEqualTo(LIST_VIEW);
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
        assertThat((List<?>) model.asMap().get("histories")).isEmpty();
    }

    // ========== list ==========

    @Test
    void list_セッションに指定番号あり_getHistoriesが呼ばれ一覧画面を返す() {
        List<TokureiTekiyoHistoryDto> histories = List.of(new TokureiTekiyoHistoryDto(1, "2024年04月", "2024年09月"));
        when(tokureiTekiyoService.getHistories()).thenReturn(histories);
        Model model = new ExtendedModelMap();

        String view = controller.list(session, model);

        verify(accessChecker).checkAccess(any());
        verify(tokureiTekiyoService).getHistories();
        assertThat(view).isEqualTo(LIST_VIEW);
        assertThat(model.asMap().get("histories")).isEqualTo(histories);
    }

    @Test
    void list_セッションに指定番号なし合算指定番号あり_モーダル表示() {
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setGassanShiteiNo(GASSAN_NO);
        when(session.getAttribute(SessionHelper.SHITEI_GASSAN_KEY)).thenReturn(dto);
        Model model = new ExtendedModelMap();

        String view = controller.list(session, model);

        verify(accessChecker).checkAccess(any());
        assertThat(view).isEqualTo(LIST_VIEW);
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
        assertThat((List<?>) model.asMap().get("histories")).isEmpty();
    }

    @Test
    void list_セッションに指定番号なし合算指定番号なし_モーダル表示() {
        when(session.getAttribute(SessionHelper.SHITEI_GASSAN_KEY)).thenReturn(null);
        Model model = new ExtendedModelMap();

        String view = controller.list(session, model);

        verify(accessChecker).checkAccess(any());
        assertThat(view).isEqualTo(LIST_VIEW);
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
        assertThat((List<?>) model.asMap().get("histories")).isEmpty();
    }

    @Test
    void list_指定番号あり_getHistoriesが空リストを返す() {
        when(tokureiTekiyoService.getHistories()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.list(session, model);

        assertThat(view).isEqualTo(LIST_VIEW);
        assertThat((List<?>) model.asMap().get("histories")).isEmpty();
    }

    // ========== view ==========

    @Test
    void view_指定rnoのデータが存在する_照会画面を返す() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        when(tokureiTekiyoService.getForView(1)).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.view(1, model);

        verify(accessChecker).checkAccess(any());
        verify(tokureiTekiyoService).getForView(1);
        assertThat(view).isEqualTo(FORM_VIEW);
        assertThat(model.asMap().get("tokureiTekiyoForm")).isEqualTo(form);
        assertThat(model.asMap()).containsEntry("isView", true);
        assertThat(model.asMap()).containsEntry("isEdit", false);
    }

    @Test
    void view_指定rnoのデータが存在しない_例外がスローされFORM_VIEWが返却されない() {
        when(tokureiTekiyoService.getForView(99)).thenThrow(new IllegalStateException("指定されたレコードが見つかりません。"));

        verify(accessChecker, never()).checkAccess(any());
        assertThatThrownBy(() -> controller.view(99, new ExtendedModelMap()))
                .isInstanceOf(IllegalStateException.class);
    }

    // ========== registerForm ==========

    @Test
    void registerForm_登録画面を表示() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        when(tokureiTekiyoService.getForRegister()).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.registerForm(model);

        verify(accessChecker).checkWriteAccess(any());
        verify(tokureiTekiyoService).getForRegister();
        assertThat(view).isEqualTo(FORM_VIEW);
        assertThat(model.asMap().get("tokureiTekiyoForm")).isEqualTo(form);
        assertThat(model.asMap()).containsEntry("isView", false);
        assertThat(model.asMap()).containsEntry("isEdit", false);
    }

    @Test
    void registerForm_書き込み権限なし_AccessDeniedExceptionがスロー() {
        doThrow(new AccessDeniedException("SCREEN", "user")).when(accessChecker).checkWriteAccess(any());

        assertThatThrownBy(() -> controller.registerForm(new ExtendedModelMap()))
                .isInstanceOf(AccessDeniedException.class);
        verify(tokureiTekiyoService, never()).getForRegister();
    }

    // ========== register ==========

    @Test
    void register_正常登録_successMessageがフラッシュ属性に設定されリダイレクト() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.register(form, new ExtendedModelMap(), redirectAttributes);

        verify(accessChecker).checkWriteAccess(any());
        verify(tokureiTekiyoService).save(form);
        assertThat(view).isEqualTo("redirect:/tokurei-tekiyo/list");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("successMessage");
    }

    @Test
    void register_saveで例外_errorMessageがモデルに設定されFORM_VIEWを返す() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        doThrow(new IllegalStateException("重複エラー")).when(tokureiTekiyoService).save(any());
        Model model = new ExtendedModelMap();

        String view = controller.register(form, model, new RedirectAttributesModelMap());

        verify(accessChecker).checkWriteAccess(any());
        assertThat(view).isEqualTo(FORM_VIEW);
        assertThat(model.asMap()).containsKey("errorMessage");
        assertThat(model.asMap()).containsEntry("isView", false);
        assertThat(model.asMap()).containsEntry("isEdit", false);
    }

    @Test
    void register_書き込み権限なし_AccessDeniedExceptionがスローされリダイレクトされない() {
        doThrow(new AccessDeniedException("SCREEN", "user")).when(accessChecker).checkWriteAccess(any());

        assertThatThrownBy(() -> controller.register(new TokureiTekiyoForm(), new ExtendedModelMap(), new RedirectAttributesModelMap()))
                .isInstanceOf(AccessDeniedException.class);
        verify(tokureiTekiyoService, never()).save(any());
    }

    // ========== editForm ==========

    @Test
    void editForm_指定rnoのデータが存在する_編集画面を返す() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        when(tokureiTekiyoService.getForView(1)).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.editForm(1, model);

        verify(accessChecker).checkWriteAccess(any());
        verify(tokureiTekiyoService).getForView(1);
        assertThat(view).isEqualTo(FORM_VIEW);
        assertThat(model.asMap().get("tokureiTekiyoForm")).isEqualTo(form);
        assertThat(model.asMap()).containsEntry("isView", false);
        assertThat(model.asMap()).containsEntry("isEdit", true);
    }

    @Test
    void editForm_指定rnoのデータが存在しない_例外がスローされFORM_VIEWが返却されない() {
        when(tokureiTekiyoService.getForView(99)).thenThrow(new IllegalStateException("指定されたレコードが見つかりません。"));

        assertThatThrownBy(() -> controller.editForm(99, new ExtendedModelMap()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void editForm_書き込み権限なし_AccessDeniedExceptionがスロー() {
        doThrow(new AccessDeniedException("SCREEN", "user")).when(accessChecker).checkWriteAccess(any());

        assertThatThrownBy(() -> controller.editForm(1, new ExtendedModelMap()))
                .isInstanceOf(AccessDeniedException.class);
        verify(tokureiTekiyoService, never()).getForView(any());
    }

    // ========== edit ==========

    @Test
    void edit_正常更新_successMessageがフラッシュ属性に設定されリダイレクト() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.edit(1, form, new ExtendedModelMap(), redirectAttributes);

        verify(accessChecker).checkWriteAccess(any());
        verify(tokureiTekiyoService).update(1, form);
        assertThat(view).isEqualTo("redirect:/tokurei-tekiyo/list");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("successMessage");
    }

    @Test
    void edit_updateで例外_errorMessageがモデルに設定されFORM_VIEWを返す() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        doThrow(new IllegalStateException("重複エラー")).when(tokureiTekiyoService).update(anyInt(), any());
        Model model = new ExtendedModelMap();

        String view = controller.edit(1, form, model, new RedirectAttributesModelMap());

        verify(accessChecker).checkWriteAccess(any());
        assertThat(view).isEqualTo(FORM_VIEW);
        assertThat(model.asMap()).containsKey("errorMessage");
        assertThat(model.asMap()).containsEntry("isView", false);
        assertThat(model.asMap()).containsEntry("isEdit", true);
    }

    @Test
    void edit_書き込み権限なし_AccessDeniedExceptionがスローされリダイレクトされない() {
        doThrow(new AccessDeniedException("SCREEN", "user")).when(accessChecker).checkWriteAccess(any());

        assertThatThrownBy(() -> controller.edit(1, new TokureiTekiyoForm(), new ExtendedModelMap(), new RedirectAttributesModelMap()))
                .isInstanceOf(AccessDeniedException.class);
        verify(tokureiTekiyoService, never()).update(anyInt(), any());
    }

    // ========== delete ==========

    @Test
    void delete_正常削除_successMessageがフラッシュ属性に設定されリダイレクト() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.delete(1, redirectAttributes);

        verify(accessChecker).checkWriteAccess(any());
        verify(tokureiTekiyoService).delete(1);
        assertThat(view).isEqualTo("redirect:/tokurei-tekiyo/list");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("successMessage");
    }

    @Test
    void delete_deleteで例外_errorMessageがフラッシュ属性に設定されリダイレクト() {
        doThrow(new IllegalStateException("削除エラー")).when(tokureiTekiyoService).delete(anyInt());
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.delete(1, redirectAttributes);

        verify(accessChecker).checkWriteAccess(any());
        assertThat(view).isEqualTo("redirect:/tokurei-tekiyo/list");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("errorMessage");
    }

    @Test
    void delete_書き込み権限なし_AccessDeniedExceptionがスローされリダイレクトされない() {
        doThrow(new AccessDeniedException("SCREEN", "user")).when(accessChecker).checkWriteAccess(any());

        assertThatThrownBy(() -> controller.delete(1, new RedirectAttributesModelMap()))
                .isInstanceOf(AccessDeniedException.class);
        verify(tokureiTekiyoService, never()).delete(anyInt());
    }
}

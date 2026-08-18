package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jakarta.servlet.http.HttpSession;

import java.util.Map;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.dto.TaxManagerForm;
import jp.lg.asp.accommodation.service.TaxManagerService;
import jp.lg.asp.accommodation.util.SessionHelper;

@ExtendWith(MockitoExtension.class)
class TaxManagerControllerTest {

    @Mock TaxManagerService taxManagerService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks TaxManagerController controller;

    private HttpSession sessionWith(String shiteiNo) {
        HttpSession session = mock(HttpSession.class);
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setShiteiNo(shiteiNo);
        when(session.getAttribute(SessionHelper.SHITEI_GASSAN_KEY)).thenReturn(dto);
        return session;
    }

    private HttpSession emptySession() {
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(SessionHelper.SHITEI_GASSAN_KEY)).thenReturn(null);
        return session;
    }

    // -----------------------------------------------------------------------
    // checkAtenaDuplicate
    // -----------------------------------------------------------------------

    @Test
    void checkAtenaDuplicate_同一人物() {
        when(taxManagerService.isSamePerson("1001", "1001")).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = controller.checkAtenaDuplicate("1001", "1001");

        assertThat(response.getBody()).containsEntry("isDuplicate", true);
    }

    @Test
    void checkAtenaDuplicate_別人物() {
        when(taxManagerService.isSamePerson("1001", "1002")).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = controller.checkAtenaDuplicate("1001", "1002");

        assertThat(response.getBody()).containsEntry("isDuplicate", false);
    }

    // -----------------------------------------------------------------------
    // register
    // -----------------------------------------------------------------------

    @Test
    void register_セッションなし_モーダルフラグを返す() {
        Model model = new ExtendedModelMap();

        String view = controller.register(model, emptySession(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("tokugimu/tTaxManagerConfig");
        assertThat(model.asMap()).containsEntry("showShiteiModal", true);
        assertThat(model.asMap()).containsEntry("isEdit", false);
        assertThat(model.asMap()).containsEntry("isView", false);
    }

    @Test
    void register_未登録_登録画面を返す() {
        TaxManagerForm form = new TaxManagerForm();
        form.setEdit(false);
        when(taxManagerService.getByShiteiNo("00100001")).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.register(model, sessionWith("00100001"), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("tokugimu/tTaxManagerConfig");
        assertThat(model.asMap()).containsEntry("isEdit", false);
        assertThat(model.asMap()).containsEntry("isView", false);
    }

    @Test
    void register_登録済み_照会画面にリダイレクト() {
        TaxManagerForm form = new TaxManagerForm();
        form.setEdit(true);
        when(taxManagerService.getByShiteiNo("00100001")).thenReturn(form);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.register(new ExtendedModelMap(), sessionWith("00100001"), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/tax-manager/view");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("infoMessage");
    }

    // -----------------------------------------------------------------------
    // edit
    // -----------------------------------------------------------------------

    @Test
    void edit_セッションなし_モーダルフラグを返す() {
        Model model = new ExtendedModelMap();

        String view = controller.edit(model, emptySession(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("tokugimu/tTaxManagerConfig");
        assertThat(model.asMap()).containsEntry("showShiteiModal", true);
        assertThat(model.asMap()).containsEntry("isEdit", false);
        assertThat(model.asMap()).containsEntry("isView", false);
    }

    @Test
    void edit_登録済み_編集画面を返す() {
        TaxManagerForm form = new TaxManagerForm();
        form.setEdit(true);
        when(taxManagerService.getByShiteiNo("00100001")).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.edit(model, sessionWith("00100001"), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("tokugimu/tTaxManagerConfig");
        assertThat(model.asMap()).containsEntry("isEdit", true);
        assertThat(model.asMap()).containsEntry("isView", false);
    }

    @Test
    void edit_未登録_登録画面にリダイレクト() {
        TaxManagerForm form = new TaxManagerForm();
        form.setEdit(false);
        when(taxManagerService.getByShiteiNo("00100001")).thenReturn(form);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.edit(new ExtendedModelMap(), sessionWith("00100001"), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/tax-manager/register");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("infoMessage");
    }

    // -----------------------------------------------------------------------
    // view
    // -----------------------------------------------------------------------

    @Test
    void view_セッションなし_モーダルフラグを返す() {
        Model model = new ExtendedModelMap();

        String view = controller.view(null, null, model, emptySession(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("tokugimu/tTaxManagerConfig");
        assertThat(model.asMap()).containsEntry("showShiteiModal", true);
    }

    @Test
    void view_登録済み_照会画面を返す() {
        TaxManagerForm form = new TaxManagerForm();
        form.setEdit(true);
        when(taxManagerService.getByShiteiNo("00100001")).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.view(null, null, model, sessionWith("00100001"), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("tokugimu/tTaxManagerConfig");
        assertThat(model.asMap()).containsEntry("isView", true);
    }

    @Test
    void view_未登録_登録画面にリダイレクト() {
        TaxManagerForm form = new TaxManagerForm();
        form.setEdit(false);
        when(taxManagerService.getByShiteiNo("00100001")).thenReturn(form);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.view(null, null, new ExtendedModelMap(), sessionWith("00100001"), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/tax-manager/register");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("infoMessage");
    }

    // -----------------------------------------------------------------------
    // save
    // -----------------------------------------------------------------------

    @Test
    void save_セッションなし_モーダルフラグを返す() {
        TaxManagerForm form = new TaxManagerForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "taxManagerForm");
        Model model = new ExtendedModelMap();

        String view = controller.save(form, bindingResult, model, new RedirectAttributesModelMap(), emptySession());

        assertThat(view).isEqualTo("tokugimu/tTaxManagerConfig");
        assertThat(model.asMap()).containsEntry("showShiteiModal", true);
    }

    @Test
    void save_バリデーションエラー() {
        TaxManagerForm form = new TaxManagerForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "taxManagerForm");
        bindingResult.rejectValue("atenaNo", "NotBlank", "必須です");
        Model model = new ExtendedModelMap();

        String view = controller.save(form, bindingResult, model, new RedirectAttributesModelMap(), sessionWith("00100001"));

        assertThat(view).isEqualTo("tokugimu/tTaxManagerConfig");
    }

    @Test
    void save_正常保存() {
        TaxManagerForm form = new TaxManagerForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "taxManagerForm");
        Model model = new ExtendedModelMap();

        String view = controller.save(form, bindingResult, model, new RedirectAttributesModelMap(), sessionWith("00100001"));

        assertThat(view).isEqualTo("redirect:/tokugimu/list");
        verify(taxManagerService).saveByShiteiNo("00100001", form);
    }

    // -----------------------------------------------------------------------
    // delete
    // -----------------------------------------------------------------------

    @Test
    void delete_セッションなし_editにリダイレクト() {
        String view = controller.delete(new RedirectAttributesModelMap(), emptySession());

        assertThat(view).isEqualTo("redirect:/tax-manager/edit");
        verify(taxManagerService, never()).deleteByShiteiNo(any());
    }

    @Test
    void delete_正常削除() {
        String view = controller.delete(new RedirectAttributesModelMap(), sessionWith("00100001"));

        assertThat(view).isEqualTo("redirect:/tokugimu/list");
        verify(taxManagerService).deleteByShiteiNo("00100001");
    }
}

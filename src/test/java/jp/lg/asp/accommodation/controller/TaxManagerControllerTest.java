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

    private HttpSession gassanOnlySession(String gassanShiteiNo) {
        HttpSession session = mock(HttpSession.class);
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setShiteiNo(null);
        dto.setGassanShiteiNo(gassanShiteiNo);
        when(session.getAttribute(SessionHelper.SHITEI_GASSAN_KEY)).thenReturn(dto);
        return session;
    }

    // -----------------------------------------------------------------------
    // No.1 checkAtenaDuplicate - 同一人物
    // -----------------------------------------------------------------------

    @Test
    void checkAtenaDuplicate_同一人物_isDuplicateTrue_警告メッセージを返す() {
        when(taxManagerService.isSamePerson("A001", "A001")).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = controller.checkAtenaDuplicate("A001", "A001");

        assertThat(response.getBody()).containsEntry("isDuplicate", true);
        assertThat(response.getBody()).containsEntry("message", "特別徴収義務者と同一人物のため、納税管理人として登録できません。");
    }

    // -----------------------------------------------------------------------
    // No.2 checkAtenaDuplicate - 別人
    // -----------------------------------------------------------------------

    @Test
    void checkAtenaDuplicate_別人_isDuplicateFalse_登録可能メッセージを返す() {
        when(taxManagerService.isSamePerson("A001", "B001")).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = controller.checkAtenaDuplicate("A001", "B001");

        assertThat(response.getBody()).containsEntry("isDuplicate", false);
        assertThat(response.getBody()).containsEntry("message", "登録可能です。");
    }

    // -----------------------------------------------------------------------
    // No.3 checkAtenaDuplicate - trim
    // -----------------------------------------------------------------------

    @Test
    void checkAtenaDuplicate_前後スペースをtrimして判定する() {
        when(taxManagerService.isSamePerson("A001", "B001")).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = controller.checkAtenaDuplicate(" A001 ", " B001 ");

        assertThat(response.getBody()).containsEntry("isDuplicate", false);
    }

    // -----------------------------------------------------------------------
    // No.4 checkAtenaDuplicate - 例外
    // -----------------------------------------------------------------------

    @Test
    void checkAtenaDuplicate_サービスが例外をスロー_isDuplicateFalse_エラーメッセージを返す() {
        when(taxManagerService.isSamePerson(any(), any())).thenThrow(new RuntimeException("エラー"));

        ResponseEntity<Map<String, Object>> response = controller.checkAtenaDuplicate("A001", "B001");

        assertThat(response.getBody()).containsEntry("isDuplicate", false);
        assertThat(response.getBody()).containsEntry("message", "チェック中にエラーが発生しました。");
    }

    // -----------------------------------------------------------------------
    // No.5 register - セッションなし
    // -----------------------------------------------------------------------

    @Test
    void register_セッションなし_モーダル表示_登録画面を返す() {
        Model model = new ExtendedModelMap();

        String view = controller.register(model, emptySession(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("tokugimu/tTaxManagerConfig");
        assertThat(model.asMap()).containsEntry("showShiteiModal", true);
        assertThat(model.asMap()).containsEntry("isEdit", false);
        assertThat(model.asMap()).containsEntry("isView", false);
    }

    // -----------------------------------------------------------------------
    // No.6 register - 合算指定番号のみ
    // -----------------------------------------------------------------------

    @Test
    void register_合算指定番号のみ_showShiteiModalTrue_登録画面を返す() {
        Model model = new ExtendedModelMap();

        String view = controller.register(model, gassanOnlySession("901001"), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("tokugimu/tTaxManagerConfig");
        assertThat(model.asMap()).containsEntry("showShiteiModal", true);
        assertThat(model.asMap()).containsEntry("isEdit", false);
        assertThat(model.asMap()).containsEntry("isView", false);
    }

    // -----------------------------------------------------------------------
    // No.7 register - 未登録
    // -----------------------------------------------------------------------

    @Test
    void register_未登録_登録画面を返す() {
        TaxManagerForm form = new TaxManagerForm();
        form.setEdit(false);
        when(taxManagerService.getByShiteiNo("S001")).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.register(model, sessionWith("S001"), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("tokugimu/tTaxManagerConfig");
        assertThat(model.asMap()).containsEntry("isEdit", false);
        assertThat(model.asMap()).containsEntry("isView", false);
    }

    // -----------------------------------------------------------------------
    // No.8 register - 登録済み
    // -----------------------------------------------------------------------

    @Test
    void register_登録済み_照会画面にリダイレクト_infoMessageあり() {
        TaxManagerForm form = new TaxManagerForm();
        form.setEdit(true);
        when(taxManagerService.getByShiteiNo("S001")).thenReturn(form);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.register(new ExtendedModelMap(), sessionWith("S001"), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/tax-manager/view");
        assertThat((String) redirectAttributes.getFlashAttributes().get("infoMessage")).isEqualTo("納税管理人が登録済みのため、照会画面に遷移しました。");
    }

    // -----------------------------------------------------------------------
    // No.9 edit - セッションなし
    // -----------------------------------------------------------------------

    @Test
    void edit_セッションなし_モーダル表示_登録画面を返す() {
        Model model = new ExtendedModelMap();

        String view = controller.edit(model, emptySession(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("tokugimu/tTaxManagerConfig");
        assertThat(model.asMap()).containsEntry("showShiteiModal", true);
        assertThat(model.asMap()).containsEntry("isEdit", false);
        assertThat(model.asMap()).containsEntry("isView", false);
    }

    // -----------------------------------------------------------------------
    // No.10 edit - 登録済み
    // -----------------------------------------------------------------------

    @Test
    void edit_登録済み_編集画面を返す() {
        TaxManagerForm form = new TaxManagerForm();
        form.setEdit(true);
        when(taxManagerService.getByShiteiNo("S001")).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.edit(model, sessionWith("S001"), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("tokugimu/tTaxManagerConfig");
        assertThat(model.asMap()).containsEntry("isEdit", true);
        assertThat(model.asMap()).containsEntry("isView", false);
    }

    // -----------------------------------------------------------------------
    // No.11 edit - 未登録
    // -----------------------------------------------------------------------

    @Test
    void edit_未登録_登録画面にリダイレクト_infoMessageあり() {
        TaxManagerForm form = new TaxManagerForm();
        form.setEdit(false);
        when(taxManagerService.getByShiteiNo("S001")).thenReturn(form);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.edit(new ExtendedModelMap(), sessionWith("S001"), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/tax-manager/register");
        assertThat((String) redirectAttributes.getFlashAttributes().get("infoMessage")).isEqualTo("納税管理人が未登録のため、登録画面に遷移しました。");
    }

    // -----------------------------------------------------------------------
    // No.12 view - セッションなし
    // -----------------------------------------------------------------------

    @Test
    void view_セッションなし_モーダル表示_照会画面を返す() {
        Model model = new ExtendedModelMap();

        String view = controller.view(null, null, model, emptySession(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("tokugimu/tTaxManagerConfig");
        assertThat(model.asMap()).containsEntry("showShiteiModal", true);
        assertThat(model.asMap()).containsEntry("isEdit", false);
        assertThat(model.asMap()).containsEntry("isView", true);
    }

    // -----------------------------------------------------------------------
    // No.13 view - 合算指定番号のみ
    // -----------------------------------------------------------------------

    @Test
    void view_合算指定番号のみ_showShiteiModalTrue_照会画面を返す() {
        Model model = new ExtendedModelMap();

        String view = controller.view(null, null, model, gassanOnlySession("901001"), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("tokugimu/tTaxManagerConfig");
        assertThat(model.asMap()).containsEntry("showShiteiModal", true);
        assertThat(model.asMap()).containsEntry("isEdit", false);
        assertThat(model.asMap()).containsEntry("isView", true);
    }

    // -----------------------------------------------------------------------
    // No.14 view - rno未指定・登録済み
    // -----------------------------------------------------------------------

    @Test
    void view_rno未指定_登録済み_getByShiteiNoを呼び照会画面を返す() {
        TaxManagerForm form = new TaxManagerForm();
        form.setEdit(true);
        when(taxManagerService.getByShiteiNo("S001")).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.view(null, null, model, sessionWith("S001"), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("tokugimu/tTaxManagerConfig");
        assertThat(model.asMap()).containsEntry("isView", true);
        verify(taxManagerService).getByShiteiNo("S001");
        verify(taxManagerService, never()).getByShiteiNoAndRno(any(), any());
    }

    // -----------------------------------------------------------------------
    // No.15 view - 未登録
    // -----------------------------------------------------------------------

    @Test
    void view_未登録_登録画面にリダイレクト_infoMessageあり() {
        TaxManagerForm form = new TaxManagerForm();
        form.setEdit(false);
        when(taxManagerService.getByShiteiNo("S001")).thenReturn(form);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.view(null, null, new ExtendedModelMap(), sessionWith("S001"), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/tax-manager/register");
        assertThat((String) redirectAttributes.getFlashAttributes().get("infoMessage")).isEqualTo("納税管理人が未登録のため、登録画面に遷移しました。");
    }

    // -----------------------------------------------------------------------
    // No.16 save - セッションなし
    // -----------------------------------------------------------------------

    @Test
    void save_セッションなし_エラーあり_登録画面にリダイレクト() {
        TaxManagerForm form = new TaxManagerForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "taxManagerForm");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.save(form, bindingResult, new ExtendedModelMap(), redirectAttributes, emptySession());

        assertThat(view).isEqualTo("redirect:/tax-manager/register");
        assertThat((String) redirectAttributes.getFlashAttributes().get("errorMessage")).isEqualTo("指定番号がセッションに存在しません。");
    }

    // -----------------------------------------------------------------------
    // No.17 save - バリデーションエラー
    // -----------------------------------------------------------------------

    @Test
    void save_バリデーションエラー_登録画面を返す_validationErrorsあり() {
        TaxManagerForm form = new TaxManagerForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "taxManagerForm");
        bindingResult.rejectValue("atenaNo", "NotBlank", "必須です");
        Model model = new ExtendedModelMap();

        String view = controller.save(form, bindingResult, model, new RedirectAttributesModelMap(), sessionWith("S001"));

        assertThat(view).isEqualTo("tokugimu/tTaxManagerConfig");
        assertThat(model.asMap()).containsKey("validationErrors");
    }

    // -----------------------------------------------------------------------
    // No.18 save - 正常保存
    // -----------------------------------------------------------------------

    @Test
    void save_正常保存_照会画面にリダイレクト_successMessageあり() {
        TaxManagerForm form = new TaxManagerForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "taxManagerForm");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.save(form, bindingResult, new ExtendedModelMap(), redirectAttributes, sessionWith("S001"));

        assertThat(view).isEqualTo("redirect:/tax-manager/view");
        assertThat((String) redirectAttributes.getFlashAttributes().get("successMessage")).isEqualTo("納税管理人情報を保存しました。");
        verify(taxManagerService).saveByShiteiNo("S001", form);
    }

    // -----------------------------------------------------------------------
    // No.19 save - サービス例外
    // -----------------------------------------------------------------------

    @Test
    void save_サービスが例外をスロー_登録画面を返す_errorMessageあり() {
        TaxManagerForm form = new TaxManagerForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "taxManagerForm");
        doThrow(new RuntimeException("保存エラー")).when(taxManagerService).saveByShiteiNo(any(), any());
        Model model = new ExtendedModelMap();

        String view = controller.save(form, bindingResult, model, new RedirectAttributesModelMap(), sessionWith("S001"));

        assertThat(view).isEqualTo("tokugimu/tTaxManagerConfig");
        assertThat(model.asMap()).containsEntry("errorMessage", "保存エラー");
    }

    // -----------------------------------------------------------------------
    // No.20 delete - セッションなし
    // -----------------------------------------------------------------------

    @Test
    void delete_セッションなし_エラーあり_editにリダイレクト() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.delete(redirectAttributes, emptySession());

        assertThat(view).isEqualTo("redirect:/tax-manager/edit");
        assertThat((String) redirectAttributes.getFlashAttributes().get("errorMessage")).isEqualTo("指定番号がセッションに存在しません。");
        verify(taxManagerService, never()).deleteByShiteiNo(any());
    }

    // -----------------------------------------------------------------------
    // No.21 delete - 前履歴あり
    // -----------------------------------------------------------------------

    @Test
    void delete_前履歴あり_照会画面にリダイレクト_successMessageあり() {
        when(taxManagerService.deleteByShiteiNo("001001")).thenReturn(true);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.delete(redirectAttributes, sessionWith("001001"));

        assertThat(view).isEqualTo("redirect:/tax-manager/view");
        assertThat((String) redirectAttributes.getFlashAttributes().get("successMessage")).isEqualTo("納税管理人を削除しました。");
    }

    // -----------------------------------------------------------------------
    // No.22 delete - 履歴なし
    // -----------------------------------------------------------------------

    @Test
    void delete_履歴なし_管理台帳にリダイレクト_successMessageあり() {
        when(taxManagerService.deleteByShiteiNo("001001")).thenReturn(false);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.delete(redirectAttributes, sessionWith("001001"));

        assertThat(view).isEqualTo("redirect:/tokugimu/list");
        assertThat((String) redirectAttributes.getFlashAttributes().get("successMessage")).isEqualTo("納税管理人を削除しました。");
    }

    // -----------------------------------------------------------------------
    // No.23 delete - サービス例外
    // -----------------------------------------------------------------------

    @Test
    void delete_サービスが例外をスロー_editにリダイレクト_errorMessageあり() {
        doThrow(new RuntimeException("削除エラー")).when(taxManagerService).deleteByShiteiNo("S001");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.delete(redirectAttributes, sessionWith("S001"));

        assertThat(view).isEqualTo("redirect:/tax-manager/edit");
        assertThat((String) redirectAttributes.getFlashAttributes().get("errorMessage")).isEqualTo("削除エラー");
    }
}

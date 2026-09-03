package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.GassanDaichoSearchForm;
import jp.lg.asp.accommodation.dto.GassanForm;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.service.GassanDaichoService;
import jp.lg.asp.accommodation.service.GassanService;
import jp.lg.asp.accommodation.util.SessionHelper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GassanControllerTest {

    @Mock GassanService gassanService;
    @Mock GassanDaichoService gassanDaichoService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks GassanController controller;

    private MockHttpSession sessionWith(String gassanShiteiNo) {
        MockHttpSession session = new MockHttpSession();
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setGassanShiteiNo(gassanShiteiNo);
        dto.setShiteiNo("S001");
        SessionHelper.saveShiteiGassan(session, dto);
        return session;
    }

    //=====================================================
    // showRegistrationForm（登録）
    //=====================================================
    @Test
    void showRegistrationForm_登録画面を返す() {
        Model model = new ExtendedModelMap();
        MockHttpSession session = new MockHttpSession();
        SessionHelper.saveShiteiGassan(session, new ShiteiGassanSearchDto());

        String view = controller.showRegistrationForm(model, session);

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        assertThat(model.asMap()).containsEntry("isEdit", false);
        assertThat(SessionHelper.getGassanShiteiNo(session)).isNull();
    }

    //=====================================================
    // showDaicho（台帳）
    //=====================================================
    @Test
    void showDaicho_一覧画面を返す() {
        when(gassanDaichoService.search(any())).thenReturn(
                new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        Model model = new ExtendedModelMap();

        String view = controller.showDaicho(new GassanDaichoSearchForm(), model);

        assertThat(view).isEqualTo("gassan/tGassanDaicho");
    }

    @Test
    void showDaicho_境界値_0件() {
        when(gassanDaichoService.search(any())).thenReturn(
                new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        Model model = new ExtendedModelMap();

        String view = controller.showDaicho(new GassanDaichoSearchForm(), model);

        assertThat(view).isEqualTo("gassan/tGassanDaicho");
        assertThat(model.asMap()).containsKey("items");
    }

    //=====================================================
    // showViewForm（照会フォーム）
    //=====================================================
    @Test
    void showViewForm_セッションなし_モーダルフラグをセット() {
        Model model = new ExtendedModelMap();

        String view = controller.showViewForm(null, model, new MockHttpSession());

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
    }

    @Test
    void showViewForm_セッションあり_照会フォームを返す() {
        GassanForm form = new GassanForm();
        when(gassanService.getByGassanShiteiNo("G001")).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.showViewForm(null, model, sessionWith("G001"));

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        assertThat(model.asMap()).containsEntry("isView", true);
    }

    @Test
    void showViewForm_セッションあり_rnoあり() {
        GassanForm form = new GassanForm();
        when(gassanService.getByGassanShiteiNoAndRno(eq("G001"), eq(BigDecimal.ONE))).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.showViewForm(BigDecimal.ONE, model, sessionWith("G001"));

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        assertThat(model.asMap()).containsEntry("isView", true);
    }

    @Test
    void showViewForm_異常系_例外発生() {
        when(gassanService.getByGassanShiteiNo("G001")).thenThrow(new RuntimeException("Error"));
        Model model = new ExtendedModelMap();

        String view = controller.showViewForm(null, model, sessionWith("G001"));

        assertThat(view).isEqualTo("redirect:/gassan/list");
        assertThat(model.asMap()).containsEntry("errorMessage", "指定された合算申告情報が見つかりません。");
    }

    //=====================================================
    // select（セッション保存）
    //=====================================================
    @Test
    void select_セッションに保存してリダイレクト() {
        MockHttpSession session = new MockHttpSession();

        String view = controller.select("G001", "S001", "山田太郎", "ホテルABC", session);

        assertThat(view).isEqualTo("redirect:/gassan/view-form");
        assertThat(SessionHelper.getGassanShiteiNo(session)).isEqualTo("G001");
        assertThat(SessionHelper.getShiteiGassan(session).getShiteiNo()).isEqualTo("S001");
        assertThat(SessionHelper.getShiteiGassan(session).getName()).isEqualTo("山田太郎");
        assertThat(SessionHelper.getShiteiGassan(session).getShisetsuName()).isEqualTo("ホテルABC");
    }

    //=====================================================
    // showEditForm（編集画面）
    //=====================================================
    @Test
    void showEditForm_セッションなし_モーダルフラグをセット() {
        Model model = new ExtendedModelMap();

        String view = controller.showEditForm(model, new MockHttpSession());

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
    }

    @Test
    void showEditForm_セッションあり_正常() {
        GassanForm form = new GassanForm();
        form.setTekiyoStYmd(java.time.LocalDate.now().plusMonths(1));
        when(gassanService.getByGassanShiteiNo("G001")).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.showEditForm(model, sessionWith("G001"));

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        assertThat(model.asMap()).containsEntry("isEdit", true);
        assertThat(model.asMap()).containsEntry("tekiyoStYmdEditable", true);
        assertThat(model.asMap()).containsEntry("editable", true);
    }

    @Test
    void showEditForm_セッションあり_適用開始年月過去() {
        GassanForm form = new GassanForm();
        form.setTekiyoStYmd(java.time.LocalDate.now().minusMonths(1));
        when(gassanService.getByGassanShiteiNo("G001")).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.showEditForm(model, sessionWith("G001"));

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        assertThat(model.asMap()).containsEntry("tekiyoStYmdEditable", false);
    }

    @Test
    void showEditForm_適用終了年月過去日_編集不可() {
        GassanForm form = new GassanForm();
        form.setTekiyoStYmd(java.time.LocalDate.now().plusMonths(1));
        form.setTekiyoEdYmd(java.time.LocalDate.now().minusMonths(1));
        when(gassanService.getByGassanShiteiNo("G001")).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.showEditForm(model, sessionWith("G001"));

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        assertThat(model.asMap()).containsEntry("editable", false);
    }

    @Test
    void showEditForm_異常系() {
        when(gassanService.getByGassanShiteiNo("G001")).thenThrow(new RuntimeException("Error"));
        Model model = new ExtendedModelMap();

        String view = controller.showEditForm(model, sessionWith("G001"));

        assertThat(view).isEqualTo("redirect:/gassan/list");
    }

    //=====================================================
    // updateGassan（編集・更新）
    //=====================================================
    @Test
    void updateGassan_セッションなし_リダイレクト() {
        GassanForm form = new GassanForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.updateGassan(form, bindingResult, model, redirectAttributes, new MockHttpSession());

        assertThat(view).isEqualTo("redirect:/gassan/edit");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("errorMessage");
    }

    @Test
    void updateGassan_正常() {
        GassanForm form = new GassanForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.updateGassan(form, bindingResult, model, redirectAttributes, sessionWith("G001"));

        assertThat(view).isEqualTo("redirect:/gassan/list");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("successMessage");
        verify(gassanService).updateByGassanShiteiNo(eq("G001"), eq(form));
    }

    @Test
    void updateGassan_バリデーションエラー() {
        GassanForm form = new GassanForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
        bindingResult.rejectValue("gassanShiteiNo", "error.required");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.updateGassan(form, bindingResult, model, redirectAttributes, sessionWith("G001"));

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        assertThat(model.asMap()).containsEntry("isEdit", true);
        verify(gassanService, never()).updateByGassanShiteiNo(any(), any());
    }

    @Test
    void updateGassan_異常系_サービス例外() {
        GassanForm form = new GassanForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
        doThrow(new RuntimeException("DB Error")).when(gassanService).updateByGassanShiteiNo(any(), any());
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.updateGassan(form, bindingResult, model, redirectAttributes, sessionWith("G001"));

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        assertThat(model.asMap()).containsKey("errorMessage");
    }

    //=====================================================
    // register（登録）
    //=====================================================
    @Test
    void register_正常登録() {
        GassanForm form = new GassanForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
        Model model = new ExtendedModelMap();
        MockHttpSession session = new MockHttpSession();
        when(gassanService.register(form, null)).thenReturn("G001");

        String view = controller.register(form, bindingResult, model, new RedirectAttributesModelMap(), session);

        assertThat(view).isEqualTo("redirect:/gassan/list");
        assertThat(SessionHelper.getGassanShiteiNo(session)).isEqualTo("G001");
        verify(gassanService).register(form, null);
    }

    @Test
    void register_セッションあり_再登録() {
        GassanForm form = new GassanForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
        Model model = new ExtendedModelMap();
        MockHttpSession session = sessionWith("G001");
        when(gassanService.register(form, "G001")).thenReturn("G001");

        String view = controller.register(form, bindingResult, model, new RedirectAttributesModelMap(), session);

        assertThat(view).isEqualTo("redirect:/gassan/list");
        assertThat(SessionHelper.getGassanShiteiNo(session)).isEqualTo("G001");
        verify(gassanService).register(form, "G001");
    }

    @Test
    void register_バリデーションエラー() {
        GassanForm form = new GassanForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
        bindingResult.rejectValue("gassanShiteiNo", "error.required");
        Model model = new ExtendedModelMap();

        String view = controller.register(form, bindingResult, model, new RedirectAttributesModelMap(), new MockHttpSession());

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        assertThat(model.asMap()).containsEntry("isEdit", false);
        verify(gassanService, never()).register(any(), any());
    }

    @Test
    void register_異常系_サービス例外() {
        GassanForm form = new GassanForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
        when(gassanService.register(any(), any())).thenThrow(new RuntimeException("DB Error"));
        Model model = new ExtendedModelMap();

        String view = controller.register(form, bindingResult, model, new RedirectAttributesModelMap(), new MockHttpSession());

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        assertThat(model.asMap()).containsKey("errorMessage");
    }

    //=====================================================
    // getFacilitiesByAtena（施設一覧取得）
    //=====================================================
    @Test
    void getFacilitiesByAtena_施設一覧を返す() {
        when(gassanService.getFacilitiesByAtenaNo(BigDecimal.valueOf(1001))).thenReturn(List.of());

        List<GassanForm.FacilityItem> result = controller.getFacilitiesByAtena(
                Map.of("atenaNo", "1001"));

        assertThat(result).isNotNull();
    }

    //=====================================================
    // delete（削除）
    //=====================================================
    @Test
    void delete_正常() {
        GassanForm form = new GassanForm();
        form.setGassanShiteiNo("G001");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.delete(form, bindingResult, model, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/gassan/list");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("successMessage");
        verify(gassanService).deleteByGassanShiteiNo("G001");
    }

    @Test
    void delete_指定番号なし() {
        GassanForm form = new GassanForm();
        form.setGassanShiteiNo("");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.delete(form, bindingResult, model, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/gassan/list");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("errorMessage");
        verify(gassanService, never()).deleteByGassanShiteiNo(any());
    }

    @Test
    void delete_異常系_サービス例外() {
        GassanForm form = new GassanForm();
        form.setGassanShiteiNo("G001");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
        doThrow(new RuntimeException("DB Error")).when(gassanService).deleteByGassanShiteiNo("G001");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.delete(form, bindingResult, model, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/gassan/edit");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("errorMessage");
    }
}

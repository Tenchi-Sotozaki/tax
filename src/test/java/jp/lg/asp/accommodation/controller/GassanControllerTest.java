package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
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
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.GassanDaichoItem;
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
        SessionHelper.saveShiteiGassan(session, dto);
        return session;
    }

    // =====================================================
    // #1 showRegistrationForm 正常系 登録画面の初期表示
    // =====================================================
    @Test
    @DisplayName("#1 showRegistrationForm 正常系 登録画面の初期表示")
    void 確認1_登録画面の初期表示() {
        Model model = new ExtendedModelMap();
        MockHttpSession session = new MockHttpSession();

        String view = controller.showRegistrationForm(model, session);

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        assertThat(model.asMap().get("GassanForm")).isInstanceOf(GassanForm.class);
        assertThat(model.asMap()).containsEntry("isEdit", false);
        assertThat(model.asMap()).containsEntry("isView", false);
        assertThat(model.asMap()).containsEntry("showAddressModal", true);
        verify(accessChecker, times(1)).checkWriteAccess(ScreenManagement.GASSAN_CONFIG);
    }

    // =====================================================
    // #2 showRegistrationForm 正常系 セッションから合算指定番号キーが削除される
    // =====================================================
    @Test
    @DisplayName("#2 showRegistrationForm 正常系 セッションから合算指定番号キーが削除される")
    void 確認2_セッションから合算指定番号キーが削除される() {
        Model model = new ExtendedModelMap();
        MockHttpSession session = new MockHttpSession();
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setGassanShiteiNo("G00001");
        SessionHelper.saveShiteiGassan(session, dto);

        controller.showRegistrationForm(model, session);

        assertThat(session.getAttribute(SessionHelper.SHITEI_GASSAN_KEY)).isNull();
    }

    // =====================================================
    // #3 showViewForm 正常系 セッションに合算指定番号あり・rnoなし
    // =====================================================
    @Test
    @DisplayName("#3 showViewForm 正常系 セッションに合算指定番号あり・rnoなし")
    void 確認3_セッションに合算指定番号あり_rnoなし() {
        GassanForm form = new GassanForm();
        when(gassanService.getByGassanShiteiNo("G00001")).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.showViewForm(null, model, sessionWith("G00001"));

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        assertThat(model.asMap().get("GassanForm")).isSameAs(form);
        assertThat(model.asMap()).containsEntry("isView", true);
        assertThat(model.asMap()).containsEntry("isEdit", false);
        assertThat(model.asMap()).containsEntry("editId", "G00001");
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.GASSAN_CONFIG);
    }

    // =====================================================
    // #4 showViewForm 正常系 セッションに合算指定番号あり・rnoあり
    // =====================================================
    @Test
    @DisplayName("#4 showViewForm 正常系 セッションに合算指定番号あり・rnoあり")
    void 確認4_セッションに合算指定番号あり_rnoあり() {
        GassanForm form = new GassanForm();
        when(gassanService.getByGassanShiteiNoAndRno("G00001", BigDecimal.ONE)).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.showViewForm(BigDecimal.ONE, model, sessionWith("G00001"));

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        assertThat(model.asMap()).containsEntry("isView", true);
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.GASSAN_CONFIG);
    }

    // =====================================================
    // #5 showViewForm 準正常系 セッションに合算指定番号なし
    // =====================================================
    @Test
    @DisplayName("#5 showViewForm 準正常系 セッションに合算指定番号なし")
    void 確認5_セッションに合算指定番号なし_照会() {
        Model model = new ExtendedModelMap();

        String view = controller.showViewForm(null, model, new MockHttpSession());

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
        assertThat(model.asMap()).containsEntry("isView", true);
        assertThat(model.asMap()).containsEntry("isEdit", false);
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.GASSAN_CONFIG);
    }

    // =====================================================
    // #6 showViewForm 異常系 サービスが例外をスロー
    // =====================================================
    @Test
    @DisplayName("#6 showViewForm 異常系 サービスが例外をスロー")
    void 確認6_照会サービスが例外をスロー() {
        when(gassanService.getByGassanShiteiNo("G00001"))
                .thenThrow(new RuntimeException("not found"));
        Model model = new ExtendedModelMap();

        String view = controller.showViewForm(null, model, sessionWith("G00001"));

        assertThat(view).isEqualTo("redirect:/gassan/list");
        assertThat(model.asMap()).containsEntry("errorMessage", "指定された合算申告情報が見つかりません。");
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.GASSAN_CONFIG);
    }

    // =====================================================
    // #7 select 正常系 合算指定番号をセッションに保存
    // =====================================================
    @Test
    @DisplayName("#7 select 正常系 合算指定番号をセッションに保存")
    void 確認7_合算指定番号をセッションに保存() {
        MockHttpSession session = new MockHttpSession();

        String view = controller.select("G00001", null, "テスト宿泊", "テスト施設", session);

        assertThat(view).isEqualTo("redirect:/gassan/view-form");
        ShiteiGassanSearchDto saved = SessionHelper.getShiteiGassan(session);
        assertThat(saved.getGassanShiteiNo()).isEqualTo("G00001");
        assertThat(saved.getName()).isEqualTo("テスト宿泊");
        assertThat(saved.getShisetsuName()).isEqualTo("テスト施設");
        assertThat(saved.getShiteiNo()).isNull();
    }

    // =====================================================
    // #8 showEditForm 正常系 セッションに合算指定番号あり
    // =====================================================
    @Test
    @DisplayName("#8 showEditForm 正常系 セッションに合算指定番号あり")
    void 確認8_編集画面_セッションに合算指定番号あり() {
        GassanForm form = new GassanForm();
        form.setTekiyoStYmd(LocalDate.now().plusDays(1));
        form.setTekiyoEdYmd(null);
        when(gassanService.getByGassanShiteiNo("G00001")).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.showEditForm(model, sessionWith("G00001"));

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        assertThat(model.asMap()).containsEntry("isEdit", true);
        assertThat(model.asMap()).containsEntry("tekiyoStYmdEditable", true);
        assertThat(model.asMap()).containsEntry("editable", true);
        verify(accessChecker, times(1)).checkWriteAccess(ScreenManagement.GASSAN_CONFIG);
    }

    // =====================================================
    // #9 showEditForm 準正常系 セッションに合算指定番号なし
    // =====================================================
    @Test
    @DisplayName("#9 showEditForm 準正常系 セッションに合算指定番号なし")
    void 確認9_編集画面_セッションに合算指定番号なし() {
        Model model = new ExtendedModelMap();

        String view = controller.showEditForm(model, new MockHttpSession());

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
        assertThat(model.asMap()).containsEntry("isEdit", true);
        assertThat(model.asMap()).containsEntry("isView", false);
        verify(accessChecker, times(1)).checkWriteAccess(ScreenManagement.GASSAN_CONFIG);
    }

    // =====================================================
    // #10 showEditForm 異常系 サービスが例外をスロー
    // =====================================================
    @Test
    @DisplayName("#10 showEditForm 異常系 サービスが例外をスロー")
    void 確認10_編集画面_サービスが例外をスロー() {
        when(gassanService.getByGassanShiteiNo("G00001"))
                .thenThrow(new RuntimeException("not found"));
        Model model = new ExtendedModelMap();

        String view = controller.showEditForm(model, sessionWith("G00001"));

        assertThat(view).isEqualTo("redirect:/gassan/list");
        verify(accessChecker, times(1)).checkWriteAccess(ScreenManagement.GASSAN_CONFIG);
    }

    // =====================================================
    // #11 updateGassan 正常系 バリデーション正常・更新成功
    // =====================================================
    @Test
    @DisplayName("#11 updateGassan 正常系 バリデーション正常・更新成功")
    void 確認11_更新成功() {
        GassanForm form = new GassanForm();
        form.setTekiyoStYmd(LocalDate.now());
        form.setTekiyoEdYmd(LocalDate.now().plusMonths(1));
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.updateGassan(form, bindingResult, model, redirectAttributes, sessionWith("G00001"));

        assertThat(view).isEqualTo("redirect:/gassan/list");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("successMessage");
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
                .isEqualTo("合算申告の更新が完了しました。");
        verify(gassanService, times(1)).updateByGassanShiteiNo("G00001", form);
        verify(accessChecker, times(1)).checkWriteAccess(ScreenManagement.GASSAN_CONFIG);
    }

    // =====================================================
    // #12 updateGassan 準正常系 バリデーションエラーあり
    // =====================================================
    @Test
    @DisplayName("#12 updateGassan 準正常系 バリデーションエラーあり")
    void 確認12_更新バリデーションエラー() {
        GassanForm form = new GassanForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
        bindingResult.rejectValue("tekiyoStYmd", "error.required");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.updateGassan(form, bindingResult, model, redirectAttributes, sessionWith("G00001"));

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        verify(gassanService, times(1)).reloadFacilityList(form);
        assertThat(model.asMap()).containsKey("validationErrors");
        verify(accessChecker, times(1)).checkWriteAccess(ScreenManagement.GASSAN_CONFIG);
    }

    // =====================================================
    // #13 updateGassan 準正常系 セッションに合算指定番号なし
    // =====================================================
    @Test
    @DisplayName("#13 updateGassan 準正常系 セッションに合算指定番号なし")
    void 確認13_更新セッションなし() {
        GassanForm form = new GassanForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.updateGassan(form, bindingResult, model, redirectAttributes, new MockHttpSession());

        assertThat(view).isEqualTo("redirect:/gassan/edit");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("errorMessage");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("合算指定番号が未選択です。");
        verify(accessChecker, times(1)).checkWriteAccess(ScreenManagement.GASSAN_CONFIG);
    }

    // =====================================================
    // #14 updateGassan 異常系 適用開始年月エラーでサービスが例外をスロー
    // =====================================================
    @Test
    @DisplayName("#14 updateGassan 異常系 適用開始年月エラーでサービスが例外をスロー")
    void 確認14_更新適用開始年月エラー() {
        GassanForm form = new GassanForm();
        form.setTekiyoStYmd(LocalDate.now());
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
        doThrow(new RuntimeException("適用開始年月は前履歴の適用終了年月より後の日付を入力してください。"))
                .when(gassanService).updateByGassanShiteiNo(any(), any());
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.updateGassan(form, bindingResult, model, redirectAttributes, sessionWith("G00001"));

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        assertThat(bindingResult.getFieldError("tekiyoStYmd")).isNotNull();
        verify(accessChecker, times(1)).checkWriteAccess(ScreenManagement.GASSAN_CONFIG);
    }

    // =====================================================
    // #15 updateGassan 異常系 適用終了年月エラーでサービスが例外をスロー
    // =====================================================
    @Test
    @DisplayName("#15 updateGassan 異常系 適用終了年月エラーでサービスが例外をスロー")
    void 確認15_更新適用終了年月エラー() {
        GassanForm form = new GassanForm();
        form.setTekiyoStYmd(LocalDate.now());
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
        doThrow(new RuntimeException("適用終了年月は適用開始年月より後の年月を入力してください。"))
                .when(gassanService).updateByGassanShiteiNo(any(), any());
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.updateGassan(form, bindingResult, model, redirectAttributes, sessionWith("G00001"));

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        assertThat(bindingResult.getFieldError("tekiyoEdYmd")).isNotNull();
        verify(accessChecker, times(1)).checkWriteAccess(ScreenManagement.GASSAN_CONFIG);
    }

    // =====================================================
    // #16 getFacilitiesByAtena 正常系 宛名番号で施設一覧取得
    // =====================================================
    @Test
    @DisplayName("#16 getFacilitiesByAtena 正常系 宛名番号で施設一覧取得")
    void 確認16_宛名番号で施設一覧取得() {
        GassanForm.FacilityItem item1 = new GassanForm.FacilityItem("S001", "施設A", "宿泊A", false);
        GassanForm.FacilityItem item2 = new GassanForm.FacilityItem("S002", "施設B", "宿泊B", false);
        when(gassanService.getFacilitiesByAtenaNo(new BigDecimal("1001")))
                .thenReturn(List.of(item1, item2));

        List<GassanForm.FacilityItem> result = controller.getFacilitiesByAtena(Map.of("atenaNo", "1001"));

        assertThat(result).hasSize(2);
        verify(gassanService, times(1)).getFacilitiesByAtenaNo(new BigDecimal("1001"));
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.GASSAN_CONFIG);
    }

    // =====================================================
    // #17 register 正常系 バリデーション正常・新規登録成功
    // =====================================================
    @Test
    @DisplayName("#17 register 正常系 バリデーション正常・新規登録成功")
    void 確認17_新規登録成功() {
        GassanForm form = new GassanForm();
        form.setAtenaNo(BigDecimal.valueOf(1001));
        form.setTorokuYmd(LocalDate.now());
        form.setShinkokuYmd(LocalDate.now());
        form.setTekiyoStYmd(LocalDate.now());
        form.setShiteiNoList(List.of("S001", "S002"));
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        MockHttpSession session = new MockHttpSession();
        when(gassanService.register(form, null)).thenReturn("G00001");

        String view = controller.register(form, bindingResult, model, redirectAttributes, session);

        assertThat(view).isEqualTo("redirect:/gassan/list");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("successMessage");
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
                .isEqualTo("合算申告の登録が完了しました。");
        verify(gassanService, times(1)).register(form, null);
    }

    // =====================================================
    // #18 register 正常系 バリデーション正常・再登録成功（セッションに合算指定番号あり）
    // =====================================================
    @Test
    @DisplayName("#18 register 正常系 バリデーション正常・再登録成功（セッションに合算指定番号あり）")
    void 確認18_再登録成功() {
        GassanForm form = new GassanForm();
        form.setAtenaNo(BigDecimal.valueOf(1001));
        form.setTorokuYmd(LocalDate.now());
        form.setShinkokuYmd(LocalDate.now());
        form.setTekiyoStYmd(LocalDate.now());
        form.setShiteiNoList(List.of("S001", "S002"));
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        MockHttpSession session = sessionWith("G00001");
        when(gassanService.register(form, "G00001")).thenReturn("G00001");

        String view = controller.register(form, bindingResult, model, redirectAttributes, session);

        assertThat(view).isEqualTo("redirect:/gassan/list");
        assertThat(SessionHelper.getGassanShiteiNo(session)).isEqualTo("G00001");
        verify(gassanService, times(1)).register(form, "G00001");
    }

    // =====================================================
    // #19 register 準正常系 バリデーションエラーあり
    // =====================================================
    @Test
    @DisplayName("#19 register 準正常系 バリデーションエラーあり")
    void 確認19_登録バリデーションエラー() {
        GassanForm form = new GassanForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
        bindingResult.rejectValue("atenaNo", "error.required");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.register(form, bindingResult, model, redirectAttributes, new MockHttpSession());

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        verify(gassanService, times(1)).reloadFacilityList(form);
        assertThat(model.asMap()).containsKey("validationErrors");
    }

    // =====================================================
    // #20 register 異常系 適用開始年月エラーでサービスが例外をスロー
    // =====================================================
    @Test
    @DisplayName("#20 register 異常系 適用開始年月エラーでサービスが例外をスロー")
    void 確認20_登録適用開始年月エラー() {
        GassanForm form = new GassanForm();
        form.setAtenaNo(BigDecimal.valueOf(1001));
        form.setTorokuYmd(LocalDate.now());
        form.setShinkokuYmd(LocalDate.now());
        form.setTekiyoStYmd(LocalDate.now());
        form.setShiteiNoList(List.of("S001", "S002"));
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
        when(gassanService.register(any(), any()))
                .thenThrow(new RuntimeException("適用開始年月は前履歴の適用終了年月より後の日付を入力してください。"));
        Model model = new ExtendedModelMap();

        String view = controller.register(form, bindingResult, model, new RedirectAttributesModelMap(), new MockHttpSession());

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        assertThat(bindingResult.getFieldError("tekiyoStYmd")).isNotNull();
    }

    // =====================================================
    // #21 register 異常系 適用終了年月エラーでサービスが例外をスロー
    // =====================================================
    @Test
    @DisplayName("#21 register 異常系 適用終了年月エラーでサービスが例外をスロー")
    void 確認21_登録適用終了年月エラー() {
        GassanForm form = new GassanForm();
        form.setAtenaNo(BigDecimal.valueOf(1001));
        form.setTorokuYmd(LocalDate.now());
        form.setShinkokuYmd(LocalDate.now());
        form.setTekiyoStYmd(LocalDate.now());
        form.setShiteiNoList(List.of("S001", "S002"));
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
        when(gassanService.register(any(), any()))
                .thenThrow(new RuntimeException("適用終了年月は適用開始年月より後の年月を入力してください。"));
        Model model = new ExtendedModelMap();

        String view = controller.register(form, bindingResult, model, new RedirectAttributesModelMap(), new MockHttpSession());

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        assertThat(bindingResult.getFieldError("tekiyoEdYmd")).isNotNull();
    }

    // =====================================================
    // #22 register 異常系 その他の例外をスロー
    // =====================================================
    @Test
    @DisplayName("#22 register 異常系 その他の例外をスロー")
    void 確認22_登録その他例外() {
        GassanForm form = new GassanForm();
        form.setAtenaNo(BigDecimal.valueOf(1001));
        form.setTorokuYmd(LocalDate.now());
        form.setShinkokuYmd(LocalDate.now());
        form.setTekiyoStYmd(LocalDate.now());
        form.setShiteiNoList(List.of("S001", "S002"));
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
        when(gassanService.register(any(), any()))
                .thenThrow(new RuntimeException("代表施設を選択してください。"));
        Model model = new ExtendedModelMap();

        String view = controller.register(form, bindingResult, model, new RedirectAttributesModelMap(), new MockHttpSession());

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        assertThat(model.asMap()).containsEntry("errorMessage", "代表施設を選択してください。");
    }

    // =====================================================
    // #23 delete 正常系 削除成功
    // =====================================================
    @Test
    @DisplayName("#23 delete 正常系 削除成功")
    void 確認23_削除成功() {
        GassanForm form = new GassanForm();
        form.setGassanShiteiNo("G00001");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.delete(form, bindingResult, model, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/gassan/list");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("successMessage");
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
                .isEqualTo("合算申告の削除が完了しました。");
        verify(gassanService, times(1)).deleteByGassanShiteiNo("G00001");
    }

    // =====================================================
    // #24 delete 準正常系 gassanShiteiNoが未指定
    // =====================================================
    @Test
    @DisplayName("#24 delete 準正常系 gassanShiteiNoが未指定")
    void 確認24_削除対象未指定() {
        GassanForm form = new GassanForm();
        form.setGassanShiteiNo(null);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.delete(form, bindingResult, model, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/gassan/list");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("errorMessage");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("削除対象の指定がありません。");
        verify(gassanService, never()).deleteByGassanShiteiNo(any());
    }

    // =====================================================
    // #25 delete 異常系 サービスが例外をスロー
    // =====================================================
    @Test
    @DisplayName("#25 delete 異常系 サービスが例外をスロー")
    void 確認25_削除サービス例外() {
        GassanForm form = new GassanForm();
        form.setGassanShiteiNo("G00001");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
        doThrow(new RuntimeException("削除エラー")).when(gassanService).deleteByGassanShiteiNo("G00001");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.delete(form, bindingResult, model, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/gassan/edit");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("errorMessage");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("削除に失敗しました: 削除エラー");
    }

    // =====================================================
    // showDaicho（台帳）チェックリスト#1より
    // =====================================================
    @Test
    @DisplayName("#1(台帳) showDaicho 正常系 台帳一覧の初期表示")
    void 確認1_台帳一覧の初期表示() {
        GassanDaichoItem item = new GassanDaichoItem();
        item.setGassanShiteiNo("G0000001");
        PageImpl<GassanDaichoItem> page = new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1);
        GassanDaichoSearchForm searchForm = new GassanDaichoSearchForm();
        when(gassanDaichoService.search(searchForm)).thenReturn(page);

        Model model = new ExtendedModelMap();
        String view = controller.showDaicho(searchForm, model);

        assertThat(view).isEqualTo("gassan/tGassanDaicho");
        assertThat(model.asMap().get("items")).isSameAs(page);
        verify(gassanDaichoService, times(1)).search(searchForm);
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.GASSAN_LIST);
    }
}

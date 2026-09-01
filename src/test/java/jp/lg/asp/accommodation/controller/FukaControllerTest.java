package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.FukaDaichoForm;
import jp.lg.asp.accommodation.dto.FukaDeclarationForm;
import jp.lg.asp.accommodation.dto.FukaMonthlyDeclarationDto;
import jp.lg.asp.accommodation.dto.FukaTaxDetailDto;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.entity.Nokigen;
import jp.lg.asp.accommodation.service.FukaService;
import jp.lg.asp.accommodation.service.FukaValidatorService;
import jp.lg.asp.accommodation.service.NokigenService;
import jp.lg.asp.accommodation.util.SessionHelper;

@ExtendWith(MockitoExtension.class)
class FukaControllerTest {

    @Mock FukaService fukaService;
    @Mock ScreenAccessChecker accessChecker;
    @Mock FukaValidatorService fukaValidatorService;
    @Mock NokigenService nokigenService;

    @InjectMocks FukaController controller;

    private MockHttpSession sessionWith(String shiteiNo) {
        MockHttpSession session = new MockHttpSession();
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setShiteiNo(shiteiNo);
        SessionHelper.saveShiteiGassan(session, dto);
        return session;
    }

    private MockHttpSession sessionWithGassan(String shiteiNo, String gassanShiteiNo) {
        MockHttpSession session = new MockHttpSession();
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setShiteiNo(shiteiNo);
        dto.setGassanShiteiNo(gassanShiteiNo);
        SessionHelper.saveShiteiGassan(session, dto);
        return session;
    }

    // ===== showDaicho =====

    // No.1 セッションに指定番号あり・年度指定あり・ステータス指定ありの場合、台帳データが設定されてDaicho画面を返す
    @Test
    void showDaicho_指定番号あり年度指定ありステータス指定あり_台帳画面を返す() {
        MockHttpSession session = sessionWith("00100001");
        FukaDaichoForm form = new FukaDaichoForm();
        when(nokigenService.findAll()).thenReturn(List.of(new Nokigen()));
        when(fukaService.getDaichoData("00100001", "2024", "999")).thenReturn(form);
        when(fukaService.getExistingNendoList("00100001")).thenReturn(List.of(2024));
        when(fukaService.getNendoStMonth()).thenReturn(4);
        Model model = new ExtendedModelMap();

        String view = controller.showDaicho("2024", "999", session, model);

        assertThat(view).isEqualTo("fuka/tFukaDaicho");
        assertThat(model.asMap()).containsKey("fukaDaichoForm");
        assertThat(model.asMap()).doesNotContainKey("errorMessage");
    }

    // No.2 セッションに指定番号あり・年度未指定の場合、今年度をデフォルトとして台帳データが設定されてDaicho画面を返す
    @Test
    void showDaicho_年度未指定_今年度をデフォルトとして台帳画面を返す() {
        MockHttpSession session = sessionWith("00100001");
        FukaDaichoForm form = new FukaDaichoForm();
        when(fukaService.getDaichoData(eq("00100001"), any(), isNull())).thenReturn(form);
        when(fukaService.getExistingNendoList("00100001")).thenReturn(List.of());
        when(fukaService.getNendoStMonth()).thenReturn(4);
        when(nokigenService.findAll()).thenReturn(List.of(new Nokigen()));
        Model model = new ExtendedModelMap();

        String view = controller.showDaicho(null, null, session, model);

        assertThat(view).isEqualTo("fuka/tFukaDaicho");
        assertThat(model.asMap()).containsKey("selectedNendo");
    }

    // No.3 納入期限が未登録の場合、errorMessageが設定されてDaicho画面を返す
    @Test
    void showDaicho_納入期限未登録_errorMessageが設定されてDaicho画面を返す() {
        MockHttpSession session = sessionWith("00100001");
        FukaDaichoForm form = new FukaDaichoForm();
        when(nokigenService.findAll()).thenReturn(List.of());
        when(fukaService.getDaichoData("00100001", "2024", null)).thenReturn(form);
        when(fukaService.getExistingNendoList("00100001")).thenReturn(List.of());
        when(fukaService.getNendoStMonth()).thenReturn(4);
        Model model = new ExtendedModelMap();

        String view = controller.showDaicho("2024", null, session, model);

        assertThat(view).isEqualTo("fuka/tFukaDaicho");
        assertThat(model.asMap()).containsEntry("errorMessage", "納入期限が登録されていません。");
    }

    // No.4 selectedのshiteiNoが空文字・gassanShiteiNoも空文字の場合、モーダル表示フラグが設定されてDaicho画面を返す
    @Test
    void showDaicho_shiteiNoが空文字gassanShiteiNoも空文字_モーダル表示() {
        MockHttpSession session = sessionWithGassan("", "");
        Model model = new ExtendedModelMap();

        String view = controller.showDaicho(null, null, session, model);

        assertThat(view).isEqualTo("fuka/tFukaDaicho");
        assertThat(model.asMap()).containsKey("showShiteiGassanModal");
    }

    // No.5 セッションに指定番号なし、合算指定番号なしの場合、モーダル表示フラグが設定されてDaicho画面を返す
    @Test
    void showDaicho_セッション未設定_モーダル表示() {
        MockHttpSession session = new MockHttpSession();
        Model model = new ExtendedModelMap();

        String view = controller.showDaicho(null, null, session, model);

        assertThat(view).isEqualTo("fuka/tFukaDaicho");
        assertThat(model.asMap()).containsKey("showShiteiGassanModal");
    }

    // No.6 書き込み権限なしの場合、例外をスロー
    @Test
    void showDaicho_アクセス権なし_例外をスロー() {
        doThrow(new RuntimeException("権限なし")).when(accessChecker).checkAccess(any());
        MockHttpSession session = new MockHttpSession();
        Model model = new ExtendedModelMap();

        assertThatThrownBy(() -> controller.showDaicho(null, null, session, model))
                .isInstanceOf(RuntimeException.class);
    }


    // ===== register =====

    // No.7 セッションに指定番号あり・対象月指定あり・未申告・合算対象外の場合、登録フォームが設定されてConfig画面を返す
    @Test
    void register_未申告合算対象外_Config画面を返す() {
        MockHttpSession session = sessionWith("00100001");
        when(fukaService.resolveGassanTekiyoPeriod(eq("00100001"), any())).thenReturn(null);
        when(fukaService.isAlreadyRegistered("00100001", "202604")).thenReturn(false);
        when(fukaService.getDeclarationFormForRegister("00100001", "202604")).thenReturn(new FukaDeclarationForm());
        Model model = new ExtendedModelMap();

        String view = controller.register("202604", session, new RedirectAttributesModelMap(), model);

        assertThat(view).isEqualTo("fuka/tFukaConfig");
        assertThat(model.asMap()).containsKey("fukaDeclarationForm");
    }

    // No.8 対象月が申告済みの場合、errorMessageが設定されてリダイレクトする
    @Test
    void register_申告済み_errorMessageを設定してリダイレクト() {
        MockHttpSession session = sessionWith("00100001");
        when(fukaService.resolveGassanTekiyoPeriod(eq("00100001"), any())).thenReturn(null);
        when(fukaService.isAlreadyRegistered("00100001", "202604")).thenReturn(true);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.register("202604", session, redirectAttributes, new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/declaration/payment-ledger");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("errorMessage");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage").toString())
                .contains("申告済みのデータです");
    }

    // No.9 対象月が合算対象月の場合、errorMessageが設定されてリダイレクトする
    @Test
    void register_合算対象月_errorMessageを設定してリダイレクト() {
        MockHttpSession session = sessionWith("00100001");
        when(fukaService.resolveGassanTekiyoPeriod(eq("00100001"), any())).thenReturn("2026年4月以降");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.register("202604", session, redirectAttributes, new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/declaration/payment-ledger");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage").toString())
                .contains("合算対象月");
    }

    // No.10 合算指定番号がセッションにあり対象月が適用期間外の場合、errorMessageが設定されてリダイレクトする
    @Test
    void register_合算指定番号あり適用期間外_errorMessageを設定してリダイレクト() {
        MockHttpSession session = sessionWithGassan("00100001", "901001");
        when(fukaService.isGassanTargetMonth(eq("901001"), any())).thenReturn(false);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.register("202604", session, redirectAttributes, new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/declaration/payment-ledger");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage").toString())
                .contains("合算適用期間外");
    }

    // No.11 getDeclarationFormForRegisterで例外が発生した場合、errorMessageが設定されてリダイレクトする
    @Test
    void register_getDeclarationFormForRegisterで例外_errorMessageを設定してリダイレクト() {
        MockHttpSession session = sessionWith("00100001");
        when(fukaService.resolveGassanTekiyoPeriod(eq("00100001"), any())).thenReturn(null);
        when(fukaService.isAlreadyRegistered("00100001", "202604")).thenReturn(false);
        when(fukaService.getDeclarationFormForRegister("00100001", "202604"))
                .thenThrow(new RuntimeException("税率マスタ未登録"));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.register("202604", session, redirectAttributes, new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/declaration/payment-ledger");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage").toString())
                .isEqualTo("税率マスタ未登録");
    }

    // No.12 month未指定（null）の場合、エラーメッセージ「対象年度が選択されていません。」を返す
    @Test
    void register_month未指定_対象年度未選択エラー() {
        MockHttpSession session = sessionWith("00100001");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.register(null, session, redirectAttributes, new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/declaration/payment-ledger");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage").toString())
                .isEqualTo("対象年度が選択されていません。");
    }

    // No.13 monthが6桁未満の場合、エラーメッセージ「対象年度の形式が不正です。」を返す
    @Test
    void register_monthが6桁未満_形式不正エラー() {
        MockHttpSession session = sessionWith("00100001");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.register("2026", session, redirectAttributes, new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/declaration/payment-ledger");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage").toString())
                .isEqualTo("対象年度の形式が不正です。");
    }

    // No.14 セッションに指定番号なし、合算指定番号なしの場合、モーダル表示フラグが設定されてDaicho画面を返す
    @Test
    void register_セッション未設定_モーダル表示() {
        MockHttpSession session = new MockHttpSession();
        Model model = new ExtendedModelMap();

        String view = controller.register("202604", session, new RedirectAttributesModelMap(), model);

        assertThat(view).isEqualTo("fuka/tFukaDaicho");
        assertThat(model.asMap()).containsKey("showShiteiGassanModal");
    }


    // ===== showEdit =====

    // No.15 セッションに指定番号あり・申告済み・合算対象外の場合、編集フォームが設定されてConfig画面を返す
    @Test
    void showEdit_申告済み合算対象外_Config画面を返す() {
        MockHttpSession session = sessionWith("00100001");
        when(fukaService.getNendoStMonth()).thenReturn(4);
        when(fukaService.resolveGassanTekiyoPeriod(eq("00100001"), any())).thenReturn(null);
        when(fukaService.isAlreadyRegisteredByKibetsu("00100001", "2024", 1)).thenReturn(true);
        FukaDeclarationForm form = new FukaDeclarationForm();
        when(fukaService.getDeclarationFormForEdit("00100001", "2024", 1)).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.showEdit("2024", 1, session, new RedirectAttributesModelMap(), model);

        assertThat(view).isEqualTo("fuka/tFukaConfig");
        assertThat(form.isEdit()).isTrue();
    }

    // No.16 対象月が未申告の場合、errorMessageが設定されてリダイレクトする
    @Test
    void showEdit_未申告_errorMessageを設定してリダイレクト() {
        MockHttpSession session = sessionWith("00100001");
        when(fukaService.getNendoStMonth()).thenReturn(4);
        when(fukaService.resolveGassanTekiyoPeriod(eq("00100001"), any())).thenReturn(null);
        when(fukaService.isAlreadyRegisteredByKibetsu("00100001", "2024", 1)).thenReturn(false);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.showEdit("2024", 1, session, redirectAttributes, new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/declaration/payment-ledger");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage").toString())
                .contains("未申告のデータです");
    }

    // No.17 合算指定番号なし・対象月が合算対象月の場合、errorMessageが設定されてリダイレクトする
    @Test
    void showEdit_合算指定番号なし合算対象月_errorMessageを設定してリダイレクト() {
        MockHttpSession session = sessionWith("00100001");
        when(fukaService.getNendoStMonth()).thenReturn(4);
        when(fukaService.resolveGassanTekiyoPeriod(eq("00100001"), any())).thenReturn("2026年4月以降");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.showEdit("2024", 1, session, redirectAttributes, new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/declaration/payment-ledger");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage").toString())
                .contains("合算対象月");
    }

    // No.18 合算指定番号あり・対象月が適用期間外の場合、errorMessageが設定されてリダイレクトする
    @Test
    void showEdit_合算指定番号あり適用期間外_errorMessageを設定してリダイレクト() {
        MockHttpSession session = sessionWithGassan("00100001", "901001");
        when(fukaService.getNendoStMonth()).thenReturn(4);
        when(fukaService.isGassanTargetMonth(eq("901001"), any())).thenReturn(false);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.showEdit("2024", 1, session, redirectAttributes, new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/declaration/payment-ledger");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage").toString())
                .contains("合算適用期間外");
    }

    // No.19 セッションに指定番号なし、合算指定番号なしの場合、モーダル表示フラグが設定されてDaicho画面を返す
    @Test
    void showEdit_セッション未設定_モーダル表示() {
        MockHttpSession session = new MockHttpSession();
        Model model = new ExtendedModelMap();

        String view = controller.showEdit("2024", 1, session, new RedirectAttributesModelMap(), model);

        assertThat(view).isEqualTo("fuka/tFukaDaicho");
        assertThat(model.asMap()).containsKey("showShiteiGassanModal");
    }

    // ===== showView =====

    // No.20 セッションに指定番号あり・申告済み・合算対象外・rno未指定の場合、照会フォームが設定されてConfig画面を返す
    @Test
    void showView_申告済み合算対象外rno未指定_Config画面を返す() {
        MockHttpSession session = sessionWith("00100001");
        when(fukaService.getNendoStMonth()).thenReturn(4);
        when(fukaService.resolveGassanTekiyoPeriod(eq("00100001"), any())).thenReturn(null);
        when(fukaService.isAlreadyRegisteredByKibetsu("00100001", "2024", 1)).thenReturn(true);
        FukaDeclarationForm form = new FukaDeclarationForm();
        when(fukaService.getDeclarationFormForView("00100001", "2024", 1)).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.showView("2024", 1, null, session, new RedirectAttributesModelMap(), model);

        assertThat(view).isEqualTo("fuka/tFukaConfig");
        assertThat(form.isView()).isTrue();
    }

    // No.21 rno指定ありの場合、getDeclarationFormForViewByRnoが呼ばれてConfig画面を返す
    @Test
    void showView_rno指定あり_getDeclarationFormForViewByRnoが呼ばれる() {
        MockHttpSession session = sessionWith("00100001");
        when(fukaService.getNendoStMonth()).thenReturn(4);
        when(fukaService.resolveGassanTekiyoPeriod(eq("00100001"), any())).thenReturn(null);
        when(fukaService.isAlreadyRegisteredByKibetsu("00100001", "2024", 1)).thenReturn(true);
        when(fukaService.getDeclarationFormForViewByRno("00100001", "2024", 1, 2)).thenReturn(new FukaDeclarationForm());
        Model model = new ExtendedModelMap();

        String view = controller.showView("2024", 1, 2, session, new RedirectAttributesModelMap(), model);

        assertThat(view).isEqualTo("fuka/tFukaConfig");
        verify(fukaService).getDeclarationFormForViewByRno("00100001", "2024", 1, 2);
    }

    // No.22 対象月が未申告の場合、errorMessageが設定されてリダイレクトする
    @Test
    void showView_未申告_errorMessageを設定してリダイレクト() {
        MockHttpSession session = sessionWith("00100001");
        when(fukaService.getNendoStMonth()).thenReturn(4);
        when(fukaService.resolveGassanTekiyoPeriod(eq("00100001"), any())).thenReturn(null);
        when(fukaService.isAlreadyRegisteredByKibetsu("00100001", "2024", 1)).thenReturn(false);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.showView("2024", 1, null, session, redirectAttributes, new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/declaration/payment-ledger");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage").toString())
                .contains("未申告のデータです");
    }

    // No.23 合算指定番号なし・対象月が合算対象月の場合、errorMessageが設定されてリダイレクトする
    @Test
    void showView_合算指定番号なし合算対象月_errorMessageを設定してリダイレクト() {
        MockHttpSession session = sessionWith("00100001");
        when(fukaService.getNendoStMonth()).thenReturn(4);
        when(fukaService.resolveGassanTekiyoPeriod(eq("00100001"), any())).thenReturn("2026年4月以降");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.showView("2024", 1, null, session, redirectAttributes, new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/declaration/payment-ledger");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage").toString())
                .contains("合算対象月");
    }

    // No.24 合算指定番号あり・対象月が適用期間外の場合、errorMessageが設定されてリダイレクトする
    @Test
    void showView_合算指定番号あり適用期間外_errorMessageを設定してリダイレクト() {
        MockHttpSession session = sessionWithGassan("00100001", "901001");
        when(fukaService.getNendoStMonth()).thenReturn(4);
        when(fukaService.isGassanTargetMonth(eq("901001"), any())).thenReturn(false);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.showView("2024", 1, null, session, redirectAttributes, new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/declaration/payment-ledger");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage").toString())
                .contains("合算適用期間外");
    }

    // No.25 セッションに指定番号なし、合算指定番号なしの場合、モーダル表示フラグが設定されてDaicho画面を返す
    @Test
    void showView_セッション未設定_モーダル表示() {
        MockHttpSession session = new MockHttpSession();
        Model model = new ExtendedModelMap();

        String view = controller.showView("2024", 1, null, session, new RedirectAttributesModelMap(), model);

        assertThat(view).isEqualTo("fuka/tFukaDaicho");
        assertThat(model.asMap()).containsKey("showShiteiGassanModal");
    }


    // ===== save =====

    // No.26 バリデーションエラーなし・税額チェック通過・保存成功の場合、リダイレクト＋successMessage
    @Test
    void save_正常保存_リダイレクトとsuccessMessage() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setShiteiNo("00100001");
        form.setTorokuDate(LocalDate.now());
        form.setShinkokuDate(LocalDate.now());
        form.setTaxCheckBypassed(true);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "fukaDeclarationForm");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.save(form, bindingResult, new ExtendedModelMap(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/declaration/payment-ledger");
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage").toString())
                .isEqualTo("賦課情報を更新しました。");
        verify(fukaService).saveDeclaration(form);
    }

    // No.27 バリデーションエラーあり（申告日>登録日）の場合、Config画面を返す＋validationErrors
    @Test
    void save_申告日が登録日より後_validationErrorsを設定してConfig画面を返す() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setTorokuDate(LocalDate.of(2024, 5, 1));
        form.setShinkokuDate(LocalDate.of(2024, 5, 2));
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "fukaDeclarationForm");
        Model model = new ExtendedModelMap();

        String view = controller.save(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("fuka/tFukaConfig");
        assertThat(model.asMap()).containsKey("validationErrors");
    }

    // No.28 加算金額区分選択・割合未入力の場合、Config画面を返す＋validationErrors
    @Test
    void save_加算金額区分選択割合未入力_validationErrorsを設定してConfig画面を返す() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setTorokuDate(LocalDate.now());
        form.setShinkokuDate(LocalDate.now());
        form.setAdditionalCategory1("1");
        form.setAdditionalRate1(null);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "fukaDeclarationForm");
        Model model = new ExtendedModelMap();

        String view = controller.save(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("fuka/tFukaConfig");
        assertThat(model.asMap()).containsKey("validationErrors");
    }

    // No.29 加算金額区分選択・金額未入力の場合、Config画面を返す＋validationErrors
    @Test
    void save_加算金額区分選択金額未入力_validationErrorsを設定してConfig画面を返す() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setTorokuDate(LocalDate.now());
        form.setShinkokuDate(LocalDate.now());
        form.setAdditionalCategory1("1");
        form.setAdditionalRate1("10");
        form.setAdditionalAmount1(null);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "fukaDeclarationForm");
        Model model = new ExtendedModelMap();

        String view = controller.save(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("fuka/tFukaConfig");
        assertThat(model.asMap()).containsKey("validationErrors");
    }

    // No.30 納入金額入力・納入年月日未入力の場合、Config画面を返す＋validationErrors
    @Test
    void save_納入金額入力納入年月日未入力_validationErrorsを設定してConfig画面を返す() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setTorokuDate(LocalDate.now());
        form.setShinkokuDate(LocalDate.now());
        form.setShunoKingaku(1000L);
        form.setShunoYmd(null);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "fukaDeclarationForm");
        Model model = new ExtendedModelMap();

        String view = controller.save(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("fuka/tFukaConfig");
        assertThat(model.asMap()).containsKey("validationErrors");
    }

    // No.31 税額チェックでズレあり・taxCheckBypassed=falseの場合、警告モーダルを表示してConfig画面を返す
    @Test
    void save_税額ズレありtaxCheckBypassedfalse_警告モーダル表示() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setTorokuDate(LocalDate.now());
        form.setShinkokuDate(LocalDate.now());
        form.setTaxCheckBypassed(false);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "fukaDeclarationForm");
        when(fukaValidatorService.getDiscrepancyMessages(form)).thenReturn(List.of("ズレあり"));
        Model model = new ExtendedModelMap();

        String view = controller.save(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("fuka/tFukaConfig");
        assertThat(model.asMap()).containsKey("showTaxWarningModal");
        assertThat(model.asMap()).containsKey("discrepancyMessages");
    }

    // No.32 税額チェックでズレあり・taxCheckBypassed=trueの場合、保存が実行されてリダイレクトする
    @Test
    void save_税額ズレありtaxCheckBypassedtrue_保存してリダイレクト() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setTorokuDate(LocalDate.now());
        form.setShinkokuDate(LocalDate.now());
        form.setTaxCheckBypassed(true);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "fukaDeclarationForm");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.save(form, bindingResult, new ExtendedModelMap(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/declaration/payment-ledger");
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage").toString())
                .isEqualTo("賦課情報を更新しました。");
    }

    // No.33 編集モード（isEdit=true）かつ変更区分未選択の場合、Config画面を返す
    @Test
    void save_編集モード変更区分未選択_Config画面を返す() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setTorokuDate(LocalDate.now());
        form.setShinkokuDate(LocalDate.now());
        form.setEdit(true);
        form.setModificationCategory(null);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "fukaDeclarationForm");

        String view = controller.save(form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("fuka/tFukaConfig");
    }

    // No.34 編集モード（isEdit=true）かつ変更区分選択済みの場合、保存が実行されてリダイレクトする
    @Test
    void save_編集モード変更区分選択済み_保存してリダイレクト() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setTorokuDate(LocalDate.now());
        form.setShinkokuDate(LocalDate.now());
        form.setEdit(true);
        form.setModificationCategory("2");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "fukaDeclarationForm");
        when(fukaValidatorService.getDiscrepancyMessages(form)).thenReturn(List.of());
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.save(form, bindingResult, new ExtendedModelMap(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/declaration/payment-ledger");
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage").toString())
                .isEqualTo("賦課情報を更新しました。");
    }

    // No.35 saveDeclarationで例外が発生した場合、errorMessageが設定されてConfig画面を返す
    @Test
    void save_saveDeclarationで例外_errorMessageを設定してConfig画面を返す() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setTorokuDate(LocalDate.now());
        form.setShinkokuDate(LocalDate.now());
        form.setTaxCheckBypassed(true);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "fukaDeclarationForm");
        doThrow(new RuntimeException("保存失敗")).when(fukaService).saveDeclaration(form);
        Model model = new ExtendedModelMap();

        String view = controller.save(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("fuka/tFukaConfig");
        assertThat(model.asMap().get("errorMessage").toString()).contains("保存失敗");
    }

    // No.36 書き込み権限なしの場合、例外をスロー
    @Test
    void save_書き込み権限なし_例外をスロー() {
        doThrow(new RuntimeException("権限なし")).when(accessChecker).checkWriteAccess(any());
        FukaDeclarationForm form = new FukaDeclarationForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "fukaDeclarationForm");

        assertThatThrownBy(() -> controller.save(form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap()))
                .isInstanceOf(RuntimeException.class);
    }


    // ===== estimateBreakdown =====

    // No.37 正常に内訳試算が実行された場合、200 OK＋FukaMonthlyDeclarationDtoを返す
    @Test
    void estimateBreakdown_正常_200OKとDtoを返す() {
        FukaMonthlyDeclarationDto dto = new FukaMonthlyDeclarationDto();
        when(fukaService.estimateBreakdown(eq("1"), any())).thenReturn(dto);
        FukaMonthlyDeclarationDto monthlyDetail = new FukaMonthlyDeclarationDto();

        ResponseEntity<?> response = controller.estimateBreakdown("1", monthlyDetail);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    // No.38 estimateBreakdownで例外が発生した場合、500＋エラーメッセージを返す
    @Test
    void estimateBreakdown_例外発生_500とエラーメッセージを返す() {
        when(fukaService.estimateBreakdown(eq("1"), any())).thenThrow(new RuntimeException("試算失敗"));
        FukaMonthlyDeclarationDto monthlyDetail = new FukaMonthlyDeclarationDto();

        ResponseEntity<?> response = controller.estimateBreakdown("1", monthlyDetail);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().toString()).contains("試算失敗");
    }

    // No.39 書き込み権限なしの場合、500＋エラーメッセージを返す
    @Test
    void estimateBreakdown_書き込み権限なし_500とエラーメッセージを返す() {
        doThrow(new RuntimeException("権限なし")).when(accessChecker).checkWriteAccess(any());
        FukaMonthlyDeclarationDto monthlyDetail = new FukaMonthlyDeclarationDto();

        ResponseEntity<?> response = controller.estimateBreakdown("1", monthlyDetail);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().toString()).contains("権限なし");
    }

    // ===== ヘルパーメソッド（既存テストとの互換性維持） =====

    @Test
    void save_課税対象外宿泊数の桁数エラーはサマリに項目名が付く() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "fukaDeclarationForm");
        bindingResult.rejectValue("monthlyDetail.exemptStayCount", "Digits", "9桁以内で入力してください");
        Model model = new ExtendedModelMap();

        String view = controller.save(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("fuka/tFukaConfig");
        assertThat(validationErrors(model)).containsExactly("課税対象外宿泊数は9桁以内で入力してください");
        assertThat(fieldErrorMessages(model))
                .containsEntry("monthlyDetail.exemptStayCount", "9桁以内で入力してください");
    }

    @Test
    void save_宿泊数合計の桁数エラーはサマリに項目名が付く() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "fukaDeclarationForm");
        bindingResult.rejectValue("monthlyDetail.totalStayCount", "Digits", "9桁以内で入力してください");
        Model model = new ExtendedModelMap();

        String view = controller.save(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("fuka/tFukaConfig");
        assertThat(validationErrors(model)).containsExactly("宿泊数合計は9桁以内で入力してください");
        assertThat(fieldErrorMessages(model))
                .containsEntry("monthlyDetail.totalStayCount", "9桁以内で入力してください");
    }

    @Test
    void save_区分ごとの宿泊数の桁数エラーはサマリに区分名が付く() {
        FukaDeclarationForm form = formWithTaxDetailLabel("0円以上");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "fukaDeclarationForm");
        bindingResult.rejectValue("monthlyDetail.taxDetails[0].hakusu", "Digits", "8桁以内で入力してください");
        Model model = new ExtendedModelMap();

        String view = controller.save(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("fuka/tFukaConfig");
        assertThat(validationErrors(model)).containsExactly("宿泊数（0円以上の区分）は8桁以内で入力してください");
        assertThat(fieldErrorMessages(model))
                .containsEntry("monthlyDetail.taxDetails[0].hakusu", "8桁以内で入力してください");
    }

    @Test
    void save_区分名が取得できない場合は通番で代替する() {
        FukaDeclarationForm form = formWithTaxDetailLabel(null);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "fukaDeclarationForm");
        bindingResult.rejectValue("monthlyDetail.taxDetails[0].hakusu", "Digits", "8桁以内で入力してください");
        Model model = new ExtendedModelMap();

        controller.save(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(validationErrors(model)).containsExactly("宿泊数（区分1）は8桁以内で入力してください");
    }

    private FukaDeclarationForm formWithTaxDetailLabel(String label) {
        FukaDeclarationForm form = new FukaDeclarationForm();
        FukaTaxDetailDto detail = new FukaTaxDetailDto();
        detail.setLabel(label);
        form.getMonthlyDetail().getTaxDetails().add(detail);
        return form;
    }

    @SuppressWarnings("unchecked")
    private List<String> validationErrors(Model model) {
        return (List<String>) model.asMap().get("validationErrors");
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> fieldErrorMessages(Model model) {
        return (Map<String, String>) model.asMap().get("fieldErrorMessages");
    }
}

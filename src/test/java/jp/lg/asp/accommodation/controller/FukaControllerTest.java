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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.FukaDaichoForm;
import jp.lg.asp.accommodation.dto.FukaDeclarationForm;
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

    private void stubDaichoNormal(String shiteiNo, String nendo) {
        when(nokigenService.findByNendo(nendo)).thenReturn(new Nokigen());
        when(fukaService.getDaichoData(eq(shiteiNo), eq(nendo))).thenReturn(new FukaDaichoForm());
        when(fukaService.getExistingNendoList(shiteiNo)).thenReturn(List.of());
    }

    // -----------------------------------------------------------------------
    // showDaicho
    // -----------------------------------------------------------------------

    // TC-01: セッションにshiteiNoなし(null) → showShiteiGassanModal=true、DAICHO_VIEWが返る
    @Test
    void showDaicho_セッションnull_showShiteiGassanModalがtrueでDAICHO_VIEWが返る() {
        MockHttpSession session = new MockHttpSession();
        Model model = new ExtendedModelMap();

        String view = controller.showDaicho(null, null, session, model);

        assertThat(view).isEqualTo("fuka/tFukaDaicho");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
        verify(accessChecker).checkAccess(any());
    }

    // TC-02: 指定番号なし・合算指定番号なし（両方空文字）→ showShiteiGassanModal=true、DAICHO_VIEWが返る
    @Test
    void showDaicho_指定番号なし合算指定番号なし_showShiteiGassanModalがtrueでDAICHO_VIEWが返る() {
        MockHttpSession session = sessionWithGassan("", "");
        Model model = new ExtendedModelMap();

        String view = controller.showDaicho(null, null, session, model);

        assertThat(view).isEqualTo("fuka/tFukaDaicho");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
        verify(accessChecker).checkAccess(any());
    }

    // TC-03: 指定番号なし・合算指定番号あり → showShiteiGassanModal=false、DAICHO_VIEWが返る
    @Test
    void showDaicho_指定番号なし合算指定番号あり_showShiteiGassanModalがfalseでDAICHO_VIEWが返る() {
        MockHttpSession session = sessionWithGassan("", "00200001");
        when(nokigenService.findByNendo("2024")).thenReturn(new Nokigen());
        when(fukaService.getDaichoData(eq(""), eq("2024"))).thenReturn(new FukaDaichoForm());
        when(fukaService.getExistingNendoList("")).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.showDaicho("2024", null, session, model);

        assertThat(view).isEqualTo("fuka/tFukaDaicho");
        assertThat(model.asMap()).doesNotContainKey("showShiteiGassanModal");
        verify(accessChecker).checkAccess(any());
    }

    // TC-04: 指定番号あり・合算指定番号なし → DAICHO_VIEWが返る
    @Test
    void showDaicho_指定番号あり合算指定番号なし_DAICHO_VIEWが返る() {
        MockHttpSession session = sessionWith("00100001");
        stubDaichoNormal("00100001", "2024");
        Model model = new ExtendedModelMap();

        String view = controller.showDaicho("2024", null, session, model);

        assertThat(view).isEqualTo("fuka/tFukaDaicho");
        assertThat(model.asMap()).containsKey("fukaDaichoForm");
        verify(accessChecker).checkAccess(any());
    }

    // TC-05: 指定番号あり・合算指定番号あり → DAICHO_VIEWが返る
    @Test
    void showDaicho_指定番号あり合算指定番号あり_DAICHO_VIEWが返る() {
        MockHttpSession session = sessionWithGassan("00100001", "00200001");
        when(nokigenService.findByNendo("2024")).thenReturn(new Nokigen());
        when(fukaService.getDaichoData(eq("00100001"), eq("2024"))).thenReturn(new FukaDaichoForm());
        when(fukaService.getExistingNendoList("00100001")).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.showDaicho("2024", null, session, model);

        assertThat(view).isEqualTo("fuka/tFukaDaicho");
        assertThat(model.asMap()).containsKey("fukaDaichoForm");
        verify(accessChecker).checkAccess(any());
    }

    // TC-06: nendoパラメータなし・現在月が年度開始月の前月 → selectedNendoに前年がセット
    @Test
    void showDaicho_年度パラメータなし_年度開始月の前月_selectedNendoに前年がセット() {
        int thisYear = LocalDate.now().getYear();
        int nextMonth = LocalDate.now().getMonthValue() % 12 + 1;
        int prevYear = thisYear - 1;
        when(fukaService.getNendoStMonth()).thenReturn(nextMonth);
        MockHttpSession session = sessionWith("00100001");
        when(nokigenService.findByNendo(String.valueOf(prevYear))).thenReturn(new Nokigen());
        when(fukaService.getDaichoData(eq("00100001"), eq(String.valueOf(prevYear))))
                .thenReturn(new FukaDaichoForm());
        when(fukaService.getExistingNendoList("00100001")).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        controller.showDaicho(null, null, session, model);

        assertThat(model.asMap()).containsEntry("selectedNendo", prevYear);
        verify(accessChecker).checkAccess(any());
    }

    // TC-07: nendoパラメータなし・現在月が年度開始月の当月 → selectedNendoに今年がセット
    @Test
    void showDaicho_年度パラメータなし_年度開始月の当月_selectedNendoに今年がセット() {
        int thisYear = LocalDate.now().getYear();
        int thisMonth = LocalDate.now().getMonthValue();
        when(fukaService.getNendoStMonth()).thenReturn(thisMonth);
        MockHttpSession session = sessionWith("00100001");
        when(nokigenService.findByNendo(String.valueOf(thisYear))).thenReturn(new Nokigen());
        when(fukaService.getDaichoData(eq("00100001"), eq(String.valueOf(thisYear))))
                .thenReturn(new FukaDaichoForm());
        when(fukaService.getExistingNendoList("00100001")).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        controller.showDaicho(null, null, session, model);

        assertThat(model.asMap()).containsEntry("selectedNendo", thisYear);
        verify(accessChecker).checkAccess(any());
    }

    // TC-08: nendoパラメータあり → 指定年度がselectedNendoにセット
    @Test
    void showDaicho_年度パラメータあり_指定年度がselectedNendoにセット() {
        MockHttpSession session = sessionWith("00100001");
        stubDaichoNormal("00100001", "2023");
        Model model = new ExtendedModelMap();

        controller.showDaicho("2023", null, session, model);

        assertThat(model.asMap()).containsEntry("selectedNendo", 2023);
        verify(accessChecker).checkAccess(any());
    }

    // TC-09: nokigenService.findByNendo()がnull → errorMessageがモデルにセット
    @Test
    void showDaicho_nokigenFindByNendoがnull_errorMessageがモデルにセット() {
        MockHttpSession session = sessionWith("00100001");
        when(nokigenService.findByNendo("2024")).thenReturn(null);
        when(fukaService.getDaichoData("00100001", "2024")).thenReturn(new FukaDaichoForm());
        when(fukaService.getExistingNendoList("00100001")).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.showDaicho("2024", null, session, model);

        assertThat(view).isEqualTo("fuka/tFukaDaicho");
        assertThat(model.asMap()).containsEntry("errorMessage", "納入期限が登録されていません。");
        verify(accessChecker).checkAccess(any());
    }

    // TC-10: nokigenService.findByNendo()がnull以外 → errorMessageがモデルにセットされない
    @Test
    void showDaicho_nokigenFindByNendoがnull以外_errorMessageがモデルにセットされない() {
        MockHttpSession session = sessionWith("00100001");
        when(nokigenService.findByNendo("2024")).thenReturn(new Nokigen());
        when(fukaService.getDaichoData("00100001", "2024")).thenReturn(new FukaDaichoForm());
        when(fukaService.getExistingNendoList("00100001")).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.showDaicho("2024", null, session, model);

        assertThat(view).isEqualTo("fuka/tFukaDaicho");
        assertThat(model.asMap()).doesNotContainKey("errorMessage");
        verify(accessChecker).checkAccess(any());
    }

    // TC-11: 全条件OK → DAICHO_VIEWが返り全モデル属性がセット
    @Test
    void showDaicho_全条件OK_全モデル属性がセット() {
        MockHttpSession session = sessionWith("00100001");
        FukaDaichoForm form = new FukaDaichoForm();
        when(nokigenService.findByNendo("2024")).thenReturn(new Nokigen());
        when(fukaService.getDaichoData("00100001", "2024")).thenReturn(form);
        when(fukaService.getExistingNendoList("00100001")).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.showDaicho("2024", null, session, model);

        assertThat(view).isEqualTo("fuka/tFukaDaicho");
        assertThat(model.asMap()).containsKeys(
                "fukaDaichoForm", "searchForm", "items", "totalAmount",
                "obligorId", "selectedNendo", "nendoList", "currentStatus");
        verify(accessChecker).checkAccess(any());
    }

    // -----------------------------------------------------------------------
    // register
    // -----------------------------------------------------------------------

    @Test
    void register_申告済みはリダイレクト() {
        MockHttpSession session = sessionWith("00100001");
        when(fukaService.isAlreadyRegistered("00100001", "2024-04")).thenReturn(true);

        String view = controller.register("2024-04", session,
                new RedirectAttributesModelMap(), new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/declaration/payment-ledger");
    }

    @Test
    void register_未申告は登録画面を返す() {
        MockHttpSession session = sessionWith("00100001");
        when(fukaService.isAlreadyRegistered("00100001", "2024-04")).thenReturn(false);
        when(fukaService.getDeclarationFormForRegister("00100001", "2024-04"))
                .thenReturn(new FukaDeclarationForm());
        Model model = new ExtendedModelMap();

        String view = controller.register("2024-04", session,
                new RedirectAttributesModelMap(), model);

        assertThat(view).isEqualTo("fuka/tFukaConfig");
    }

    // -----------------------------------------------------------------------
    // showEdit
    // -----------------------------------------------------------------------

    @Test
    void showEdit_未申告はリダイレクト() {
        MockHttpSession session = sessionWith("00100001");
        when(fukaService.getNendoStMonth()).thenReturn(4);
        when(fukaService.resolveGassanTekiyoPeriod(eq("00100001"), any())).thenReturn(null);
        when(fukaService.isAlreadyRegisteredByKibetsu("00100001", "2024", 1)).thenReturn(false);

        String view = controller.showEdit("2024", 1, session,
                new RedirectAttributesModelMap(), new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/declaration/payment-ledger");
    }

    @Test
    void showEdit_申告済みは編集画面を返す() {
        MockHttpSession session = sessionWith("00100001");
        when(fukaService.getNendoStMonth()).thenReturn(4);
        when(fukaService.resolveGassanTekiyoPeriod(eq("00100001"), any())).thenReturn(null);
        when(fukaService.isAlreadyRegisteredByKibetsu("00100001", "2024", 1)).thenReturn(true);
        FukaDeclarationForm form = new FukaDeclarationForm();
        when(fukaService.getDeclarationFormForEdit("00100001", "2024", 1)).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.showEdit("2024", 1, session,
                new RedirectAttributesModelMap(), model);

        assertThat(view).isEqualTo("fuka/tFukaConfig");
        assertThat(form.isEdit()).isTrue();
    }

    // -----------------------------------------------------------------------
    // save
    // -----------------------------------------------------------------------

    @Test
    void save_バリデーションエラー() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "fukaDeclarationForm");
        bindingResult.rejectValue("torokuDate", "NotNull", "必須です");
        Model model = new ExtendedModelMap();

        String view = controller.save(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("fuka/tFukaConfig");
    }

    @Test
    void save_不整合ありはモーダル表示() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setTorokuDate(LocalDate.now());
        form.setShinkokuDate(LocalDate.now());
        form.setTaxCheckBypassed(false);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "fukaDeclarationForm");
        when(fukaValidatorService.getDiscrepancyMessages(form)).thenReturn(List.of("税額不一致"));
        Model model = new ExtendedModelMap();

        String view = controller.save(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("fuka/tFukaConfig");
        assertThat(model.asMap()).containsKey("showTaxWarningModal");
    }

    @Test
    void save_正常保存() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setShiteiNo("00100001");
        form.setTorokuDate(LocalDate.now());
        form.setShinkokuDate(LocalDate.now());
        form.setTaxCheckBypassed(true);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "fukaDeclarationForm");
        Model model = new ExtendedModelMap();

        String view = controller.save(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/declaration/payment-ledger");
        verify(fukaService).saveDeclaration(form);
    }

    @Test
    void save_課税対象外宿泊数の桁数エラーはサマリに項目名が付く() {
        FukaDeclarationForm form = new FukaDeclarationForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "fukaDeclarationForm");
        bindingResult.rejectValue("monthlyDetail.exemptStayCount", "Digits", "9桁以内で入力してください");
        Model model = new ExtendedModelMap();

        String view = controller.save(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("fuka/tFukaConfig");
        assertThat(validationErrors(model))
                .containsExactly("課税対象外宿泊数は9桁以内で入力してください");
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
        assertThat(validationErrors(model))
                .containsExactly("宿泊数合計は9桁以内で入力してください");
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
        assertThat(validationErrors(model))
                .containsExactly("宿泊数（0円以上の区分）は8桁以内で入力してください");
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

        assertThat(validationErrors(model))
                .containsExactly("宿泊数（区分1）は8桁以内で入力してください");
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

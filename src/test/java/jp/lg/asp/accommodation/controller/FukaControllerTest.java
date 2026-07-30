package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;

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
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.service.FukaService;
import jp.lg.asp.accommodation.service.FukaValidatorService;
import jp.lg.asp.accommodation.util.SessionHelper;

@ExtendWith(MockitoExtension.class)
class FukaControllerTest {

    @Mock FukaService fukaService;
    @Mock ScreenAccessChecker accessChecker;
    @Mock FukaValidatorService fukaValidatorService;

    @InjectMocks FukaController controller;

    private MockHttpSession sessionWith(String shiteiNo) {
        MockHttpSession session = new MockHttpSession();
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setShiteiNo(shiteiNo);
        SessionHelper.saveShiteiGassan(session, dto);
        return session;
    }

    @Test
    void showDaicho_台帳画面を返す() {
        MockHttpSession session = sessionWith("00100001");
        FukaDaichoForm form = new FukaDaichoForm();
        when(fukaService.getDaichoData("00100001", "2024", null)).thenReturn(form);
        when(fukaService.getExistingNendoList("00100001")).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.showDaicho("2024", null, session, model);

        assertThat(view).isEqualTo("fuka/tFukaDaicho");
        assertThat(model.asMap()).containsKey("fukaDaichoForm");
    }

    @Test
    void showDaicho_年度未指定はデフォルト年度() {
        MockHttpSession session = sessionWith("00100001");
        FukaDaichoForm form = new FukaDaichoForm();
        when(fukaService.getDaichoData(eq("00100001"), any(), isNull())).thenReturn(form);
        when(fukaService.getExistingNendoList("00100001")).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.showDaicho(null, null, session, model);

        assertThat(view).isEqualTo("fuka/tFukaDaicho");
    }

    @Test
    void showDaicho_セッション未設定はモーダル表示() {
        MockHttpSession session = new MockHttpSession();
        Model model = new ExtendedModelMap();

        String view = controller.showDaicho(null, null, session, model);

        assertThat(view).isEqualTo("fuka/tFukaDaicho");
        assertThat(model.asMap()).containsKey("showShiteiGassanModal");
    }

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

    @Test
    void showEdit_未申告はリダイレクト() {
        MockHttpSession session = sessionWith("00100001");
        when(fukaService.isAlreadyRegisteredByKibetsu("00100001", "2024", 1)).thenReturn(false);

        String view = controller.showEdit("2024", 1, session,
                new RedirectAttributesModelMap(), new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/declaration/payment-ledger");
    }

    @Test
    void showEdit_申告済みは編集画面を返す() {
        MockHttpSession session = sessionWith("00100001");
        when(fukaService.isAlreadyRegisteredByKibetsu("00100001", "2024", 1)).thenReturn(true);
        FukaDeclarationForm form = new FukaDeclarationForm();
        when(fukaService.getDeclarationFormForEdit("00100001", "2024", 1)).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.showEdit("2024", 1, session,
                new RedirectAttributesModelMap(), model);

        assertThat(view).isEqualTo("fuka/tFukaConfig");
        assertThat(form.isEdit()).isTrue();
    }

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
}

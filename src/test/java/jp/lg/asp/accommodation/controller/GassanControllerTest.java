package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.GassanDaichoItem;
import jp.lg.asp.accommodation.dto.GassanDaichoSearchForm;
import jp.lg.asp.accommodation.dto.GassanForm;
import jp.lg.asp.accommodation.service.GassanDaichoService;
import jp.lg.asp.accommodation.service.GassanService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GassanControllerTest {

    @Mock GassanService gassanService;
    @Mock GassanDaichoService gassanDaichoService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks GassanController controller;

    @Test
    void showRegistrationForm_登録画面を返す() {
        Model model = new ExtendedModelMap();

        String view = controller.showRegistrationForm(model);

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        assertThat(model.asMap()).containsEntry("isEdit", false);
    }

    @Test
    void showDaicho_一覧画面を返す() {
        when(gassanDaichoService.search(any())).thenReturn(
                new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        Model model = new ExtendedModelMap();

        String view = controller.showDaicho(new GassanDaichoSearchForm(), model);

        assertThat(view).isEqualTo("gassan/tGassanDaicho");
    }

    @Test
    void showView_データあり_照会画面を返す() {
        GassanDaichoItem item = new GassanDaichoItem();
        when(gassanDaichoService.getByGassanShiteiNo("G001")).thenReturn(item);
        Model model = new ExtendedModelMap();

        String view = controller.showView("G001", model);

        assertThat(view).isEqualTo("gassan/tGassanView");
    }

    @Test
    void showView_データなし_リダイレクト() {
        when(gassanDaichoService.getByGassanShiteiNo("G999")).thenReturn(null);
        Model model = new ExtendedModelMap();

        String view = controller.showView("G999", model);

        assertThat(view).isEqualTo("redirect:/gassan/list");
    }

    @Test
    void showViewForm_照会フォームを返す() {
        GassanForm form = new GassanForm();
        when(gassanService.getByGassanShiteiNo("G001")).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.showViewForm("G001", null, model);

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        assertThat(model.asMap()).containsEntry("isView", true);
    }

    @Test
    void register_正常登録() {
        GassanForm form = new GassanForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
        Model model = new ExtendedModelMap();

        String view = controller.register(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/gassan/list");
        verify(gassanService).register(form);
    }

    @Test
    void getFacilitiesByAtena_施設一覧を返す() {
        when(gassanService.getFacilitiesByAtenaNo(BigDecimal.valueOf(1001))).thenReturn(List.of());

        List<GassanForm.FacilityItem> result = controller.getFacilitiesByAtena(
                Map.of("atenaNo", "1001"));

        assertThat(result).isNotNull();
    }
}

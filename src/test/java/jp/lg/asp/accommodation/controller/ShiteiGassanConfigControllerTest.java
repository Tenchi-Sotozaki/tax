package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

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
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.ShiteiGassanConfigDto;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.service.ShiteiGassanConfigService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShiteiGassanConfigControllerTest {

    @Mock ShiteiGassanConfigService shiteiGassanConfigService;
    @Mock ScreenAccessChecker accessChecker;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks ShiteiGassanConfigController controller;

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("011002");
    }

    @Test
    void register_未登録は登録画面を返す() {
        Jichitai jichitai = new Jichitai();
        when(shiteiGassanConfigService.findById("011002")).thenReturn(jichitai);
        Model model = new ExtendedModelMap();

        String view = controller.register(model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/shiteiGassanConfig");
        assertThat(model.asMap()).containsEntry("mode", "register");
    }

    @Test
    void register_登録済みは照会へリダイレクト() {
        Jichitai jichitai = new Jichitai();
        jichitai.setShiteiStChar("000");
        when(shiteiGassanConfigService.findById("011002")).thenReturn(jichitai);

        String view = controller.register(new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/admin/shitei-gassan/view");
    }

    @Test
    void view_登録済みは照会画面を返す() {
        Jichitai jichitai = new Jichitai();
        jichitai.setShiteiStChar("000");
        jichitai.setGassanStChar("900");
        when(shiteiGassanConfigService.findById("011002")).thenReturn(jichitai);
        Model model = new ExtendedModelMap();

        String view = controller.view(model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/shiteiGassanConfig");
        assertThat(model.asMap()).containsEntry("mode", "view");
    }

    @Test
    void save_正常保存() {
        ShiteiGassanConfigDto dto = new ShiteiGassanConfigDto();
        dto.setShiteiStChar("000");
        dto.setGassanStChar("900");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(dto, "configDto");
        Jichitai jichitai = new Jichitai();
        when(shiteiGassanConfigService.findById("011002")).thenReturn(jichitai);

        String view = controller.save(dto, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/admin/shitei-gassan/view");
        verify(shiteiGassanConfigService).save("011002", dto);
    }
}

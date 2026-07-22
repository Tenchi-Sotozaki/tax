package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.dto.KofuRitsuConfigDto;
import jp.lg.asp.accommodation.service.KofuRitsuConfigService;

@ExtendWith(MockitoExtension.class)
class KofuRitsuConfigControllerTest {

    @Mock KofuRitsuConfigService kofuRitsuConfigService;

    @InjectMocks KofuRitsuConfigController controller;

    @Test
    void index_初期表示() {
        when(kofuRitsuConfigService.findAll()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.index(model);

        assertThat(view).isEqualTo("admin/kofuRitsuConfig");
        assertThat(model.asMap()).containsKeys("configForm", "historyList");
    }

    @Test
    void save_バリデーションエラー() {
        KofuRitsuConfigDto form = new KofuRitsuConfigDto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "configForm");
        bindingResult.rejectValue("kofuRitsu", "NotNull", "必須です");
        when(kofuRitsuConfigService.findAll()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.save(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/kofuRitsuConfig");
    }

    @Test
    void save_正常登録() {
        KofuRitsuConfigDto form = new KofuRitsuConfigDto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "configForm");
        Model model = new ExtendedModelMap();

        String view = controller.save(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/admin/kofu-ritsu");
        verify(kofuRitsuConfigService).register(form);
    }
}

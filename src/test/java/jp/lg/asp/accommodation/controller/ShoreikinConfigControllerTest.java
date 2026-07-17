package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.ShoreikinConfigDto;
import jp.lg.asp.accommodation.service.ShoreikinConfigService;

@ExtendWith(MockitoExtension.class)
class ShoreikinConfigControllerTest {

    @Mock ShoreikinConfigService shoreikinConfigService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks ShoreikinConfigController controller;

    @Test
    void config_照会画面を返す() {
        ShoreikinConfigDto dto = new ShoreikinConfigDto();
        when(shoreikinConfigService.getShoreikin("00100001", "2024")).thenReturn(dto);
        Model model = new ExtendedModelMap();

        String view = controller.config("00100001", "2024", model);

        assertThat(view).isEqualTo("shoreikin/shoreikinConfig");
        assertThat(model.asMap()).containsKey("configForm");
    }

    @Test
    void editMode_編集モードに切り替え() {
        ShoreikinConfigDto form = new ShoreikinConfigDto();
        Model model = new ExtendedModelMap();

        String view = controller.editMode(form, model);

        assertThat(view).isEqualTo("shoreikin/shoreikinConfig");
        assertThat(form.getMode()).isEqualTo("edit");
    }

    @Test
    void calculate_算出処理() {
        ShoreikinConfigDto form = new ShoreikinConfigDto();
        ShoreikinConfigDto result = new ShoreikinConfigDto();
        when(shoreikinConfigService.calculateShoreikin(form)).thenReturn(result);
        Model model = new ExtendedModelMap();

        String view = controller.calculate(form, model);

        assertThat(view).isEqualTo("shoreikin/shoreikinConfig");
    }

    @Test
    void create_バリデーションエラー() {
        ShoreikinConfigDto form = new ShoreikinConfigDto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "configForm");
        bindingResult.rejectValue("shiteiNo", "NotBlank", "必須です");
        Model model = new ExtendedModelMap();

        String view = controller.create(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("shoreikin/shoreikinConfig");
    }

    @Test
    void create_正常登録() {
        ShoreikinConfigDto form = new ShoreikinConfigDto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "configForm");
        Model model = new ExtendedModelMap();

        String view = controller.create(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/shoreikin/list");
        verify(shoreikinConfigService).createShoreikin(form);
    }

    @Test
    void update_正常更新() {
        ShoreikinConfigDto form = new ShoreikinConfigDto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "configForm");
        Model model = new ExtendedModelMap();

        String view = controller.update(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/shoreikin/list");
        verify(shoreikinConfigService).updateShoreikin(form);
    }
}

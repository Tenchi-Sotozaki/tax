package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
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
import jp.lg.asp.accommodation.dto.FurikomiKozaDto;
import jp.lg.asp.accommodation.service.FurikomiKozaService;

@ExtendWith(MockitoExtension.class)
class FurikomiKozaControllerTest {

    @Mock FurikomiKozaService furikomiKozaService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks FurikomiKozaController controller;

    @Test
    void view_照会画面を返す() {
        FurikomiKozaDto dto = new FurikomiKozaDto();
        when(furikomiKozaService.getFurikomiKoza("00100001")).thenReturn(dto);
        Model model = new ExtendedModelMap();

        String view = controller.view("00100001", model);

        assertThat(view).isEqualTo("shoreikin/furikomiKoza");
        assertThat(model.asMap()).containsKey("kozaForm");
    }

    @Test
    void editMode_編集モード切り替え() {
        FurikomiKozaDto form = new FurikomiKozaDto();
        Model model = new ExtendedModelMap();

        String view = controller.editMode(form, model);

        assertThat(view).isEqualTo("shoreikin/furikomiKoza");
        assertThat(form.getMode()).isEqualTo("edit");
    }

    @Test
    void create_バリデーションエラー() {
        FurikomiKozaDto form = new FurikomiKozaDto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "kozaForm");
        bindingResult.rejectValue("bankCd", "NotBlank", "必須です");
        Model model = new ExtendedModelMap();

        String view = controller.create(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("shoreikin/furikomiKoza");
    }

    @Test
    void create_正常登録() {
        FurikomiKozaDto form = new FurikomiKozaDto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "kozaForm");
        Model model = new ExtendedModelMap();

        String view = controller.create(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/shoreikin/list");
        verify(furikomiKozaService).createFurikomiKoza(form);
    }

    @Test
    void update_正常更新() {
        FurikomiKozaDto form = new FurikomiKozaDto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "kozaForm");
        Model model = new ExtendedModelMap();

        String view = controller.update(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/shoreikin/list");
        verify(furikomiKozaService).updateFurikomiKoza(form);
    }
}

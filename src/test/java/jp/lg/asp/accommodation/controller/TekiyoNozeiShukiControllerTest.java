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
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.TekiyoNozeiShukiForm;
import jp.lg.asp.accommodation.service.TekiyoNozeiShukiService;

@ExtendWith(MockitoExtension.class)
class TekiyoNozeiShukiControllerTest {

    @Mock TekiyoNozeiShukiService tekiyoNozeiShukiService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks TekiyoNozeiShukiController controller;

    @Test
    void edit_編集画面を返す() {
        TekiyoNozeiShukiForm form = new TekiyoNozeiShukiForm();
        form.setEdit(false);
        when(tekiyoNozeiShukiService.getByShiteiNo("00100001")).thenReturn(form);
        when(tekiyoNozeiShukiService.getNozeiShukiOptions()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.edit("00100001", null, model);

        assertThat(view).isEqualTo("tokugimu/tTekiyoNozeiShukiConfig");
        assertThat(model.asMap()).containsEntry("isEdit", false);
    }

    @Test
    void view_照会画面を返す() {
        TekiyoNozeiShukiForm form = new TekiyoNozeiShukiForm();
        when(tekiyoNozeiShukiService.getByShiteiNo("00100001")).thenReturn(form);
        when(tekiyoNozeiShukiService.getNozeiShukiOptions()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.view("00100001", null, model);

        assertThat(view).isEqualTo("tokugimu/tTekiyoNozeiShukiConfig");
        assertThat(model.asMap()).containsEntry("isView", true);
    }

    @Test
    void save_正常保存() {
        TekiyoNozeiShukiForm form = new TekiyoNozeiShukiForm();
        Model model = new ExtendedModelMap();

        String view = controller.save("00100001", form, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/tokugimu/list");
        verify(tekiyoNozeiShukiService).save("00100001", form);
    }

    @Test
    void save_例外時はエラー表示() {
        TekiyoNozeiShukiForm form = new TekiyoNozeiShukiForm();
        doThrow(new RuntimeException("重複エラー")).when(tekiyoNozeiShukiService).save(any(), any());
        when(tekiyoNozeiShukiService.getNozeiShukiOptions()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.save("00100001", form, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("tokugimu/tTekiyoNozeiShukiConfig");
        assertThat(model.asMap()).containsKey("errorMessage");
    }

    @Test
    void delete_削除後リダイレクト() {
        String view = controller.delete("00100001", new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/tokugimu/list");
        verify(tekiyoNozeiShukiService).delete("00100001");
    }
}

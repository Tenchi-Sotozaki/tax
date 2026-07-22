package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Map;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.TaxManagerForm;
import jp.lg.asp.accommodation.service.TaxManagerService;

@ExtendWith(MockitoExtension.class)
class TaxManagerControllerTest {

    @Mock TaxManagerService taxManagerService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks TaxManagerController controller;

    @Test
    void checkAtenaDuplicate_同一人物() {
        when(taxManagerService.isSamePerson("1001", "1001")).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = controller.checkAtenaDuplicate("1001", "1001");

        assertThat(response.getBody()).containsEntry("isDuplicate", true);
    }

    @Test
    void checkAtenaDuplicate_別人物() {
        when(taxManagerService.isSamePerson("1001", "1002")).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = controller.checkAtenaDuplicate("1001", "1002");

        assertThat(response.getBody()).containsEntry("isDuplicate", false);
    }

    @Test
    void edit_編集画面を返す() {
        TaxManagerForm form = new TaxManagerForm();
        form.setEdit(false);
        when(taxManagerService.getByShiteiNo("00100001")).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.edit("00100001", null, model);

        assertThat(view).isEqualTo("tokugimu/tTaxManagerConfig");
        assertThat(model.asMap()).containsEntry("isEdit", false);
    }

    @Test
    void view_照会画面を返す() {
        TaxManagerForm form = new TaxManagerForm();
        when(taxManagerService.getByShiteiNo("00100001")).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.view("00100001", null, null, model);

        assertThat(view).isEqualTo("tokugimu/tTaxManagerConfig");
        assertThat(model.asMap()).containsEntry("isView", true);
    }

    @Test
    void save_バリデーションエラー() {
        TaxManagerForm form = new TaxManagerForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "taxManagerForm");
        bindingResult.rejectValue("atenaNo", "NotBlank", "必須です");
        Model model = new ExtendedModelMap();

        String view = controller.save("00100001", form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("tokugimu/tTaxManagerConfig");
    }

    @Test
    void save_正常保存() {
        TaxManagerForm form = new TaxManagerForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "taxManagerForm");
        Model model = new ExtendedModelMap();

        String view = controller.save("00100001", form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/tokugimu/list");
        verify(taxManagerService).saveByShiteiNo("00100001", form);
    }

    @Test
    void delete_正常削除() {
        String view = controller.delete("00100001", new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/tokugimu/list");
        verify(taxManagerService).deleteByShiteiNo("00100001");
    }
}

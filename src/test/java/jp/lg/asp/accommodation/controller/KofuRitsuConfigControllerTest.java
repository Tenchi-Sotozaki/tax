package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
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
import jp.lg.asp.accommodation.entity.KofuRitsu;
import jp.lg.asp.accommodation.service.KofuRitsuConfigService;

@ExtendWith(MockitoExtension.class)
class KofuRitsuConfigControllerTest {

    @Mock KofuRitsuConfigService kofuRitsuConfigService;

    @InjectMocks KofuRitsuConfigController controller;

    @Test
    void register_初期表示() {
        Model model = new ExtendedModelMap();

        String view = controller.register(model);

        assertThat(view).isEqualTo("admin/kofuRitsuConfig");
        assertThat(model.asMap()).containsKey("configForm");
        assertThat(model.asMap().get("mode")).isEqualTo("register");
    }

    @Test
    void save_バリデーションエラー() {
        KofuRitsuConfigDto form = new KofuRitsuConfigDto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "configForm");
        bindingResult.rejectValue("kofuRitsu", "NotNull", "必須です");
        Model model = new ExtendedModelMap();

        String view = controller.save(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/kofuRitsuConfig");
        verify(kofuRitsuConfigService, never()).register(any());
    }

    @Test
    void save_正常登録() {
        KofuRitsuConfigDto form = new KofuRitsuConfigDto();
        form.setTekiyoStNendo(2024);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "configForm");
        Model model = new ExtendedModelMap();
        when(kofuRitsuConfigService.existsByTekiyoStNendo(2024)).thenReturn(false);

        String view = controller.save(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/admin/kofu-ritsu/list");
        verify(kofuRitsuConfigService).register(form);
    }

    @Test
    void save_同一年度重複_登録画面に戻りエラーメッセージが設定される() {
        KofuRitsuConfigDto form = new KofuRitsuConfigDto();
        form.setTekiyoStNendo(2024);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "configForm");
        Model model = new ExtendedModelMap();
        when(kofuRitsuConfigService.existsByTekiyoStNendo(2024)).thenReturn(true);

        String view = controller.save(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/kofuRitsuConfig");
        assertThat(model.asMap().get("errorMessage").toString()).contains("登録済みの適用開始年度");
        assertThat(model.asMap().get("mode")).isEqualTo("register");
        verify(kofuRitsuConfigService, never()).register(any());
    }

    @Test
    void list_照会画面() {
        when(kofuRitsuConfigService.findAll()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.list(model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/kofuRitsuList");
        assertThat(model.asMap()).containsKey("historyList");
    }

    @Test
    void list_例外発生() {
        when(kofuRitsuConfigService.findAll()).thenThrow(new RuntimeException("DB error"));
        Model model = new ExtendedModelMap();

        String view = controller.list(model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/kofuRitsuList");
        assertThat(model.asMap()).containsKey("errorMessage");
    }

    @Test
    void editForm_編集画面初期表示() {
        BigDecimal rno = BigDecimal.ONE;
        KofuRitsu entity = new KofuRitsu();
        entity.setRno(rno);
        entity.setKofuRitsu(new BigDecimal("10.00"));
        entity.setSanshutsu(1);
        entity.setKbn("1");
        entity.setSaiteigaku(BigDecimal.ZERO);
        entity.setTekiyoStNendo(2024);
        when(kofuRitsuConfigService.findByRno(rno)).thenReturn(entity);
        Model model = new ExtendedModelMap();

        String view = controller.editForm(rno, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/kofuRitsuConfig");
        assertThat(model.asMap()).containsKeys("configForm", "rno");
        assertThat(model.asMap().get("mode")).isEqualTo("edit");
    }

    @Test
    void editForm_例外発生() {
        BigDecimal rno = BigDecimal.ONE;
        when(kofuRitsuConfigService.findByRno(rno)).thenThrow(new RuntimeException("not found"));
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.editForm(rno, model, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/kofu-ritsu/list");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("errorMessage");
    }

    @Test
    void editSave_バリデーションエラー() {
        BigDecimal rno = BigDecimal.ONE;
        KofuRitsuConfigDto form = new KofuRitsuConfigDto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "configForm");
        bindingResult.rejectValue("kofuRitsu", "NotNull", "必須です");
        Model model = new ExtendedModelMap();

        String view = controller.editSave(rno, form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/kofuRitsuConfig");
        verify(kofuRitsuConfigService, never()).update(any(), any());
    }

    @Test
    void editSave_正常更新() {
        BigDecimal rno = BigDecimal.ONE;
        KofuRitsuConfigDto form = new KofuRitsuConfigDto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "configForm");
        Model model = new ExtendedModelMap();

        String view = controller.editSave(rno, form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/admin/kofu-ritsu/list");
        verify(kofuRitsuConfigService).update(rno, form);
    }
}

package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

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
import jp.lg.asp.accommodation.dto.JichitaiConfigDto;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.repository.JichitaiRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JichitaiConfigControllerTest {

    @Mock JichitaiRepository jichitaiRepository;
    @Mock ScreenAccessChecker accessChecker;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks JichitaiConfigController controller;

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("011002");
        Jichitai jichitai = new Jichitai();
        jichitai.setNendoStMonth("4");
        when(jichitaiRepository.findById("011002")).thenReturn(Optional.of(jichitai));
    }

    @Test
    void index_初期表示() {
        Model model = new ExtendedModelMap();

        String view = controller.index(model);

        assertThat(view).isEqualTo("admin/jichitaiConfig");
        assertThat(model.asMap()).containsKeys("configForm", "jichitai");
    }

    @Test
    void save_バリデーションエラー() {
        JichitaiConfigDto form = new JichitaiConfigDto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "configForm");
        bindingResult.rejectValue("nendoStMonth", "NotBlank", "必須です");
        Model model = new ExtendedModelMap();

        String view = controller.save(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/jichitaiConfig");
        assertThat(model.asMap()).containsKey("validationErrors");
    }

    @Test
    void save_正常保存() {
        JichitaiConfigDto form = new JichitaiConfigDto();
        form.setNendoStMonth("4");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "configForm");

        String view = controller.save(form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/admin/jichitai-config");
        verify(jichitaiRepository).save(any(Jichitai.class));
    }
}

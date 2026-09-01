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

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.JichitaiConfigDto;
import jp.lg.asp.accommodation.service.JichitaiConfigService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JichitaiConfigControllerTest {

    @Mock JichitaiConfigService jichitaiConfigService;
    @Mock ScreenAccessChecker accessChecker;
    @InjectMocks JichitaiConfigController controller;

    // No.14 正常系: 自治体情報が存在する場合、configFormが設定されてJichitaiConfig画面を返す
    @Test
    void index_自治体情報が存在する場合_configFormが設定されてJichitaiConfig画面を返す() {
        JichitaiConfigDto dto = new JichitaiConfigDto();
        dto.setJichitaiCd("011002");
        when(jichitaiConfigService.getJichitaiConfigDto()).thenReturn(dto);

        Model model = new ExtendedModelMap();
        String view = controller.index(model);

        assertThat(view).isEqualTo("admin/JichitaiConfig");
        assertThat(((JichitaiConfigDto) model.asMap().get("configForm")).getJichitaiCd())
                .isEqualTo("011002");
    }

    // No.15 正常系: getJichitaiConfigDtoがデフォルト値のDtoを返す場合、configFormが設定されてJichitaiConfig画面を返す
    @Test
    void index_デフォルト値のDtoを返す場合_configFormが設定されてJichitaiConfig画面を返す() {
        JichitaiConfigDto dto = new JichitaiConfigDto();
        dto.setNendoStMonth("3");
        when(jichitaiConfigService.getJichitaiConfigDto()).thenReturn(dto);

        Model model = new ExtendedModelMap();
        String view = controller.index(model);

        assertThat(view).isEqualTo("admin/JichitaiConfig");
        assertThat(model.asMap()).containsKey("configForm");
    }

    // No.16 正常系: バリデーションエラーなしで保存成功の場合、リダイレクト＋successMessage
    @Test
    void save_バリデーションエラーなしで保存成功の場合_リダイレクトとsuccessMessage() {
        JichitaiConfigDto form = new JichitaiConfigDto();
        form.setJichitaiCd("011002");
        form.setName("札幌市");
        form.setKbnName("市");
        form.setNendoStMonth("4");
        form.setNozeiShuki("1");
        form.setShiteiStChar("001");
        form.setGassanStChar("901");
        form.setUserId("user01");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "configForm");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.save(form, bindingResult, new ExtendedModelMap(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/jichitai-config");
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
                .isEqualTo("自治体情報を保存しました。");
        verify(jichitaiConfigService).saveJichitaiConfig(form);
    }

    // No.17 異常系: バリデーションエラーありの場合、jichitaiConfig画面を返す＋validationErrors
    @Test
    void save_バリデーションエラーありの場合_jichitaiConfig画面を返すとvalidationErrors() {
        JichitaiConfigDto form = new JichitaiConfigDto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "configForm");
        bindingResult.rejectValue("jichitaiCd", "NotBlank", "自治体コードは必須です。");
        Model model = new ExtendedModelMap();

        String view = controller.save(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/jichitaiConfig");
        assertThat(model.asMap()).containsKey("validationErrors");
    }

    // No.18 異常系: saveJichitaiConfigで例外が発生した場合、リダイレクト＋errorMessage
    @Test
    void save_saveJichitaiConfigで例外が発生した場合_リダイレクトとerrorMessage() {
        JichitaiConfigDto form = new JichitaiConfigDto();
        form.setJichitaiCd("011002");
        form.setName("札幌市");
        form.setKbnName("市");
        form.setNendoStMonth("4");
        form.setNozeiShuki("1");
        form.setShiteiStChar("001");
        form.setGassanStChar("901");
        form.setUserId("user01");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "configForm");
        doThrow(new RuntimeException("DB error")).when(jichitaiConfigService).saveJichitaiConfig(form);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.save(form, bindingResult, new ExtendedModelMap(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/jichitai-config");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("自治体情報の保存に失敗しました: DB error");
    }

    // No.19 異常系: 書き込み権限なしの場合、例外をスロー
    @Test
    void save_書き込み権限なしの場合_例外をスロー() {
        JichitaiConfigDto form = new JichitaiConfigDto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "configForm");
        doThrow(new RuntimeException("アクセス権限がありません")).when(accessChecker).checkWriteAccess(any());

        assertThatThrownBy(() -> controller.save(form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap()))
                .isInstanceOf(RuntimeException.class);
    }
}

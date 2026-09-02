package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    // No.14 正常系: registerエンドポイントで空のconfigFormとmode=registerでjichitaiConfig画面を返す
    @Test
    void register_空のconfigFormとmode_registerでjichitaiConfig画面を返す() {
        Model model = new ExtendedModelMap();
        String view = controller.register(model);

        assertThat(view).isEqualTo("admin/jichitaiConfig");
        assertThat(((JichitaiConfigDto) model.asMap().get("configForm")).getJichitaiCd()).isNull();
        assertThat(model.asMap().get("mode")).isEqualTo("register");
        verify(jichitaiConfigService, never()).getJichitaiConfigDto();
    }

    // No.15 正常系: viewエンドポイントでgetJichitaiConfigDtoByIdを使いmode=viewでjichitaiConfig画面を返す
    @Test
    void view_getJichitaiConfigDtoByIdを使いmode_viewでjichitaiConfig画面を返す() {
        JichitaiConfigDto dto = new JichitaiConfigDto();
        dto.setJichitaiCd("011002");
        when(jichitaiConfigService.getJichitaiConfigDtoById("011002")).thenReturn(dto);

        Model model = new ExtendedModelMap();
        String view = controller.view("011002", model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/jichitaiConfig");
        assertThat(((JichitaiConfigDto) model.asMap().get("configForm")).getJichitaiCd()).isEqualTo("011002");
        assertThat(model.asMap().get("mode")).isEqualTo("view");
    }

    // No.15a 正常系: editエンドポイントでgetJichitaiConfigDtoByIdを使いmode=editでjichitaiConfig画面を返す
    @Test
    void edit_getJichitaiConfigDtoByIdを使いmode_editでjichitaiConfig画面を返す() {
        JichitaiConfigDto dto = new JichitaiConfigDto();
        dto.setJichitaiCd("011002");
        when(jichitaiConfigService.getJichitaiConfigDtoById("011002")).thenReturn(dto);

        Model model = new ExtendedModelMap();
        String view = controller.edit("011002", model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/jichitaiConfig");
        assertThat(((JichitaiConfigDto) model.asMap().get("configForm")).getJichitaiCd()).isEqualTo("011002");
        assertThat(model.asMap().get("mode")).isEqualTo("edit");
    }

    // No.16 正常系: バリデーションエラーなしで保存成功の場合（register）、viewへリダイレクト＋successMessage
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

        String view = controller.save(form, bindingResult, new ExtendedModelMap(), "register", redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/jichitai-config/view/011002");
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
                .isEqualTo("自治体情報を登録しました。");
        verify(jichitaiConfigService).saveJichitaiConfig(form);
    }

    // No.16a 正常系: バリデーションエラーなしで更新成功の場合（edit）、viewへリダイレクト＋successMessage
    @Test
    void save_editモードで保存成功の場合_リダイレクトと更新successMessage() {
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

        String view = controller.save(form, bindingResult, new ExtendedModelMap(), "edit", redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/jichitai-config/view/011002");
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
                .isEqualTo("自治体情報を更新しました。");
        verify(jichitaiConfigService).saveJichitaiConfig(form);
    }

    // No.17 異常系: バリデーションエラーありの場合（register）、registerへリダイレクト＋fieldErrors
    @Test
    void save_バリデーションエラーありの場合_registerへリダイレクトとfieldErrors() {
        JichitaiConfigDto form = new JichitaiConfigDto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "configForm");
        bindingResult.rejectValue("jichitaiCd", "NotBlank", "自治体コードは必須です。");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.save(form, bindingResult, new ExtendedModelMap(), "register", redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/jichitai-config/register");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("fieldErrors");
        @SuppressWarnings("unchecked")
        var fieldErrors = (java.util.Map<String, String>) redirectAttributes.getFlashAttributes().get("fieldErrors");
        assertThat(fieldErrors).containsEntry("jichitaiCd", "自治体コードは必須です。");
    }

    // No.17a 異常系: バリデーションエラーありの場合（edit）、editへリダイレクト＋fieldErrors
    @Test
    void save_edit_バリデーションエラーありの場合_editへリダイレクトとfieldErrors() {
        JichitaiConfigDto form = new JichitaiConfigDto();
        form.setJichitaiCd("011002");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "configForm");
        bindingResult.rejectValue("name", "NotBlank", "自治体名は必須です。");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.save(form, bindingResult, new ExtendedModelMap(), "edit", redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/jichitai-config/edit/011002");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("fieldErrors");
        @SuppressWarnings("unchecked")
        var fieldErrors = (java.util.Map<String, String>) redirectAttributes.getFlashAttributes().get("fieldErrors");
        assertThat(fieldErrors).containsEntry("name", "自治体名は必須です。");
    }

    // No.18 異常系: saveJichitaiConfigで例外が発生した場合、viewへリダイレクト＋errorMessage
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

        String view = controller.save(form, bindingResult, new ExtendedModelMap(), "register", redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/jichitai-config/view/011002");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("自治体情報の保存に失敗しました: DB error");
    }

    // No.18a 異常系: 指定番号と合算指定番号が同じ場合、registerへリダイレクト＋fieldErrors
    @Test
    void save_指定番号と合算指定番号が同じ場合_registerへリダイレクトとfieldErrors() {
        JichitaiConfigDto form = new JichitaiConfigDto();
        form.setJichitaiCd("011002");
        form.setName("札幌市");
        form.setKbnName("市");
        form.setNendoStMonth("4");
        form.setNozeiShuki("1");
        form.setShiteiStChar("001");
        form.setGassanStChar("001");
        form.setUserId("user01");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "configForm");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.save(form, bindingResult, new ExtendedModelMap(), "register", redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/jichitai-config/register");
        @SuppressWarnings("unchecked")
        var fieldErrors = (java.util.Map<String, String>) redirectAttributes.getFlashAttributes().get("fieldErrors");
        assertThat(fieldErrors).containsKey("shiteiStChar").containsKey("gassanStChar");
        verify(jichitaiConfigService, never()).saveJichitaiConfig(any());
    }

    // No.19 異常系: 書き込み権限なしの場合、例外をスロー
    @Test
    void save_書き込み権限なしの場合_例外をスロー() {
        JichitaiConfigDto form = new JichitaiConfigDto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "configForm");
        doThrow(new RuntimeException("アクセス権限がありません")).when(accessChecker).checkWriteAccess(any());

        assertThatThrownBy(() -> controller.save(form, bindingResult, new ExtendedModelMap(), "register", new RedirectAttributesModelMap()))
                .isInstanceOf(RuntimeException.class);
    }
}

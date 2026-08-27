package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.dto.ShoreikinConfigDto;
import jp.lg.asp.accommodation.service.ShoreikinConfigService;
import jp.lg.asp.accommodation.util.SessionHelper;

@ExtendWith(MockitoExtension.class)
class ShoreikinConfigControllerTest {

    @Mock ShoreikinConfigService shoreikinConfigService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks ShoreikinConfigController controller;

    private MockHttpSession sessionWith(String shiteiNo) {
        MockHttpSession session = new MockHttpSession();
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setShiteiNo(shiteiNo);
        SessionHelper.saveShiteiGassan(session, dto);
        return session;
    }

    @Test
    void config_照会画面を返す() {
        MockHttpSession session = sessionWith("00100001");
        ShoreikinConfigDto dto = new ShoreikinConfigDto();
        when(shoreikinConfigService.getShoreikin("00100001", "2024")).thenReturn(dto);
        Model model = new ExtendedModelMap();

        String view = controller.config(session, "2024", model);

        assertThat(view).isEqualTo("shoreikin/shoreikinConfig");
        assertThat(model.asMap()).containsKey("configForm");
    }

    @Test
    void config_セッション未設定はモーダル表示() {
        MockHttpSession session = new MockHttpSession();
        Model model = new ExtendedModelMap();

        String view = controller.config(session, null, model);

        assertThat(view).isEqualTo("shoreikin/shoreikinConfig");
        assertThat(model.asMap()).containsKey("showShiteiGassanModal");
    }

    @Test
    void switchMode_編集モードに切り替え() {
        ShoreikinConfigDto form = new ShoreikinConfigDto();
        Model model = new ExtendedModelMap();

        String view = controller.switchMode("edit", form, model);

        assertThat(view).isEqualTo("shoreikin/shoreikinConfig");
        assertThat(form.getMode()).isEqualTo("edit");
    }

    @Test
    void switchMode_照会モードに切り替え() {
        ShoreikinConfigDto form = new ShoreikinConfigDto();
        Model model = new ExtendedModelMap();

        String view = controller.switchMode("view", form, model);

        assertThat(view).isEqualTo("shoreikin/shoreikinConfig");
        assertThat(form.getMode()).isEqualTo("view");
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
    void calculate_交付率が取得できない場合は交付率をクリアする() {
        ShoreikinConfigDto form = new ShoreikinConfigDto();
        form.setMode("edit");
        form.setKofuRitsu(new BigDecimal("10.00"));
        when(shoreikinConfigService.calculateShoreikin(form))
                .thenThrow(new IllegalStateException("交付率が設定されていません。交付率設定画面で登録してください。"));
        Model model = new ExtendedModelMap();

        String view = controller.calculate(form, model);

        assertThat(view).isEqualTo("shoreikin/shoreikinConfig");
        // 交付率が null であることが、テンプレートが誘導モーダルを描画する条件
        assertThat(form.getKofuRitsu()).isNull();
        assertThat(model.asMap()).containsEntry("configForm", form);
        // 文言はモーダル側に持たせているため、上部の alert は出さない
        assertThat(model.asMap()).doesNotContainKey("errorMessage");
    }

    @Test
    void calculate_想定外の例外は従来どおり画面上部にエラーを出す() {
        ShoreikinConfigDto form = new ShoreikinConfigDto();
        form.setKofuRitsu(new BigDecimal("10.00"));
        when(shoreikinConfigService.calculateShoreikin(form))
                .thenThrow(new RuntimeException("DB接続エラー"));
        Model model = new ExtendedModelMap();

        String view = controller.calculate(form, model);

        assertThat(view).isEqualTo("shoreikin/shoreikinConfig");
        // 交付率はクリアしない（誘導モーダルの対象ではない）
        assertThat(form.getKofuRitsu()).isEqualByComparingTo("10.00");
        assertThat(model.asMap()).containsKey("errorMessage");
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

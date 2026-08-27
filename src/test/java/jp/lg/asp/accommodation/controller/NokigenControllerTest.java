package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Nokigen;
import jp.lg.asp.accommodation.service.NokigenService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NokigenControllerTest {

    @Mock NokigenService nokigenService;
    @Mock ScreenAccessChecker accessChecker;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks NokigenController controller;

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("011002");
        Jichitai jichitai = new Jichitai();
        jichitai.setNendoStMonth("4");
        when(nokigenService.findJichitai("011002")).thenReturn(jichitai);
    }

    @Test
    void list_データあり_リダイレクト() {
        Nokigen nokigen = new Nokigen();
        nokigen.setNendo("2024");
        when(nokigenService.findAll()).thenReturn(List.of(nokigen));

        String view = controller.list(new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/admin/nokigen/view/2024");
    }

    @Test
    void list_データなし_登録画面へリダイレクト() {
        when(nokigenService.findAll()).thenReturn(List.of());

        String view = controller.list(new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/admin/nokigen/register");
    }

    @Test
    void register_登録画面を返す() {
        when(nokigenService.findAll()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.register(model);

        assertThat(view).isEqualTo("admin/nokigenConfig");
        assertThat(model.asMap()).containsKey("nokigen");
    }

    @Test
    void view_データあり_照会画面を返す() {
        Nokigen nokigen = new Nokigen();
        nokigen.setNendo("2024");
        when(nokigenService.findByNendo("2024")).thenReturn(nokigen);
        when(nokigenService.findAll()).thenReturn(List.of(nokigen));
        Model model = new ExtendedModelMap();

        String view = controller.view("2024", model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/nokigenConfig");
    }

    @Test
    void view_データなし_リダイレクト() {
        when(nokigenService.findByNendo("9999")).thenReturn(null);

        String view = controller.view("9999", new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/admin/nokigen/register");
    }

    @Test
    void save_正常登録() {
        Nokigen nokigen = new Nokigen();
        nokigen.setNendo("2024");
        when(nokigenService.existsByNendo("2024")).thenReturn(false);
        when(nokigenService.findAll()).thenReturn(List.of(nokigen));
        Model model = new ExtendedModelMap();

        String view = controller.save(nokigen, "register", model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/admin/nokigen/view/2024");
        verify(nokigenService).save(nokigen);
    }

    @Test
    void save_年度空はバリデーションエラー() {
        Nokigen nokigen = new Nokigen();
        nokigen.setNendo("");
        when(nokigenService.findAll()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.save(nokigen, "register", model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/nokigenConfig");
        assertThat(model.asMap()).containsKey("validationErrors");
    }

    @Test
    void exists_存在チェック() {
        when(nokigenService.existsByNendo("2024")).thenReturn(true);

        var response = controller.exists("2024");

        assertThat(response.getBody()).containsEntry("exists", true);
    }
}

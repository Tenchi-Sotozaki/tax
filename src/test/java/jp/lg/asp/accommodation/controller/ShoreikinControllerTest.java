package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.ShoreikinDto;
import jp.lg.asp.accommodation.service.ShoreikinService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShoreikinControllerTest {

    @Mock ShoreikinService shoreikinService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks ShoreikinController controller;

    @BeforeEach
    void setUp() {
        when(shoreikinService.search(any())).thenReturn(
                new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
    }

    @Test
    void list_初期表示はitemsなし() {
        Model model = new ExtendedModelMap();

        String view = controller.list(new ShoreikinDto(), model, null);

        assertThat(view).isEqualTo("shoreikin/shoreikin");
        assertThat(model.asMap()).doesNotContainKey("items");
    }

    @Test
    void list_検索後はitemsあり() {
        Model model = new ExtendedModelMap();

        String view = controller.list(new ShoreikinDto(), model, "true");

        assertThat(view).isEqualTo("shoreikin/shoreikin");
        assertThat(model.asMap()).containsKey("items");
    }

    @Test
    void search_検索後一覧画面を返す() {
        Model model = new ExtendedModelMap();
        ShoreikinDto form = new ShoreikinDto();
        form.setPage(5);

        String view = controller.search(form, model);

        assertThat(view).isEqualTo("shoreikin/shoreikin");
        assertThat(form.getPage()).isEqualTo(0); // ページリセット確認
    }

    @Test
    void viewKofu_未選択はエラー() {
        String view = controller.viewKofu(List.of(), null, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/shoreikin/list");
    }

    @Test
    void viewKofu_複数選択はエラー() {
        String view = controller.viewKofu(List.of("A", "B"), "2024", new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/shoreikin/list");
    }

    @Test
    void viewKofu_単一選択はリダイレクト() {
        String view = controller.viewKofu(List.of("00100001"), "2024", new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/shoreikin/config");
    }

    @Test
    void viewKoza_単一選択はリダイレクト() {
        String view = controller.viewKoza(List.of("00100001"), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/shoreikin/furikomiKoza");
    }
}

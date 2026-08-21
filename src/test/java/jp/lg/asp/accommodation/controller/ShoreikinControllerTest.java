package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.ShoreikinDto;
import jp.lg.asp.accommodation.service.ShoreikinService;
import jp.lg.asp.accommodation.util.SessionHelper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShoreikinControllerTest {

    @Mock ShoreikinService shoreikinService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks ShoreikinController controller;

    @BeforeEach
    void setUp() {
        when(shoreikinService.search(any())).thenReturn(List.of());
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

        String view = controller.search(form, model);

        assertThat(view).isEqualTo("shoreikin/shoreikin");
        assertThat(model.asMap()).containsKey("items");
        assertThat(model.asMap()).containsKey("searchForm");
    }

    @Test
	void viewKofu_未選択はエラー() {
		MockHttpSession session = new MockHttpSession();
		String view = controller.viewKofu(List.of(), null, null, null, session, new RedirectAttributesModelMap());

		assertThat(view).isEqualTo("redirect:/shoreikin/list");
	}

	@Test
	void viewKofu_複数選択はエラー() {
		MockHttpSession session = new MockHttpSession();
		String view = controller.viewKofu(List.of("A", "B"), null, null, "2024", session, new RedirectAttributesModelMap());

		assertThat(view).isEqualTo("redirect:/shoreikin/list");
	}

	@Test
	void viewKofu_単一選択はリダイレクト() {
		MockHttpSession session = new MockHttpSession();
		String view = controller.viewKofu(List.of("00100001"), "テスト施設", "テスト太郎", "2024", session, new RedirectAttributesModelMap());

		assertThat(view).isEqualTo("redirect:/shoreikin/config");
		assertThat(SessionHelper.getShiteiNo(session)).isEqualTo("00100001");
	}

	@Test
	void viewKofu_nendoなしはリダイレクトパラメータなし() {
		MockHttpSession session = new MockHttpSession();
		RedirectAttributesModelMap redirectAttrs = new RedirectAttributesModelMap();
		String view = controller.viewKofu(List.of("00100001"), "テスト施設", "テスト太郎", null, session, redirectAttrs);

		assertThat(view).isEqualTo("redirect:/shoreikin/config");
		assertThat(redirectAttrs.asMap()).doesNotContainKey("nendo");
	}

	@Test
	void viewKoza_未選択はエラー() {
		MockHttpSession session = new MockHttpSession();
		String view = controller.viewKoza(List.of(), null, null, session, new RedirectAttributesModelMap());

		assertThat(view).isEqualTo("redirect:/shoreikin/list");
	}

	@Test
	void viewKoza_複数選択はエラー() {
		MockHttpSession session = new MockHttpSession();
		String view = controller.viewKoza(List.of("A", "B"), null, null, session, new RedirectAttributesModelMap());

		assertThat(view).isEqualTo("redirect:/shoreikin/list");
	}

	@Test
	void viewKoza_単一選択はリダイレクト() {
		MockHttpSession session = new MockHttpSession();
		String view = controller.viewKoza(List.of("00100001"), "テスト施設", "テスト太郎", session, new RedirectAttributesModelMap());

		assertThat(view).isEqualTo("redirect:/shoreikin/furikomiKoza");
		assertThat(SessionHelper.getShiteiNo(session)).isEqualTo("00100001");
	}
}

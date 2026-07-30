package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.SelectedJigyoshaResolver;
import jp.lg.asp.accommodation.dto.TokugimuForm;
import jp.lg.asp.accommodation.dto.TokugimuListItem;
import jp.lg.asp.accommodation.dto.TokugimuSearchForm;
import jp.lg.asp.accommodation.service.NozeiShukiService;
import jp.lg.asp.accommodation.service.TokugimuService;
import jp.lg.asp.accommodation.util.SessionHelper;

@ExtendWith(MockitoExtension.class)
class TokugimuControllerTest {

    @Mock TokugimuService tokugimuService;
    @Mock NozeiShukiService nozeiShukiService;
    @Mock ScreenAccessChecker accessChecker;
    @Mock SelectedJigyoshaResolver selectedJigyoshaResolver;

    @InjectMocks TokugimuController controller;

    @Test
    void list_検索後は一覧を表示する() {
        Page<TokugimuListItem> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(tokugimuService.search(any())).thenReturn(page);
        Model model = new ExtendedModelMap();

        String view = controller.list(new TokugimuSearchForm(), 0, 10, true, model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuDaicho");
        assertThat(model.asMap()).containsKey("items");
        assertThat(model.asMap()).containsEntry("isSearched", true);
    }

    @Test
    void list_初期表示時は検索しない() {
        Model model = new ExtendedModelMap();

        String view = controller.list(new TokugimuSearchForm(), 0, 10, false, model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuDaicho");
        assertThat(model.asMap()).containsEntry("isSearched", false);
        // 初期表示時は検索処理を行わない
        verify(tokugimuService, never()).search(any());
    }

    @Test
    void showRegistrationForm_登録画面を返す() {
        Model model = new ExtendedModelMap();

        String view = controller.showRegistrationForm(model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuConfig");
        assertThat(model.asMap()).containsEntry("isEdit", false);
    }

    @Test
    void register_バリデーションエラー() {
        TokugimuForm form = new TokugimuForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "TokugimuForm");
        bindingResult.rejectValue("name", "NotBlank", "必須です");
        Model model = new ExtendedModelMap();

        String view = controller.register(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("tokugimu/tTokugimuConfig");
    }

    @Test
    void register_正常登録() {
        TokugimuForm form = new TokugimuForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "TokugimuForm");
        Model model = new ExtendedModelMap();

        String view = controller.register(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/tokugimu/list");
        verify(tokugimuService).register(form);
    }

    @Test
    void showView_照会画面を返す() {
        TokugimuForm form = new TokugimuForm();
        when(tokugimuService.getTokugimuByShiteiNo("00100001")).thenReturn(form);
        Model model = new ExtendedModelMap();
        MockHttpSession session = new MockHttpSession();

        String view = controller.showView("00100001", null, session, model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuConfig");
        assertThat(model.asMap()).containsEntry("isView", true);
        // 帳票発行画面が参照するセッションに、表示中の特別徴収義務者が格納されること
        assertThat(SessionHelper.getShiteiGassan(session)).isNotNull();
    }

    @Test
    void delete_削除後リダイレクト() {
        String view = controller.delete("00100001", new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/tokugimu/list");
        verify(tokugimuService).deleteByShiteiNo("00100001");
    }
}

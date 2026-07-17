package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.NozeiShukiDto;
import jp.lg.asp.accommodation.entity.NozeiShuki;
import jp.lg.asp.accommodation.service.NozeiShukiService;

@ExtendWith(MockitoExtension.class)
class NozeiShukiControllerTest {

    @Mock NozeiShukiService nozeiShukiService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks NozeiShukiController controller;

    @Test
    void index_一覧画面を返す() {
        when(nozeiShukiService.findAll()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.index(model);

        assertThat(view).isEqualTo("admin/nozeiShukiDaicho");
        assertThat(model.asMap()).containsKey("nozeiShukiList");
    }

    @Test
    void search_JSONレスポンスを返す() {
        when(nozeiShukiService.findByShuki(12)).thenReturn(List.of(new NozeiShukiDto(BigDecimal.ONE, BigDecimal.valueOf(12))));

        ResponseEntity<Map<String, Object>> response = controller.search(12);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsKey("data");
    }

    @Test
    void register_登録画面を返す() {
        Model model = new ExtendedModelMap();

        String view = controller.register(model);

        assertThat(view).isEqualTo("admin/nozeiShukiConfig");
    }

    @Test
    void edit_データあり_編集画面を返す() {
        NozeiShuki nozeiShuki = new NozeiShuki();
        nozeiShuki.setShuki(BigDecimal.valueOf(12));
        when(nozeiShukiService.findBySeq(BigDecimal.ONE)).thenReturn(nozeiShuki);
        Model model = new ExtendedModelMap();

        String view = controller.edit(BigDecimal.ONE, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/nozeiShukiConfig");
    }

    @Test
    void edit_データなし_リダイレクト() {
        when(nozeiShukiService.findBySeq(BigDecimal.ONE)).thenReturn(null);

        String view = controller.edit(BigDecimal.ONE, new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/admin/nozei-shuki/list");
    }

    @Test
    void save_正常登録() {
        NozeiShuki nozeiShuki = new NozeiShuki();
        nozeiShuki.setShuki(BigDecimal.valueOf(12));
        when(nozeiShukiService.existsByShuki(BigDecimal.valueOf(12))).thenReturn(false);

        String view = controller.save(nozeiShuki, new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/admin/nozei-shuki/list");
        verify(nozeiShukiService).save(nozeiShuki);
    }

    @Test
    void save_周期空はバリデーションエラー() {
        NozeiShuki nozeiShuki = new NozeiShuki();
        nozeiShuki.setShuki(null);
        Model model = new ExtendedModelMap();

        String view = controller.save(nozeiShuki, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/nozeiShukiConfig");
        assertThat(model.asMap()).containsKey("validationErrors");
    }

    @Test
    void delete_正常削除() {
        String view = controller.delete(BigDecimal.ONE, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/admin/nozei-shuki/list");
        verify(nozeiShukiService).delete(BigDecimal.ONE);
    }
}

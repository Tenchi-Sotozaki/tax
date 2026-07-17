package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
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
import jp.lg.asp.accommodation.dto.ZeiritsuForm;
import jp.lg.asp.accommodation.dto.ZeiritsuSearchForm;
import jp.lg.asp.accommodation.entity.Zeiritsu;
import jp.lg.asp.accommodation.entity.ZeiritsuId;
import jp.lg.asp.accommodation.repository.ZeiritsuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeigakuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeiritsuRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ZeiritsuControllerTest {

    @Mock ZeiritsuRepository zeiritsuRepository;
    @Mock ZeiritsuTeigakuRepository zeiritsuTeigakuRepository;
    @Mock ZeiritsuTeiritsuRepository zeiritsuTeiritsuRepository;
    @Mock ScreenAccessChecker accessChecker;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks ZeiritsuController controller;

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("011002");
        when(zeiritsuRepository.findActiveByJichitaiCd("011002")).thenReturn(List.of());
    }

    @Test
    void list_一覧画面を返す() {
        Model model = new ExtendedModelMap();

        String view = controller.list(new ZeiritsuSearchForm(), model);

        assertThat(view).isEqualTo("admin/zeiritsuDaicho");
        assertThat(model.asMap()).containsKey("items");
    }

    @Test
    void view_照会画面を返す() {
        Zeiritsu z = new Zeiritsu();
        z.setJichitaiCd("011002");
        z.setSeq(BigDecimal.ONE);
        z.setFukaKbn("1");
        z.setTaishoKbn("1");
        z.setTekiyoStYm("202401");
        when(zeiritsuRepository.findById(new ZeiritsuId("011002", BigDecimal.ONE)))
                .thenReturn(Optional.of(z));
        when(zeiritsuTeigakuRepository.findActiveBySeq("011002", BigDecimal.ONE)).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.view(1L, model);

        assertThat(view).isEqualTo("admin/zeiritsuConfig");
        assertThat(model.asMap()).containsEntry("isView", true);
    }

    @Test
    void showForm_登録画面を返す() {
        Model model = new ExtendedModelMap();

        String view = controller.showForm(model);

        assertThat(view).isEqualTo("admin/zeiritsuConfig");
        assertThat(model.asMap()).containsEntry("isEdit", false);
    }

    @Test
    void delete_論理削除後リダイレクト() {
        Zeiritsu z = new Zeiritsu();
        z.setJichitaiCd("011002");
        z.setSeq(BigDecimal.ONE);
        z.setFukaKbn("1");
        z.setDelFlg("0");
        when(zeiritsuRepository.findById(new ZeiritsuId("011002", BigDecimal.ONE)))
                .thenReturn(Optional.of(z));
        when(zeiritsuTeigakuRepository.findActiveBySeq("011002", BigDecimal.ONE)).thenReturn(List.of());

        String view = controller.delete(1L, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/admin/zeiritsu/list");
        assertThat(z.getDelFlg()).isEqualTo("1");
    }
}

package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
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
import org.springframework.validation.BeanPropertyBindingResult;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.ShoreikinBulkDto;
import jp.lg.asp.accommodation.service.ShoreikinBulkService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShoreikinBulkControllerTest {

    @Mock ShoreikinBulkService shoreikinBulkService;
    @Mock ScreenAccessChecker accessChecker;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks ShoreikinBulkController controller;

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("011002");
        when(shoreikinBulkService.findKofuRitsuList(eq("011002"), any(Integer.class)))
                .thenReturn(List.of(BigDecimal.valueOf(50)));
    }

    @Test
    void bulk_初期表示() {
        Model model = new ExtendedModelMap();

        String view = controller.bulk("2024", model);

        assertThat(view).isEqualTo("shoreikin/shoreikinBulk");
        assertThat(model.asMap()).containsKey("bulkForm");
    }

    @Test
    void executeBulk_バリデーションエラー() {
        ShoreikinBulkDto form = new ShoreikinBulkDto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "bulkForm");
        bindingResult.rejectValue("nendo", "NotBlank", "必須です");
        Model model = new ExtendedModelMap();

        String view = controller.executeBulk(form, bindingResult, model);

        assertThat(view).isEqualTo("shoreikin/shoreikinBulk");
        verifyNoMoreInteractions(shoreikinBulkService);
    }

    @Test
    void executeBulk_正常実行() {
        ShoreikinBulkDto form = new ShoreikinBulkDto();
        form.setNendo("2024");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "bulkForm");
        ShoreikinBulkDto result = new ShoreikinBulkDto();
        result.setFailureCount(0);
        when(shoreikinBulkService.executeBulkSanshutsu(form)).thenReturn(result);
        Model model = new ExtendedModelMap();

        String view = controller.executeBulk(form, bindingResult, model);

        assertThat(view).isEqualTo("shoreikin/shoreikinBulk");
        assertThat(model.asMap()).containsKey("bulkForm");
    }

    @Test
    void executeBulk_失敗あり() {
        ShoreikinBulkDto form = new ShoreikinBulkDto();
        form.setNendo("2024");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "bulkForm");
        ShoreikinBulkDto result = new ShoreikinBulkDto();
        result.setFailureCount(2);
        when(shoreikinBulkService.executeBulkSanshutsu(form)).thenReturn(result);
        Model model = new ExtendedModelMap();

        String view = controller.executeBulk(form, bindingResult, model);

        assertThat(view).isEqualTo("shoreikin/shoreikinBulk");
        assertThat(model.asMap()).containsKey("bulkForm");
    }
}

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
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.ShunoDto;
import jp.lg.asp.accommodation.service.ShunoRenkeiService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShunoRenkeiControllerTest {

    @Mock ScreenAccessChecker accessChecker;
    @Mock ShunoRenkeiService shunoRenkeiService;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks ShunoRenkeiController controller;

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("011002");
        when(shunoRenkeiService.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
    }

    @Test
    void index_一覧画面を返す() {
        Model model = new ExtendedModelMap();

        String view = controller.index(null, null, null, null, null, "partial", 0, 10, null, model);

        assertThat(view).isEqualTo("renkei/shunoRenkei");
        assertThat(model.asMap()).containsKey("searchForm");
    }

    @Test
    void search_JSONレスポンスを返す() {
        List<ShunoDto> result = controller.search(null, null, null, null, null, "partial");

        assertThat(result).isNotNull();
    }

    @Test
    void downloadCsv_CSVを返す() {
        when(shunoRenkeiService.findByKeys(any(), any())).thenReturn(List.of());

        ResponseEntity<byte[]> response = controller.downloadCsv(List.of());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).isNotNull();
    }
}

package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.entity.EltaxRenkei;
import jp.lg.asp.accommodation.entity.Nokigen;
import jp.lg.asp.accommodation.service.EltaxRenkeiService;
import jp.lg.asp.accommodation.service.NokigenService;

@ExtendWith(MockitoExtension.class)
class EltaxRenkeiControllerTest {

    @Mock EltaxRenkeiService eltaxRenkeiService;
    @Mock ScreenAccessChecker accessChecker;
    @Mock NokigenService nokigenService;

    @InjectMocks EltaxRenkeiController controller;

    @Test
    void index_一覧画面を返す() {
        Model model = new ExtendedModelMap();
        when(nokigenService.findAll()).thenReturn(List.of(new Nokigen()));
        when(eltaxRenkeiService.findAll()).thenReturn(List.of());

        String view = controller.index(model);

        assertThat(view).isEqualTo("eltaxRenkei/eltaxRenkei");
        assertThat(model.asMap()).containsKey("eltaxRenkeiList");
        assertThat(model.asMap()).doesNotContainKey("errorMessage");
    }

    @Test
    void download_存在する場合はファイルを返す() {
        EltaxRenkei entity = new EltaxRenkei();
        entity.setFileName("test.csv");
        entity.setLog(new byte[]{1, 2, 3});
        when(eltaxRenkeiService.findBySeq(BigDecimal.ONE)).thenReturn(entity);

        ResponseEntity<byte[]> response = controller.download(BigDecimal.ONE);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(new byte[]{1, 2, 3});
    }

    @Test
    void download_存在しない場合は404() {
        when(eltaxRenkeiService.findBySeq(BigDecimal.ONE)).thenReturn(null);

        ResponseEntity<byte[]> response = controller.download(BigDecimal.ONE);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }
}

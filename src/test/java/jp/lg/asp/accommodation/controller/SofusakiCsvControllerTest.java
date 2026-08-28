package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.SofusakiCsvDto;
import jp.lg.asp.accommodation.service.SofusakiCsvService;

@ExtendWith(MockitoExtension.class)
class SofusakiCsvControllerTest {

    @Mock SofusakiCsvService sofusakiCsvService;
    @Mock ScreenAccessChecker accessChecker;
    @InjectMocks SofusakiCsvController controller;

    private static final String SCREEN_ID = ScreenManagement.SOFUSAKI_CSV;

    private SofusakiCsvDto dto(String name) {
        SofusakiCsvDto d = new SofusakiCsvDto();
        d.setAtenaNo(BigDecimal.ONE);
        d.setSoufusakiName(name);
        return d;
    }

    // ── init ──────────────────────────────────────────────────────

    @Test
    void init_一覧がモデルに設定され画面を返す() {
        SofusakiCsvDto dto1 = dto("山田太郎");
        when(sofusakiCsvService.findAll()).thenReturn(List.of(dto1));
        Model model = new ExtendedModelMap();

        String view = controller.init(model);

        assertThat(view).isEqualTo("renkei/sofusakiCsv");
        assertThat((List<?>) model.asMap().get("items")).hasSize(1);
        verify(accessChecker).checkAccess(SCREEN_ID);
    }

    // ── download ──────────────────────────────────────────────────

    @Test
    void download_selectedIndexesがnullの場合_全件をCSV出力する() throws IOException {
        SofusakiCsvDto dto1 = dto("山田");
        SofusakiCsvDto dto2 = dto("鈴木");
        when(sofusakiCsvService.findAll()).thenReturn(List.of(dto1, dto2));
        when(sofusakiCsvService.toCsvString(List.of(dto1, dto2))).thenReturn("csv");

        ResponseEntity<byte[]> response = controller.download(null);

        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] csvBytes = "csv".getBytes(StandardCharsets.UTF_8);
        byte[] expected = new byte[bom.length + csvBytes.length];
        System.arraycopy(bom, 0, expected, 0, bom.length);
        System.arraycopy(csvBytes, 0, expected, bom.length, csvBytes.length);

        assertThat(response.getBody()).isEqualTo(expected);
        assertThat(response.getHeaders().getFirst("Content-Disposition"))
                .isEqualTo("attachment; filename=\"sofusaki.csv\"");
        verify(accessChecker).checkAccess(SCREEN_ID);
    }

    @Test
    void download_selectedIndexesが空の場合_全件をCSV出力する() throws IOException {
        SofusakiCsvDto dto1 = dto("山田");
        SofusakiCsvDto dto2 = dto("鈴木");
        when(sofusakiCsvService.findAll()).thenReturn(List.of(dto1, dto2));
        when(sofusakiCsvService.toCsvString(List.of(dto1, dto2))).thenReturn("csv");

        controller.download(List.of());

        verify(sofusakiCsvService).toCsvString(List.of(dto1, dto2));
        verify(accessChecker).checkAccess(SCREEN_ID);
    }

    @Test
    void download_selectedIndexesが指定された場合_該当インデックスのみCSV出力する() throws IOException {
        SofusakiCsvDto dto1 = dto("山田");
        SofusakiCsvDto dto2 = dto("鈴木");
        SofusakiCsvDto dto3 = dto("田中");
        when(sofusakiCsvService.findAll()).thenReturn(List.of(dto1, dto2, dto3));
        when(sofusakiCsvService.toCsvString(List.of(dto2))).thenReturn("csv");

        controller.download(List.of(1));

        verify(sofusakiCsvService).toCsvString(List.of(dto2));
    }

    @Test
    void download_範囲外インデックスが含まれる場合_範囲内のみCSV出力する() throws IOException {
        SofusakiCsvDto dto1 = dto("山田");
        SofusakiCsvDto dto2 = dto("鈴木");
        when(sofusakiCsvService.findAll()).thenReturn(List.of(dto1, dto2));
        when(sofusakiCsvService.toCsvString(List.of(dto1))).thenReturn("csv");

        controller.download(List.of(0, 99));

        verify(sofusakiCsvService).toCsvString(List.of(dto1));
    }
}

package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.SofusakiCsvDto;
import jp.lg.asp.accommodation.service.SofusakiCsvService;

/**
 * 送付先情報CSV出力 単体テスト（コントローラ）
 *
 * <p>チェックリスト「送付先情報CSV出力_単体テストチェックリスト.xlsx」の #1〜#5 に1対1で対応する。
 * チェックリストはあるべき仕様で書かれている。テストが通るように期待値を実装へ寄せないこと。</p>
 */
@ExtendWith(MockitoExtension.class)
class SofusakiCsvControllerTest {

    @Mock SofusakiCsvService sofusakiCsvService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks SofusakiCsvController controller;

    @Captor ArgumentCaptor<List<SofusakiCsvDto>> targetsCaptor;

    private static final byte[] BOM = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

    /** テスト用DTOを生成する */
    private SofusakiCsvDto dto(String shiteiNo) {
        SofusakiCsvDto dto = new SofusakiCsvDto();
        dto.setShiteiNo(shiteiNo);
        return dto;
    }

    // ------------------------------------------------------------------
    // #1 init
    // ------------------------------------------------------------------

    @Test
    @DisplayName("#1 init 正常系 初期表示：一覧がモデルに設定され画面を返す")
    void init_初期表示で一覧がモデルに設定される() {
        SofusakiCsvDto dto1 = dto("0001");
        List<SofusakiCsvDto> items = List.of(dto1);
        when(sofusakiCsvService.findAll()).thenReturn(items);

        Model model = new ExtendedModelMap();

        String view = controller.init(model);

        assertThat(view).isEqualTo("renkei/sofusakiCsv");
        assertThat(model.asMap()).containsEntry("items", items);
        verify(sofusakiCsvService, times(1)).findAll();
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.SOFUSAKI_CSV);
    }

    // ------------------------------------------------------------------
    // #2・#3 download 未選択
    // ------------------------------------------------------------------

    @Test
    @DisplayName("#2 download 異常系 selectedIndexesがnullの場合：エラーとなる（未選択でのダウンロードは不可）")
    void download_selectedIndexesがnullはエラー() throws IOException {
        assertUnselectedIsError(null);
    }

    @Test
    @DisplayName("#3 download 異常系 selectedIndexesが空の場合：エラーとなる（未選択でのダウンロードは不可）")
    void download_selectedIndexesが空はエラー() throws IOException {
        assertUnselectedIsError(List.of());
    }

    /**
     * 未選択でのダウンロードがエラーとなり、一覧取得・CSV生成が行われないことを検証する。
     * ACCOMMODATION_TAX-539 で「400 + メッセージ本文」を返す形に確定した。
     */
    private void assertUnselectedIsError(List<Integer> selectedIndexes) throws IOException {
        ResponseEntity<byte[]> response = controller.download(selectedIndexes);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(new String(response.getBody(), StandardCharsets.UTF_8))
                .isEqualTo("出力対象が選択されていません。");
        verify(sofusakiCsvService, never()).findAll();
        verify(sofusakiCsvService, never()).toCsvString(anyList());
    }

    // ------------------------------------------------------------------
    // #4・#5 download
    // ------------------------------------------------------------------

    @Test
    @DisplayName("#4 download 正常系 selectedIndexesが指定された場合：該当インデックスのみCSV出力する")
    void download_指定インデックスのみCSV出力する() throws IOException {
        SofusakiCsvDto dto1 = dto("0001");
        SofusakiCsvDto dto2 = dto("0002");
        SofusakiCsvDto dto3 = dto("0003");
        when(sofusakiCsvService.findAll()).thenReturn(List.of(dto1, dto2, dto3));
        when(sofusakiCsvService.toCsvString(anyList())).thenReturn("csv");

        ResponseEntity<byte[]> response = controller.download(List.of(1));

        verify(sofusakiCsvService, times(1)).findAll();
        verify(sofusakiCsvService).toCsvString(targetsCaptor.capture());
        assertThat(targetsCaptor.getValue()).containsExactly(dto2);

        byte[] body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(Arrays.copyOfRange(body, 0, 3))
                .as("先頭3バイトがBOM(EF BB BF)であること")
                .isEqualTo(BOM);
        assertThat(Arrays.copyOfRange(body, 3, body.length))
                .as("BOMに続くバイト列が\"csv\"のUTF-8であること")
                .isEqualTo("csv".getBytes(StandardCharsets.UTF_8));

        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"sofusaki.csv\"");
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.SOFUSAKI_CSV);
    }

    @Test
    @DisplayName("#5 download 異常系 selectedIndexesに範囲外インデックスが含まれる場合：範囲内のみCSV出力する")
    void download_範囲外インデックスは無視される() throws IOException {
        SofusakiCsvDto dto1 = dto("0001");
        SofusakiCsvDto dto2 = dto("0002");
        when(sofusakiCsvService.findAll()).thenReturn(List.of(dto1, dto2));
        when(sofusakiCsvService.toCsvString(anyList())).thenReturn("csv");

        ResponseEntity<byte[]> response = controller.download(List.of(0, 2));

        verify(sofusakiCsvService, times(1)).findAll();
        verify(sofusakiCsvService).toCsvString(targetsCaptor.capture());
        assertThat(targetsCaptor.getValue())
                .as("範囲外のindex=2は無視され、dto1のみが渡されること")
                .containsExactly(dto1);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}

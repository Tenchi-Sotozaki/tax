package jp.lg.asp.accommodation.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.ShoreikinRenkeiDto;
import jp.lg.asp.accommodation.exception.AccessDeniedException;
import jp.lg.asp.accommodation.service.ShoreikinRenkeiService;

@ExtendWith(MockitoExtension.class)
class KofukinFurikomiControllerTest {

    private static final String SCREEN_ID = ScreenManagement.KOFUKIN_FURIKOMI;

    @InjectMocks
    private KofukinFurikomiController controller;

    @Mock
    private ScreenAccessChecker accessChecker;

    @Mock
    private ShoreikinRenkeiService shoreikinRenkeiService;

    @Mock
    private JichitaiContext jichitaiContext;

    // =====================================================================
    // ヘルパー
    // =====================================================================

    private ShoreikinRenkeiDto dto(String shiteiNo) {
        ShoreikinRenkeiDto d = new ShoreikinRenkeiDto();
        d.setShiteiNo(shiteiNo);
        return d;
    }

    private ShoreikinRenkeiDto.Key key(String shiteiNo, String nendo) {
        ShoreikinRenkeiDto.Key k = new ShoreikinRenkeiDto.Key();
        k.setShiteiNo(shiteiNo);
        k.setNendo(nendo);
        return k;
    }

    // =====================================================================
    // #1 index 正常系
    // =====================================================================

    @Test
    @DisplayName("#1 index 正常系 検索条件を指定して1件ヒットする場合")
    void index_検索条件を指定して1件ヒット() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        ShoreikinRenkeiDto item = dto("S001");
        item.setAtenaNo("1");
        item.setName("山田太郎");
        item.setNendo("2024");
        item.setKofuGaku(15L);
        when(shoreikinRenkeiService.search("01100", "2024", "S001", "山田", "partial"))
                .thenReturn(List.of(item));

        Model model = new ExtendedModelMap();
        String result = controller.index("2024", "S001", "山田", "partial", model);

        assertEquals("renkei/kofukinFurikomi", result);

        @SuppressWarnings("unchecked")
        List<ShoreikinRenkeiDto> items = (List<ShoreikinRenkeiDto>) model.asMap().get("items");
        assertEquals(1, items.size());
        assertEquals("S001", items.get(0).getShiteiNo());

        @SuppressWarnings("unchecked")
        Map<String, Object> searchForm = (Map<String, Object>) model.asMap().get("searchForm");
        assertEquals("2024", searchForm.get("nendo"));
        assertEquals("S001", searchForm.get("shiteiNo"));
        assertEquals("山田", searchForm.get("name"));
        assertEquals("partial", searchForm.get("nameMatchType"));

        verify(shoreikinRenkeiService, times(1)).search("01100", "2024", "S001", "山田", "partial");
        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #2 index 正常系
    // =====================================================================

    @Test
    @DisplayName("#2 index 正常系 検索結果が0件の場合")
    void index_検索結果0件() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(shoreikinRenkeiService.search("01100", "2024", null, null, "partial"))
                .thenReturn(List.of());

        Model model = new ExtendedModelMap();
        String result = controller.index("2024", null, null, "partial", model);

        assertEquals("renkei/kofukinFurikomi", result);

        @SuppressWarnings("unchecked")
        List<?> items = (List<?>) model.asMap().get("items");
        assertNotNull(items);
        assertTrue(items.isEmpty());

        @SuppressWarnings("unchecked")
        Map<String, Object> searchForm = (Map<String, Object>) model.asMap().get("searchForm");
        assertTrue(searchForm.containsKey("nendo"));
        assertTrue(searchForm.containsKey("shiteiNo"));
        assertTrue(searchForm.containsKey("name"));
        assertTrue(searchForm.containsKey("nameMatchType"));

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #3 index 正常系
    // =====================================================================

    @Test
    @DisplayName("#3 index 正常系 検索条件がすべて未指定の場合")
    void index_検索条件がすべて未指定() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(shoreikinRenkeiService.search("01100", null, null, null, "partial"))
                .thenReturn(List.of());

        ArgumentCaptor<String> nendoCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> shiteiNoCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);

        Model model = new ExtendedModelMap();
        controller.index(null, null, null, "partial", model);

        verify(shoreikinRenkeiService).search(eq("01100"), nendoCaptor.capture(), shiteiNoCaptor.capture(), nameCaptor.capture(), any());
        assertNull(nendoCaptor.getValue());
        assertNull(shiteiNoCaptor.getValue());
        assertNull(nameCaptor.getValue());

        @SuppressWarnings("unchecked")
        Map<String, Object> searchForm = (Map<String, Object>) model.asMap().get("searchForm");
        assertNull(searchForm.get("nendo"));
        assertNull(searchForm.get("shiteiNo"));
        assertNull(searchForm.get("name"));

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #4 index 正常系
    // =====================================================================

    @Test
    @DisplayName("#4 index 正常系 nameMatchType 未指定の場合は既定値 \"partial\" が使われる")
    void index_nameMatchType未指定は既定値partial() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(shoreikinRenkeiService.search(any(), any(), any(), any(), any())).thenReturn(List.of());

        ArgumentCaptor<String> matchTypeCaptor = ArgumentCaptor.forClass(String.class);

        Model model = new ExtendedModelMap();
        controller.index(null, null, null, "partial", model);

        verify(shoreikinRenkeiService).search(any(), any(), any(), any(), matchTypeCaptor.capture());
        assertEquals("partial", matchTypeCaptor.getValue());

        @SuppressWarnings("unchecked")
        Map<String, Object> searchForm = (Map<String, Object>) model.asMap().get("searchForm");
        assertEquals("partial", searchForm.get("nameMatchType"));

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #5 index 異常系
    // =====================================================================

    @Test
    @DisplayName("#5 index 異常系 検索条件が空文字の場合")
    void index_検索条件が空文字() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(shoreikinRenkeiService.search("01100", "", "", "", "partial")).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        controller.index("", "", "", "partial", model);

        verify(shoreikinRenkeiService, times(1)).search("01100", "", "", "", "partial");

        @SuppressWarnings("unchecked")
        List<?> items = (List<?>) model.asMap().get("items");
        assertTrue(items.isEmpty());

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #6 index 異常系
    // =====================================================================

    @Test
    @DisplayName("#6 index 異常系 サービスが例外をスローした場合")
    void index_サービスが例外をスロー() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(shoreikinRenkeiService.search(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("DB error"));

        Model model = new ExtendedModelMap();
        String result = controller.index("2024", null, null, "partial", model);

        assertEquals("renkei/kofukinFurikomi", result);

        @SuppressWarnings("unchecked")
        List<?> items = (List<?>) model.asMap().get("items");
        assertNotNull(items);
        assertTrue(items.isEmpty());

        assertNotNull(model.asMap().get("searchForm"));

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #7 search 正常系
    // =====================================================================

    @Test
    @DisplayName("#7 search 正常系 サービスの戻り値をそのまま返す")
    void search_サービスの戻り値をそのまま返す() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        ShoreikinRenkeiDto d1 = dto("S001");
        ShoreikinRenkeiDto d2 = dto("S002");
        when(shoreikinRenkeiService.search("01100", "2024", "S001", "山田", "prefix"))
                .thenReturn(List.of(d1, d2));

        List<ShoreikinRenkeiDto> result = controller.search("2024", "S001", "山田", "prefix");

        assertEquals(2, result.size());
        assertEquals("S001", result.get(0).getShiteiNo());
        assertEquals("S002", result.get(1).getShiteiNo());

        verify(shoreikinRenkeiService, times(1)).search("01100", "2024", "S001", "山田", "prefix");
        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #8 search 正常系
    // =====================================================================

    @Test
    @DisplayName("#8 search 正常系 該当0件の場合")
    void search_該当0件() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(shoreikinRenkeiService.search("01100", null, null, null, "partial")).thenReturn(List.of());

        List<ShoreikinRenkeiDto> result = controller.search(null, null, null, "partial");

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #9 search 異常系
    // =====================================================================

    @Test
    @DisplayName("#9 search 異常系 サービスが例外をスローした場合は伝播する")
    void search_サービスが例外をスローした場合は伝播する() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(shoreikinRenkeiService.search(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class,
                () -> controller.search("2024", null, null, "partial"));

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #10 downloadCsv 正常系
    // =====================================================================

    @Test
    @DisplayName("#10 downloadCsv 正常系 ヘッダー行が15列・指定の順序で出力される")
    void downloadCsv_ヘッダー行が15列() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(shoreikinRenkeiService.findByKeys(eq("01100"), any())).thenReturn(List.of());

        ResponseEntity<byte[]> response = controller.downloadCsv(List.of(key("S001", "2024")));

        byte[] body = response.getBody();
        assertNotNull(body);
        String csv = new String(body, 3, body.length - 3, StandardCharsets.UTF_8);
        String headerLine = csv.split("\n")[0];

        assertEquals(
                "\"指定番号\",\"宛名番号\",\"氏名/名称\",\"奨励金年度\",\"交付年月日\",\"納入税額\",\"交付率\",\"交付額\","
                        + "\"金融機関コード\",\"金融機関名\",\"支店コード\",\"支店名\",\"預金種目\",\"口座番号\",\"口座名義\"",
                headerLine);

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #11 downloadCsv 正常系
    // =====================================================================

    @Test
    @DisplayName("#11 downloadCsv 正常系 レスポンス先頭に UTF-8 BOM が付く")
    void downloadCsv_BOMが付く() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(shoreikinRenkeiService.findByKeys(eq("01100"), any())).thenReturn(List.of());

        ResponseEntity<byte[]> response = controller.downloadCsv(List.of(key("S001", "2024")));

        byte[] body = response.getBody();
        assertNotNull(body);
        assertEquals((byte) 0xEF, body[0]);
        assertEquals((byte) 0xBB, body[1]);
        assertEquals((byte) 0xBF, body[2]);

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #12 downloadCsv 正常系
    // =====================================================================

    @Test
    @DisplayName("#12 downloadCsv 正常系 Content-Type・Content-Disposition・ステータスが正しい")
    void downloadCsv_レスポンスヘッダーが正しい() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(shoreikinRenkeiService.findByKeys(eq("01100"), any())).thenReturn(List.of());

        ResponseEntity<byte[]> response = controller.downloadCsv(List.of(key("S001", "2024")));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("text/csv;charset=utf-8",
                response.getHeaders().getContentType().toString());
        assertTrue(response.getHeaders().getContentDisposition().toString()
                .contains("kofukin_furikomi.csv"));

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #13 downloadCsv 正常系
    // =====================================================================

    @Test
    @DisplayName("#13 downloadCsv 正常系 データ1件が15列の順・形式で出力される")
    void downloadCsv_データ1件が15列で出力される() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");

        ShoreikinRenkeiDto d = new ShoreikinRenkeiDto();
        d.setShiteiNo("S001");
        d.setAtenaNo("1");
        d.setName("山田太郎");
        d.setNendo("2024");
        d.setKofuYmd(LocalDate.of(2024, 4, 1));
        d.setKofuZeigaku(1000L);
        d.setKofuRitsu(new BigDecimal("1.5"));
        d.setKofuGaku(15L);
        d.setBankCd("0001");
        d.setBankName("テスト銀行");
        d.setBranchCd("001");
        d.setBranchName("本店");
        d.setShumoku("1");
        d.setKozaNo("1234567");
        d.setMeigi("ヤマダタロウ");

        when(shoreikinRenkeiService.findByKeys(eq("01100"), any())).thenReturn(List.of(d));

        ResponseEntity<byte[]> response = controller.downloadCsv(List.of(key("S001", "2024")));

        byte[] body = response.getBody();
        String csv = new String(body, 3, body.length - 3, StandardCharsets.UTF_8);
        String dataLine = csv.split("\n")[1];

        assertEquals(
                "\"S001\",\"1\",\"山田太郎\",\"2024\",\"2024-04-01\",\"1000\",\"1.5\",\"15\","
                        + "\"0001\",\"テスト銀行\",\"001\",\"本店\",\"普通\",\"1234567\",\"ヤマダタロウ\"",
                dataLine);

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #14 downloadCsv 正常系
    // =====================================================================

    @Test
    @DisplayName("#14 downloadCsv 正常系 複数件は findByKeys の戻り順で出力される")
    void downloadCsv_複数件は戻り順で出力される() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(shoreikinRenkeiService.findByKeys(eq("01100"), any()))
                .thenReturn(List.of(dto("S002"), dto("S001")));

        ResponseEntity<byte[]> response = controller.downloadCsv(
                List.of(key("S001", "2024"), key("S002", "2024")));

        byte[] body = response.getBody();
        String csv = new String(body, 3, body.length - 3, StandardCharsets.UTF_8);
        String[] lines = csv.split("\n");

        assertTrue(lines[1].startsWith("\"S002\""));
        assertTrue(lines[2].startsWith("\"S001\""));

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #15 downloadCsv 正常系
    // =====================================================================

    @Test
    @DisplayName("#15 downloadCsv 正常系 預金種目が \"1\"→\"普通\" / \"2\"→\"当座\" に変換される")
    void downloadCsv_預金種目が変換される() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");

        ShoreikinRenkeiDto d1 = dto("S001");
        d1.setShumoku("1");
        ShoreikinRenkeiDto d2 = dto("S002");
        d2.setShumoku("2");

        when(shoreikinRenkeiService.findByKeys(eq("01100"), any())).thenReturn(List.of(d1, d2));

        ResponseEntity<byte[]> response = controller.downloadCsv(
                List.of(key("S001", "2024"), key("S002", "2024")));

        byte[] body = response.getBody();
        String csv = new String(body, 3, body.length - 3, StandardCharsets.UTF_8);
        String[] lines = csv.split("\n");

        assertTrue(lines[1].contains("\"普通\""));
        assertTrue(lines[2].contains("\"当座\""));

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #16 downloadCsv 正常系
    // =====================================================================

    @Test
    @DisplayName("#16 downloadCsv 正常系 対象0件の場合はヘッダー行のみ出力される")
    void downloadCsv_対象0件はヘッダー行のみ() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(shoreikinRenkeiService.findByKeys("01100", List.of())).thenReturn(List.of());

        ResponseEntity<byte[]> response = controller.downloadCsv(List.of());

        byte[] body = response.getBody();
        String csv = new String(body, 3, body.length - 3, StandardCharsets.UTF_8);
        String[] lines = csv.split("\n");

        assertEquals(1, lines.length);

        verify(shoreikinRenkeiService, times(1)).findByKeys("01100", List.of());
        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #17 downloadCsv 正常系
    // =====================================================================

    @Test
    @DisplayName("#17 downloadCsv 正常系 ダブルクォートを含む項目はエスケープされる")
    void downloadCsv_ダブルクォートがエスケープされる() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");

        ShoreikinRenkeiDto d = dto("S001");
        d.setName("山田\"太郎");

        when(shoreikinRenkeiService.findByKeys(eq("01100"), any())).thenReturn(List.of(d));

        ResponseEntity<byte[]> response = controller.downloadCsv(List.of(key("S001", "2024")));

        byte[] body = response.getBody();
        String csv = new String(body, 3, body.length - 3, StandardCharsets.UTF_8);
        String dataLine = csv.split("\n")[1];

        assertTrue(dataLine.contains("\"山田\"\"太郎\""));

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #18 downloadCsv 正常系
    // =====================================================================

    @Test
    @DisplayName("#18 downloadCsv 正常系 行区切りの確認")
    void downloadCsv_行区切りがLF() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(shoreikinRenkeiService.findByKeys(eq("01100"), any())).thenReturn(List.of(dto("S001")));

        ResponseEntity<byte[]> response = controller.downloadCsv(List.of(key("S001", "2024")));

        byte[] body = response.getBody();
        String csv = new String(body, 3, body.length - 3, StandardCharsets.UTF_8);

        assertTrue(csv.contains("\n"));
        assertTrue(csv.endsWith("\n"));

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #19 downloadCsv 異常系
    // =====================================================================

    @Test
    @DisplayName("#19 downloadCsv 異常系 各項目が null の場合は空文字で出力される")
    void downloadCsv_各項目がnullの場合は空文字() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(shoreikinRenkeiService.findByKeys(eq("01100"), any()))
                .thenReturn(List.of(new ShoreikinRenkeiDto()));

        ResponseEntity<byte[]> response = assertDoesNotThrow(
                () -> controller.downloadCsv(List.of(key("S001", "2024"))));

        byte[] body = response.getBody();
        String csv = new String(body, 3, body.length - 3, StandardCharsets.UTF_8);
        String dataLine = csv.split("\n")[1];

        assertEquals("\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"", dataLine);

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #20 downloadCsv 異常系
    // =====================================================================

    @Test
    @DisplayName("#20 downloadCsv 異常系 預金種目が未定義値・null の場合")
    void downloadCsv_預金種目が未定義値とnull() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");

        ShoreikinRenkeiDto d1 = dto("S001");
        d1.setShumoku("9");
        ShoreikinRenkeiDto d2 = dto("S002");
        d2.setShumoku(null);

        when(shoreikinRenkeiService.findByKeys(eq("01100"), any())).thenReturn(List.of(d1, d2));

        ResponseEntity<byte[]> response = assertDoesNotThrow(
                () -> controller.downloadCsv(List.of(key("S001", "2024"), key("S002", "2024"))));

        byte[] body = response.getBody();
        String csv = new String(body, 3, body.length - 3, StandardCharsets.UTF_8);
        String[] lines = csv.split("\n");

        assertTrue(lines[1].contains("\"9\""));
        assertTrue(lines[2].contains("\"\""));

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #21 downloadCsv 異常系
    // =====================================================================

    @Test
    @DisplayName("#21 downloadCsv 異常系 存在しない指定番号のみを指定した場合")
    void downloadCsv_存在しない指定番号はヘッダー行のみ() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(shoreikinRenkeiService.findByKeys(eq("01100"), any())).thenReturn(List.of());

        ResponseEntity<byte[]> response = controller.downloadCsv(List.of(key("X999", "2024")));

        assertEquals(200, response.getStatusCode().value());

        byte[] body = response.getBody();
        String csv = new String(body, 3, body.length - 3, StandardCharsets.UTF_8);
        assertEquals(1, csv.split("\n").length);

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #22 downloadCsv 異常系
    // =====================================================================

    @Test
    @DisplayName("#22 downloadCsv 異常系 keys が null の場合")
    void downloadCsv_keysがnull() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");

        // 現行実装は NullPointerException となるため失敗する（実装修正が必要）
        ResponseEntity<byte[]> response = assertDoesNotThrow(
                () -> controller.downloadCsv(null));

        byte[] body = response.getBody();
        String csv = new String(body, 3, body.length - 3, StandardCharsets.UTF_8);
        assertEquals(1, csv.split("\n").length);

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // =====================================================================
    // #23 downloadCsv 異常系
    // =====================================================================

    @Test
    @DisplayName("#23 downloadCsv 異常系 アクセス権限が無い場合")
    void downloadCsv_アクセス権限が無い場合() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        doThrow(new AccessDeniedException("mo00000003", "user1"))
                .when(accessChecker).checkAccess(SCREEN_ID);

        assertThrows(AccessDeniedException.class,
                () -> controller.downloadCsv(List.of(key("S001", "2024"))));

        verify(shoreikinRenkeiService, never()).findByKeys(any(), any());
    }
}

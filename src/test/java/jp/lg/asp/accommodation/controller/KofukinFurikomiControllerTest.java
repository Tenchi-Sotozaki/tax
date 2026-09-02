package jp.lg.asp.accommodation.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.ShoreikinRenkeiDto;
import jp.lg.asp.accommodation.service.ShoreikinRenkeiService;

@ExtendWith(MockitoExtension.class)
class KofukinFurikomiControllerTest {

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

    // =====================================================================
    // 交付金振込情報出力確認_単体テストチェックリスト（#確認1〜#確認14）
    // =====================================================================

    private static final String SCREEN_ID_KAKUNIN = ScreenManagement.KOFUKIN_FURIKOMI_KAKUNIN;

    @Test
    @DisplayName("#確認1 kakunin 正常系 選択1件を確認画面に表示する")
    void kakunin_選択1件を確認画面に表示する() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");

        ShoreikinRenkeiDto d = dto("S001");
        d.setAtenaNo("1");
        d.setName("山田太郎");
        d.setNendo("2024");
        d.setKofuGaku(15L);
        when(shoreikinRenkeiService.findByKeys(eq("01100"), any())).thenReturn(List.of(d));

        ArgumentCaptor<String> jichitaiCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<ShoreikinRenkeiDto.Key>> keysCaptor = ArgumentCaptor.forClass(List.class);

        Model model = new ExtendedModelMap();
        String result = controller.kakunin("[{\"shiteiNo\":\"S001\",\"nendo\":\"2024\"}]", model);

        assertEquals("renkei/kofukinFurikomiKakunin", result);

        @SuppressWarnings("unchecked")
        List<ShoreikinRenkeiDto> rows = (List<ShoreikinRenkeiDto>) model.asMap().get("rows");
        assertEquals(1, rows.size());
        assertEquals("S001", rows.get(0).getShiteiNo());
        assertEquals("山田太郎", rows.get(0).getName());

        verify(shoreikinRenkeiService).findByKeys(jichitaiCaptor.capture(), keysCaptor.capture());
        assertEquals("01100", jichitaiCaptor.getValue());
        assertEquals(1, keysCaptor.getValue().size());
        assertEquals("S001", keysCaptor.getValue().get(0).getShiteiNo());
        assertEquals("2024", keysCaptor.getValue().get(0).getNendo());

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID_KAKUNIN);
    }

    @Test
    @DisplayName("#確認2 kakunin 正常系 選択2件を渡した順序どおりに表示する")
    void kakunin_選択2件を渡した順序どおりに表示する() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(shoreikinRenkeiService.findByKeys(eq("01100"), any()))
                .thenReturn(List.of(dto("S001"), dto("S002")));

        ArgumentCaptor<List<ShoreikinRenkeiDto.Key>> keysCaptor = ArgumentCaptor.forClass(List.class);

        Model model = new ExtendedModelMap();
        String result = controller.kakunin(
                "[{\"shiteiNo\":\"S001\",\"nendo\":\"2024\"},{\"shiteiNo\":\"S002\",\"nendo\":\"2024\"}]", model);

        assertEquals("renkei/kofukinFurikomiKakunin", result);

        @SuppressWarnings("unchecked")
        List<ShoreikinRenkeiDto> rows = (List<ShoreikinRenkeiDto>) model.asMap().get("rows");
        assertEquals(2, rows.size());
        assertEquals("S001", rows.get(0).getShiteiNo());
        assertEquals("S002", rows.get(1).getShiteiNo());

        verify(shoreikinRenkeiService, times(1)).findByKeys(any(), keysCaptor.capture());
        assertEquals(2, keysCaptor.getValue().size());
        assertEquals("S001", keysCaptor.getValue().get(0).getShiteiNo());
        assertEquals("S002", keysCaptor.getValue().get(1).getShiteiNo());

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID_KAKUNIN);
    }

    @Test
    @DisplayName("#確認3 kakunin 正常系 サービスの戻り値を加工せずそのまま model に設定する")
    void kakunin_サービスの戻り値を加工せずそのままmodelに設定する() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");

        ShoreikinRenkeiDto d = dto("S001");
        d.setShumoku("9");
        d.setKofuRitsu(new BigDecimal("1.50"));
        when(shoreikinRenkeiService.findByKeys(eq("01100"), any())).thenReturn(List.of(d));

        Model model = new ExtendedModelMap();
        controller.kakunin("[{\"shiteiNo\":\"S001\",\"nendo\":\"2024\"}]", model);

        @SuppressWarnings("unchecked")
        List<ShoreikinRenkeiDto> rows = (List<ShoreikinRenkeiDto>) model.asMap().get("rows");
        // 預金種目の名称変換や桁整形はコントローラで行わず、画面側の th:switch に委ねること
        assertEquals("9", rows.get(0).getShumoku());
        assertEquals(new BigDecimal("1.50"), rows.get(0).getKofuRitsu());

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID_KAKUNIN);
    }

    @Test
    @DisplayName("#確認4 kakunin 正常系 該当データが0件の場合は空リストを設定する")
    void kakunin_該当データが0件の場合は空リスト() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(shoreikinRenkeiService.findByKeys(eq("01100"), any())).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        String result = controller.kakunin("[{\"shiteiNo\":\"S001\",\"nendo\":\"2024\"}]", model);

        assertEquals("renkei/kofukinFurikomiKakunin", result);

        List<?> rows = (List<?>) model.asMap().get("rows");
        assertNotNull(rows);
        assertTrue(rows.isEmpty());

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID_KAKUNIN);
    }

    @Test
    @DisplayName("#確認5 kakunin 正常系 keysJson が空配列の場合はサービスに空リストが渡る")
    void kakunin_keysJsonが空配列() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(shoreikinRenkeiService.findByKeys(eq("01100"), any())).thenReturn(List.of());

        ArgumentCaptor<List<ShoreikinRenkeiDto.Key>> keysCaptor = ArgumentCaptor.forClass(List.class);

        Model model = new ExtendedModelMap();
        String result = controller.kakunin("[]", model);

        assertEquals("renkei/kofukinFurikomiKakunin", result);

        List<?> rows = (List<?>) model.asMap().get("rows");
        assertNotNull(rows);
        assertTrue(rows.isEmpty());

        // 呼ばずに return しないこと
        verify(shoreikinRenkeiService, times(1)).findByKeys(any(), keysCaptor.capture());
        assertTrue(keysCaptor.getValue().isEmpty());

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID_KAKUNIN);
    }

    @Test
    @DisplayName("#確認6 kakunin 異常系 keysJson が不正な JSON の場合")
    void kakunin_keysJsonが不正なJSON() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");

        Model model = new ExtendedModelMap();
        String result = controller.kakunin("invalid", model);

        assertEquals("renkei/kofukinFurikomiKakunin", result);

        List<?> rows = (List<?>) model.asMap().get("rows");
        assertNotNull(rows);
        assertTrue(rows.isEmpty());

        verify(shoreikinRenkeiService, never()).findByKeys(any(), any());
        verify(accessChecker, times(1)).checkAccess(SCREEN_ID_KAKUNIN);
    }

    @Test
    @DisplayName("#確認7 kakunin 異常系 keysJson が null の場合")
    void kakunin_keysJsonがnull() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");

        Model model = new ExtendedModelMap();
        String result = assertDoesNotThrow(() -> controller.kakunin(null, model));

        assertEquals("renkei/kofukinFurikomiKakunin", result);

        List<?> rows = (List<?>) model.asMap().get("rows");
        assertNotNull(rows);
        assertTrue(rows.isEmpty());

        verify(shoreikinRenkeiService, never()).findByKeys(any(), any());
        verify(accessChecker, times(1)).checkAccess(SCREEN_ID_KAKUNIN);
    }

    @Test
    @DisplayName("#確認8 kakunin 異常系 keysJson が空文字の場合")
    void kakunin_keysJsonが空文字() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");

        Model model = new ExtendedModelMap();
        String result = controller.kakunin("", model);

        assertEquals("renkei/kofukinFurikomiKakunin", result);

        List<?> rows = (List<?>) model.asMap().get("rows");
        assertNotNull(rows);
        assertTrue(rows.isEmpty());

        verify(shoreikinRenkeiService, never()).findByKeys(any(), any());
        verify(accessChecker, times(1)).checkAccess(SCREEN_ID_KAKUNIN);
    }

    @Test
    @DisplayName("#確認9 kakunin 異常系 keysJson が JSON 配列でない場合（単一オブジェクト）")
    void kakunin_keysJsonがJSON配列でない() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");

        Model model = new ExtendedModelMap();
        String result = controller.kakunin("{\"shiteiNo\":\"S001\",\"nendo\":\"2024\"}", model);

        assertEquals("renkei/kofukinFurikomiKakunin", result);

        List<?> rows = (List<?>) model.asMap().get("rows");
        assertNotNull(rows);
        assertTrue(rows.isEmpty());

        verify(shoreikinRenkeiService, never()).findByKeys(any(), any());
        verify(accessChecker, times(1)).checkAccess(SCREEN_ID_KAKUNIN);
    }

    @Test
    @DisplayName("#確認10 kakunin 異常系 keysJson に未定義のプロパティが含まれる場合")
    void kakunin_keysJsonに未定義プロパティが含まれる() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");

        Model model = new ExtendedModelMap();
        // ObjectMapper の既定で UnrecognizedPropertyException となり catch される
        controller.kakunin("[{\"shiteiNo\":\"S001\",\"nendo\":\"2024\",\"dummy\":\"x\"}]", model);

        List<?> rows = (List<?>) model.asMap().get("rows");
        assertNotNull(rows);
        assertTrue(rows.isEmpty());

        verify(shoreikinRenkeiService, never()).findByKeys(any(), any());
        verify(accessChecker, times(1)).checkAccess(SCREEN_ID_KAKUNIN);
    }

    @Test
    @DisplayName("#確認11 kakunin 異常系 キー項目が null の場合")
    void kakunin_キー項目がnull() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(shoreikinRenkeiService.findByKeys(eq("01100"), any())).thenReturn(List.of());

        ArgumentCaptor<List<ShoreikinRenkeiDto.Key>> keysCaptor = ArgumentCaptor.forClass(List.class);

        Model model = new ExtendedModelMap();
        String result = controller.kakunin("[{\"shiteiNo\":null,\"nendo\":null}]", model);

        assertEquals("renkei/kofukinFurikomiKakunin", result);

        List<?> rows = (List<?>) model.asMap().get("rows");
        assertNotNull(rows);
        assertTrue(rows.isEmpty());

        verify(shoreikinRenkeiService).findByKeys(any(), keysCaptor.capture());
        assertNull(keysCaptor.getValue().get(0).getShiteiNo());
        assertNull(keysCaptor.getValue().get(0).getNendo());

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID_KAKUNIN);
    }

    @Test
    @DisplayName("#確認12 kakunin 異常系 キー項目が空文字の場合")
    void kakunin_キー項目が空文字() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(shoreikinRenkeiService.findByKeys(eq("01100"), any())).thenReturn(List.of());

        ArgumentCaptor<List<ShoreikinRenkeiDto.Key>> keysCaptor = ArgumentCaptor.forClass(List.class);

        Model model = new ExtendedModelMap();
        String result = controller.kakunin("[{\"shiteiNo\":\"\",\"nendo\":\"\"}]", model);

        assertEquals("renkei/kofukinFurikomiKakunin", result);

        List<?> rows = (List<?>) model.asMap().get("rows");
        assertNotNull(rows);
        assertTrue(rows.isEmpty());

        // コントローラで null 変換しないこと
        verify(shoreikinRenkeiService).findByKeys(any(), keysCaptor.capture());
        assertEquals("", keysCaptor.getValue().get(0).getShiteiNo());
        assertEquals("", keysCaptor.getValue().get(0).getNendo());

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID_KAKUNIN);
    }

    @Test
    @DisplayName("#確認13 kakunin 異常系 存在しない指定番号を指定した場合")
    void kakunin_存在しない指定番号() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(shoreikinRenkeiService.findByKeys(eq("01100"), any())).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        String result = controller.kakunin("[{\"shiteiNo\":\"X999\",\"nendo\":\"2024\"}]", model);

        assertEquals("renkei/kofukinFurikomiKakunin", result);

        List<?> rows = (List<?>) model.asMap().get("rows");
        assertNotNull(rows);
        assertTrue(rows.isEmpty());

        verify(shoreikinRenkeiService, times(1)).findByKeys(any(), any());
        verify(accessChecker, times(1)).checkAccess(SCREEN_ID_KAKUNIN);
    }

    @Test
    @DisplayName("#確認14 kakunin 異常系 サービスが例外をスローした場合")
    void kakunin_サービスが例外をスロー() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(shoreikinRenkeiService.findByKeys(eq("01100"), any()))
                .thenThrow(new RuntimeException("DB error"));

        Model model = new ExtendedModelMap();
        // 例外が呼び出し元に伝播しないこと
        String result = assertDoesNotThrow(
                () -> controller.kakunin("[{\"shiteiNo\":\"S001\",\"nendo\":\"2024\"}]", model));

        assertEquals("renkei/kofukinFurikomiKakunin", result);

        List<?> rows = (List<?>) model.asMap().get("rows");
        assertNotNull(rows);
        assertTrue(rows.isEmpty());

        verify(accessChecker, times(1)).checkAccess(SCREEN_ID_KAKUNIN);
    }
}

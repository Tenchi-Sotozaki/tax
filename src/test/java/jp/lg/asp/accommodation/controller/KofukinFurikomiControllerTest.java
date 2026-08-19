package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.ShoreikinRenkeiDto;
import jp.lg.asp.accommodation.service.ShoreikinRenkeiService;

/**
 * 交付金振込情報出力 / 同 確認（ACCOMMODATION_TAX-372 / 373）の Controller 単体テスト。
 *
 * サービスをモックに差し替え、一覧・検索・CSVダウンロード・確認画面を検証する。
 *
 * CSV の組み立てがこのクラスに直接書かれているため、そこを厚めに見ている。
 * Excel で開いたときに文字化けしないよう先頭に UTF-8 BOM を付ける仕様なので、
 * バイト列の先頭も確認している。
 */
@ExtendWith(MockitoExtension.class)
class KofukinFurikomiControllerTest {

    @Mock ScreenAccessChecker accessChecker;
    @Mock ShoreikinRenkeiService shoreikinRenkeiService;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks KofukinFurikomiController controller;

    private static final String JICHITAI_CD = "01100";
    private static final byte[] UTF8_BOM = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    // ===================================================================
    // テストデータ
    // ===================================================================

    private ShoreikinRenkeiDto row() {
        ShoreikinRenkeiDto d = new ShoreikinRenkeiDto();
        d.setShiteiNo("00100001");
        d.setAtenaNo("1001");
        d.setName("株式会社ホテルA");
        d.setNendo("2026");
        d.setKofuYmd(LocalDate.of(2026, 4, 30));
        d.setKofuZeigaku(1000000L);
        d.setKofuRitsu(new BigDecimal("2.0"));
        d.setKofuGaku(20000L);
        d.setBankCd("0001");
        d.setBankName("みずほ銀行");
        d.setBranchCd("001");
        d.setBranchName("札幌支店");
        d.setShumoku("1");
        d.setKozaNo("1234567");
        d.setMeigi("カ）ホテルエー");
        return d;
    }

    private ShoreikinRenkeiDto.Key key() {
        ShoreikinRenkeiDto.Key k = new ShoreikinRenkeiDto.Key();
        k.setShiteiNo("00100001");
        k.setNendo("2026");
        return k;
    }

    /** レスポンスボディを BOM を除いた文字列にする */
    private String csvBody(ResponseEntity<byte[]> response) {
        byte[] body = response.getBody();
        return new String(body, UTF8_BOM.length, body.length - UTF8_BOM.length, StandardCharsets.UTF_8);
    }

    // ===================================================================
    // index — 一覧
    // ===================================================================

    @Test
    void index_検索結果と検索条件がモデルに載る() {
        when(shoreikinRenkeiService.search(JICHITAI_CD, "2026", "00100001", "ホテル", "partial"))
                .thenReturn(List.of(row()));
        Model model = new ExtendedModelMap();

        String view = controller.index("2026", "00100001", "ホテル", "partial", model);

        assertThat(view).isEqualTo("renkei/kofukinFurikomi");
        assertThat((List<?>) model.asMap().get("items")).hasSize(1);
        @SuppressWarnings("unchecked")
        Map<String, Object> searchForm = (Map<String, Object>) model.asMap().get("searchForm");
        assertThat(searchForm).containsEntry("nendo", "2026")
                .containsEntry("shiteiNo", "00100001")
                .containsEntry("name", "ホテル")
                .containsEntry("nameMatchType", "partial");
    }

    /**
     * 現状の実装はサービスの例外を握りつぶし、空の一覧で画面を返す。
     * DB障害時も画面上は「0件」に見えるため、仕様の是非は別途確認が必要。
     * ここでは現状の挙動を固定している。
     */
    @Test
    void index_サービスが例外を投げても空の一覧で画面を返す() {
        when(shoreikinRenkeiService.search(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("DB接続エラー"));
        Model model = new ExtendedModelMap();

        String view = controller.index("2026", null, null, "partial", model);

        assertThat(view).isEqualTo("renkei/kofukinFurikomi");
        assertThat((List<?>) model.asMap().get("items")).isEmpty();
    }

    @Test
    void index_参照権限を確認する() {
        when(shoreikinRenkeiService.search(any(), any(), any(), any(), any())).thenReturn(List.of());

        controller.index(null, null, null, "partial", new ExtendedModelMap());

        verify(accessChecker).checkAccess(ScreenManagement.KOFUKIN_FURIKOMI);
    }

    // ===================================================================
    // search — 検索API
    // ===================================================================

    @Test
    void search_サービスの戻り値をそのまま返す() {
        List<ShoreikinRenkeiDto> expected = List.of(row());
        when(shoreikinRenkeiService.search(JICHITAI_CD, "2026", null, null, "partial")).thenReturn(expected);

        assertThat(controller.search("2026", null, null, "partial")).isSameAs(expected);
    }

    // ===================================================================
    // downloadCsv — CSV出力
    // ===================================================================

    @Test
    void downloadCsv_CSVとしてダウンロードさせるヘッダを返す() {
        when(shoreikinRenkeiService.findByKeys(eq(JICHITAI_CD), anyList())).thenReturn(List.of(row()));

        ResponseEntity<byte[]> response = controller.downloadCsv(List.of(key()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).startsWith("text/csv");
        assertThat(response.getHeaders().getContentDisposition().getFilename())
                .isEqualTo("kofukin_furikomi.csv");
    }

    /** Excel で開いたときに文字化けしないよう先頭に BOM を付けている */
    @Test
    void downloadCsv_先頭にUTF8のBOMが付く() {
        when(shoreikinRenkeiService.findByKeys(eq(JICHITAI_CD), anyList())).thenReturn(List.of(row()));

        byte[] body = controller.downloadCsv(List.of(key())).getBody();

        assertThat(body).startsWith(UTF8_BOM);
    }

    @Test
    void downloadCsv_見出し行が15列で出力される() {
        when(shoreikinRenkeiService.findByKeys(eq(JICHITAI_CD), anyList())).thenReturn(List.of());

        String csv = csvBody(controller.downloadCsv(List.of(key())));

        String header = csv.split("\n")[0];
        assertThat(header).startsWith("\"指定番号\",\"宛名番号\",\"氏名/名称\"");
        assertThat(header).endsWith("\"口座番号\",\"口座名義\"");
        assertThat(header.split(",")).hasSize(15);
    }

    @Test
    void downloadCsv_明細が全項目出力される() {
        when(shoreikinRenkeiService.findByKeys(eq(JICHITAI_CD), anyList())).thenReturn(List.of(row()));

        String csv = csvBody(controller.downloadCsv(List.of(key())));

        String detail = csv.split("\n")[1];
        assertThat(detail).contains("\"00100001\"").contains("\"株式会社ホテルA\"")
                .contains("\"2026-04-30\"").contains("\"20000\"")
                .contains("\"みずほ銀行\"").contains("\"カ）ホテルエー\"");
    }

    @Test
    void downloadCsv_預金種目が名称に変換される() {
        ShoreikinRenkeiDto futsu = row();
        ShoreikinRenkeiDto toza = row();
        toza.setShumoku("2");
        ShoreikinRenkeiDto other = row();
        other.setShumoku("9");
        when(shoreikinRenkeiService.findByKeys(eq(JICHITAI_CD), anyList()))
                .thenReturn(List.of(futsu, toza, other));

        String[] lines = csvBody(controller.downloadCsv(List.of(key()))).split("\n");

        assertThat(lines[1]).contains("\"普通\"");
        assertThat(lines[2]).contains("\"当座\"");
        assertThat(lines[3]).contains("\"9\"");
    }

    @Test
    void downloadCsv_項目がnullでも空文字になり落ちない() {
        when(shoreikinRenkeiService.findByKeys(eq(JICHITAI_CD), anyList()))
                .thenReturn(List.of(new ShoreikinRenkeiDto()));

        String csv = csvBody(controller.downloadCsv(List.of(key())));

        String detail = csv.split("\n")[1];
        assertThat(detail).isEqualTo("\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"");
    }

    @Test
    void downloadCsv_値にダブルクォートが含まれるとエスケープされる() {
        ShoreikinRenkeiDto dto = row();
        dto.setName("株式会社\"ホテルA\"");
        when(shoreikinRenkeiService.findByKeys(eq(JICHITAI_CD), anyList())).thenReturn(List.of(dto));

        String csv = csvBody(controller.downloadCsv(List.of(key())));

        assertThat(csv.split("\n")[1]).contains("\"株式会社\"\"ホテルA\"\"\"");
    }

    @Test
    void downloadCsv_該当が無ければ見出し行だけになる() {
        when(shoreikinRenkeiService.findByKeys(eq(JICHITAI_CD), anyList())).thenReturn(List.of());

        String csv = csvBody(controller.downloadCsv(List.of(key())));

        assertThat(csv.strip().split("\n")).hasSize(1);
    }

    // ===================================================================
    // kakunin — 確認画面（373）
    // ===================================================================

    @Test
    void kakunin_JSONを解釈して明細をモデルに載せる() {
        when(shoreikinRenkeiService.findByKeys(eq(JICHITAI_CD), anyList())).thenReturn(List.of(row()));
        Model model = new ExtendedModelMap();

        String view = controller.kakunin("[{\"shiteiNo\":\"00100001\",\"nendo\":\"2026\"}]", model);

        assertThat(view).isEqualTo("renkei/kofukinFurikomiKakunin");
        assertThat((List<?>) model.asMap().get("rows")).hasSize(1);
        verify(accessChecker).checkAccess(ScreenManagement.KOFUKIN_FURIKOMI_KAKUNIN);
    }

    /** JSON が壊れていても画面は返す。現状の握りつぶしを固定している */
    @Test
    void kakunin_JSONが不正なら空の明細で画面を返す() {
        Model model = new ExtendedModelMap();

        String view = controller.kakunin("これはJSONではない", model);

        assertThat(view).isEqualTo("renkei/kofukinFurikomiKakunin");
        assertThat((List<?>) model.asMap().get("rows")).isEmpty();
        verify(shoreikinRenkeiService, never()).findByKeys(any(), any());
    }
}

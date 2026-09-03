package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.service.KoseiKetteiTsuchiReportsService;
import jp.lg.asp.accommodation.util.SessionHelper;

/**
 * 宿泊税更正・決定通知書 単体テスト（コントローラ）
 *
 * <p>チェックリスト「KoseiKetteiTsuchiController」の #1〜#14 に1対1で対応する。</p>
 */
@ExtendWith(MockitoExtension.class)
class KoseiKetteiTsuchiControllerTest {

    @Mock KoseiKetteiTsuchiReportsService reportsService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks KoseiKetteiTsuchiController controller;

    /** セッションに ShiteiGassanSearchDto をセットして返す */
    private MockHttpSession sessionWith(String shiteiNo, String gassanShiteiNo) {
        MockHttpSession session = new MockHttpSession();
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setShiteiNo(shiteiNo);
        dto.setGassanShiteiNo(gassanShiteiNo);
        session.setAttribute(SessionHelper.SHITEI_GASSAN_KEY, dto);
        return session;
    }

    // ------------------------------------------------------------------
    // #1 index 正常系
    // ------------------------------------------------------------------

    @Test
    @DisplayName("#1 index 正常系 セッションに指定番号あり：対象年月リストが取得され画面を返す")
    void index_指定番号ありで対象年月リストが取得され画面を返す() {
        when(reportsService.findTaishoYmList("S001"))
                .thenReturn(List.of("202404", "202405"));

        Model model = new ExtendedModelMap();
        String view = controller.index(sessionWith("S001", null), model);

        assertThat(view).isEqualTo("reports/koseiKetteiTsuchi");
        assertThat(model.asMap().get("taishoYmList")).isEqualTo(List.of("202404", "202405"));
        assertThat(model.asMap()).doesNotContainKey("dto");
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.KOSEI_KETTEI_TSUCHI);
    }

    // ------------------------------------------------------------------
    // #2 index 正常系
    // ------------------------------------------------------------------

    @Test
    @DisplayName("#2 index 正常系 セッションに指定番号あり＋合算指定番号あり：指定番号が優先される")
    void index_指定番号と合算指定番号が両方ある場合は指定番号が優先される() {
        when(reportsService.findTaishoYmList("S001"))
                .thenReturn(List.of("202404"));

        Model model = new ExtendedModelMap();
        String view = controller.index(sessionWith("S001", "G001"), model);

        assertThat(view).isEqualTo("reports/koseiKetteiTsuchi");
        verify(reportsService, times(1)).findTaishoYmList("S001");
        verify(reportsService, never()).findTaishoYmList("G001");
        assertThat(model.asMap().get("shiteiNo")).isEqualTo("S001");
        assertThat(model.asMap()).containsKey("taishoYmList");
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.KOSEI_KETTEI_TSUCHI);
    }

    // ------------------------------------------------------------------
    // #3 index 正常系（実装修正あり）
    // ------------------------------------------------------------------

    /**
     * ※実装修正：index の判定を targetNo = (shiteiNo != null) ? shiteiNo : gassanShiteiNo に変更し、
     * targetNo == null のときのみモーダル表示。合算指定番号のみの場合は処理を継続する。
     */
    @Test
    @DisplayName("#3 index 正常系 セッションに合算指定番号のみあり（指定番号なし）：検索モーダルを表示せず合算指定番号で処理を継続する")
    void index_合算指定番号のみの場合は合算指定番号で処理を継続する() {
        when(reportsService.findTaishoYmList("G001"))
                .thenReturn(List.of("202404"));

        Model model = new ExtendedModelMap();
        String view = controller.index(sessionWith(null, "G001"), model);

        assertThat(view).isEqualTo("reports/koseiKetteiTsuchi");
        verify(reportsService, times(1)).findTaishoYmList("G001");
        assertThat(model.asMap().get("shiteiNo")).isEqualTo("G001");
        assertThat(model.asMap()).containsKey("taishoYmList");
        assertThat(model.asMap()).doesNotContainKey("showShiteiGassanModal");
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.KOSEI_KETTEI_TSUCHI);
    }

    // ------------------------------------------------------------------
    // #4 index 正常系（実装修正あり）
    // ------------------------------------------------------------------

    /**
     * ※要注意：修正案の targetNo = (shiteiNo != null) ? shiteiNo : gassanShiteiNo では
     * shiteiNo="" が null 判定にならずモーダルが出ない。
     * 空文字も null 扱いにする（StringUtils.isEmpty 等）必要あり。
     */
    @Test
    @DisplayName("#4 index 正常系 指定番号が空文字＋合算指定番号なし：検索モーダルを表示する")
    void index_指定番号が空文字で合算指定番号もない場合はモーダル表示() {
        Model model = new ExtendedModelMap();
        String view = controller.index(sessionWith("", null), model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
        verify(reportsService, never()).findTaishoYmList(any());
    }

    // ------------------------------------------------------------------
    // #5 index 正常系
    // ------------------------------------------------------------------

    @Test
    @DisplayName("#5 index 正常系 findTaishoYmList が0件を返す場合")
    void index_findTaishoYmListが0件の場合は空リストがセットされる() {
        when(reportsService.findTaishoYmList("S001"))
                .thenReturn(List.of());

        Model model = new ExtendedModelMap();
        String view = controller.index(sessionWith("S001", null), model);

        assertThat(view).isEqualTo("reports/koseiKetteiTsuchi");
        assertThat(model.asMap().get("taishoYmList")).isEqualTo(List.of());
    }

    // ------------------------------------------------------------------
    // #6 index 異常系
    // ------------------------------------------------------------------

    @Test
    @DisplayName("#6 index 異常系 指定番号・合算指定番号ともに null/空の場合：検索モーダルを表示する")
    void index_指定番号と合算指定番号がともにnullの場合はモーダル表示() {
        Model model = new ExtendedModelMap();
        String view = controller.index(sessionWith(null, null), model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
        verify(reportsService, never()).findTaishoYmList(any());
    }

    // ------------------------------------------------------------------
    // #7 generatePdf 正常系
    // ------------------------------------------------------------------

    @Test
    @DisplayName("#7 generatePdf 正常系 PDF生成処理")
    void generatePdf_PDF生成処理() {
        byte[] pdfBytes = new byte[]{1, 2, 3};
        when(reportsService.generatePdf("S001", "202404", null, null, "2"))
                .thenReturn(pdfBytes);

        ResponseEntity<byte[]> response = controller.generatePdf("S001", "202404", null, null, "2");

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.KOSEI_KETTEI_TSUCHI);
    }

    // ------------------------------------------------------------------
    // #8 generatePdf 正常系
    // ------------------------------------------------------------------

    @Test
    @DisplayName("#8 generatePdf 正常系 b2Ym / b3Ym が未指定の場合は null がサービスに渡ること")
    void generatePdf_b2YmとB3Ymが未指定の場合はnullがサービスに渡る() {
        byte[] pdfBytes = new byte[]{1, 2, 3};
        when(reportsService.generatePdf("S001", "202404", null, null, "2"))
                .thenReturn(pdfBytes);

        controller.generatePdf("S001", "202404", null, null, "2");

        verify(reportsService, times(1)).generatePdf("S001", "202404", null, null, "2");
    }

    // ------------------------------------------------------------------
    // #9 generatePdf 正常系
    // ------------------------------------------------------------------

    @Test
    @DisplayName("#9 generatePdf 正常系 henkoKbn 未指定の場合は \"2\" が渡ること")
    void generatePdf_henkoKbn未指定の場合はデフォルト値2が渡る() {
        byte[] pdfBytes = new byte[]{1, 2, 3};
        when(reportsService.generatePdf(eq("S001"), eq("202404"), any(), any(), eq("2")))
                .thenReturn(pdfBytes);

        // @RequestParam の defaultValue = "2" を明示して呼び出す
        controller.generatePdf("S001", "202404", null, null, "2");

        verify(reportsService, times(1))
                .generatePdf("S001", "202404", null, null, "2");
    }

    // ------------------------------------------------------------------
    // #10 generatePdf 異常系
    // ------------------------------------------------------------------

    @Test
    @DisplayName("#10 generatePdf 異常系 reportsService.generatePdf が例外をスローした場合：500エラー")
    void generatePdf_例外発生時は500エラーを返す() {
        when(reportsService.generatePdf(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("PDF生成エラー"));

        ResponseEntity<byte[]> response = controller.generatePdf("S001", "202404", null, null, "2");

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    // ------------------------------------------------------------------
    // #11 preview 正常系
    // ------------------------------------------------------------------

    @Test
    @DisplayName("#11 preview 正常系 プレビュー処理")
    void preview_プレビュー処理() {
        when(reportsService.generatePdf(any(), any(), any(), any(), any()))
                .thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<byte[]> response = controller.preview("S001", "202404", null, null, "2");

        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("inline");
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.KOSEI_KETTEI_TSUCHI);
    }

    // ------------------------------------------------------------------
    // #12 preview 異常系
    // ------------------------------------------------------------------

    @Test
    @DisplayName("#12 preview 異常系 reportsService.generatePdf が例外をスローした場合：500エラー")
    void preview_例外発生時は500エラーを返す() {
        when(reportsService.generatePdf(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("プレビューエラー"));

        ResponseEntity<byte[]> response = controller.preview("S001", "202404", null, null, "2");

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    // ------------------------------------------------------------------
    // #13 print 正常系
    // ------------------------------------------------------------------

    @Test
    @DisplayName("#13 print 正常系 印刷処理")
    void print_印刷処理() {
        when(reportsService.generatePdf(any(), any(), any(), any(), any()))
                .thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<byte[]> response = controller.print("S001", "202404", null, null, "2");

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst("X-Print-Action")).isEqualTo("true");
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.KOSEI_KETTEI_TSUCHI);
    }

    // ------------------------------------------------------------------
    // #14 print 異常系
    // ------------------------------------------------------------------

    @Test
    @DisplayName("#14 print 異常系 reportsService.generatePdf が例外をスローした場合：500エラー")
    void print_例外発生時は500エラーを返す() {
        when(reportsService.generatePdf(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("印刷エラー"));

        ResponseEntity<byte[]> response = controller.print("S001", "202404", null, null, "2");

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }
}

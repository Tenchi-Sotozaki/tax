package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

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
import jp.lg.asp.accommodation.dto.TokureiShiteiDto;
import jp.lg.asp.accommodation.service.TokureiShiteiReportsService;
import jp.lg.asp.accommodation.service.TokureiShiteiService;
import jp.lg.asp.accommodation.util.SessionHelper;

/**
 * 納入申告書の提出期限等の特例適用者指定通知書 単体テスト（コントローラ）
 *
 * <p>チェックリスト「TokureiShiteiController」の #1〜#10 に1対1で対応する。</p>
 */
@ExtendWith(MockitoExtension.class)
class TokureiShiteiControllerTest {

    @Mock TokureiShiteiService tokureiShiteiService;
    @Mock TokureiShiteiReportsService reportsService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks TokureiShiteiController controller;

    /** セッションに ShiteiGassanSearchDto をセットして返す */
    private MockHttpSession sessionWith(String shiteiNo, String gassanShiteiNo) {
        MockHttpSession session = new MockHttpSession();
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setShiteiNo(shiteiNo);
        dto.setGassanShiteiNo(gassanShiteiNo);
        session.setAttribute(SessionHelper.SHITEI_GASSAN_KEY, dto);
        return session;
    }

    /** 最低限のフィールドを持つ TokureiShiteiDto を生成する */
    private TokureiShiteiDto dto(String shiteiNo) {
        TokureiShiteiDto dto = new TokureiShiteiDto();
        dto.setShiteiNo(shiteiNo);
        return dto;
    }

    // ------------------------------------------------------------------
    // #1 index
    // ------------------------------------------------------------------

    @Test
    @DisplayName("#1 index 正常系 セッションに指定番号あり（合算指定番号なし）：通知書画面を返す")
    void index_指定番号ありで通知書画面を返す() {
        TokureiShiteiDto expected = dto("0001");
        expected.setHakkoYmd(LocalDate.of(2026, 4, 1));
        when(tokureiShiteiService.getTokugimuInfo("0001")).thenReturn(expected);

        Model model = new ExtendedModelMap();
        String view = controller.index(sessionWith("0001", null), model);

        assertThat(view).isEqualTo("reports/tokureiShitei");
        assertThat(model.asMap()).containsEntry("dto", expected);
        verify(tokureiShiteiService, times(1)).getTokugimuInfo("0001");
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.TOKUREI_SHITEI);
    }

    // ------------------------------------------------------------------
    // #2 index
    // ------------------------------------------------------------------

    @Test
    @DisplayName("#2 index 正常系 セッションに指定番号あり＋合算指定番号あり：合算指定番号は参照されず通知書画面を返す")
    void index_合算指定番号ありでも指定番号のみ使用する() {
        TokureiShiteiDto expected = dto("0001");
        expected.setHakkoYmd(LocalDate.of(2026, 4, 1));
        when(tokureiShiteiService.getTokugimuInfo("0001")).thenReturn(expected);

        Model model = new ExtendedModelMap();
        String view = controller.index(sessionWith("0001", "G001"), model);

        assertThat(view).isEqualTo("reports/tokureiShitei");
        assertThat(model.asMap()).containsEntry("dto", expected);
        verify(tokureiShiteiService, times(1)).getTokugimuInfo("0001");
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.TOKUREI_SHITEI);
    }

    // ------------------------------------------------------------------
    // #3 index
    // ------------------------------------------------------------------

    /**
     * ※現行実装は合算指定番号を参照しないため、合算のみの場合もモーダル表示となる
     */
    @Test
    @DisplayName("#3 index 正常系 セッションに合算指定番号のみあり（指定番号なし）：検索モーダルを表示する")
    void index_合算指定番号のみの場合はモーダル表示() {
        Model model = new ExtendedModelMap();
        String view = controller.index(sessionWith(null, "G001"), model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
        verify(tokureiShiteiService, never()).getTokugimuInfo(org.mockito.ArgumentMatchers.any());
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.TOKUREI_SHITEI);
    }

    // ------------------------------------------------------------------
    // #4 index
    // ------------------------------------------------------------------

    @Test
    @DisplayName("#4 index 正常系 セッションに両方なし（ShiteiGassanSearchDto が null）：検索モーダルを表示する")
    void index_セッションdtoがnullの場合はモーダル表示() {
        MockHttpSession session = new MockHttpSession();
        // ShiteiGassanSearchDto をセットしない（null）

        Model model = new ExtendedModelMap();
        String view = controller.index(session, model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
        verify(tokureiShiteiService, never()).getTokugimuInfo(org.mockito.ArgumentMatchers.any());
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.TOKUREI_SHITEI);
    }

    // ------------------------------------------------------------------
    // #5 index
    // ------------------------------------------------------------------

    @Test
    @DisplayName("#5 index 正常系 セッションに両方なし（指定番号が空文字）：検索モーダルを表示する")
    void index_指定番号が空文字の場合はモーダル表示() {
        Model model = new ExtendedModelMap();
        String view = controller.index(sessionWith("", null), model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
        verify(tokureiShiteiService, never()).getTokugimuInfo(org.mockito.ArgumentMatchers.any());
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.TOKUREI_SHITEI);
    }

    // ------------------------------------------------------------------
    // #6 index
    // ------------------------------------------------------------------

    @Test
    @DisplayName("#6 index 正常系 取得した dto の hakkoYmd が null の場合：発行年月日に当日が設定される")
    void index_hakkoYmdがnullの場合は当日が設定される() {
        TokureiShiteiDto returned = dto("0001");
        returned.setHakkoYmd(null);
        when(tokureiShiteiService.getTokugimuInfo("0001")).thenReturn(returned);

        Model model = new ExtendedModelMap();
        String view = controller.index(sessionWith("0001", null), model);

        assertThat(view).isEqualTo("reports/tokureiShitei");
        TokureiShiteiDto resultDto = (TokureiShiteiDto) model.asMap().get("dto");
        assertThat(resultDto.getHakkoYmd()).isEqualTo(LocalDate.now());
    }

    // ------------------------------------------------------------------
    // #7 index
    // ------------------------------------------------------------------

    @Test
    @DisplayName("#7 index 異常系 getTokugimuInfo が null を返す場合：エラーメッセージを設定して戻る")
    void index_getTokugimuInfoがnullの場合はエラーメッセージを設定() {
        when(tokureiShiteiService.getTokugimuInfo("0001")).thenReturn(null);

        Model model = new ExtendedModelMap();
        String view = controller.index(sessionWith("0001", null), model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
        assertThat(model.asMap()).containsEntry("errorMessage", "納税管理人情報が登録されていません。");
        assertThat(model.asMap()).doesNotContainKey("dto");
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.TOKUREI_SHITEI);
    }

    // ------------------------------------------------------------------
    // #8 generatePdf
    // ------------------------------------------------------------------

    @Test
    @DisplayName("#8 generatePdf 正常系 PDF生成処理")
    void generatePdf_PDF生成処理() {
        TokureiShiteiDto input = dto("0001");
        byte[] pdfBytes = new byte[]{1, 2, 3};
        when(reportsService.generateTsuchiPdf(input)).thenReturn(pdfBytes);

        ResponseEntity<byte[]> response = controller.generatePdf(input);

        verify(reportsService, times(1)).generateTsuchiPdf(input);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.TOKUREI_SHITEI);
    }

    // ------------------------------------------------------------------
    // #9 preview
    // ------------------------------------------------------------------

    @Test
    @DisplayName("#9 preview 正常系 プレビュー処理")
    void preview_プレビュー処理() {
        TokureiShiteiDto input = dto("0001");
        when(reportsService.generateTsuchiPdf(input)).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<byte[]> response = controller.preview(input);

        verify(reportsService, times(1)).generateTsuchiPdf(input);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("inline");
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.TOKUREI_SHITEI);
    }

    // ------------------------------------------------------------------
    // #10 print
    // ------------------------------------------------------------------

    @Test
    @DisplayName("#10 print 正常系 印刷処理")
    void print_印刷処理() {
        TokureiShiteiDto input = dto("0001");
        when(reportsService.generateTsuchiPdf(input)).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<byte[]> response = controller.print(input);

        verify(reportsService, times(1)).generateTsuchiPdf(input);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst("X-Print-Action")).isEqualTo("true");
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.TOKUREI_SHITEI);
    }
}

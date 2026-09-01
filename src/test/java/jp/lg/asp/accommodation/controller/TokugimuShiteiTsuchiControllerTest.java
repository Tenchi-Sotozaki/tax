package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import jp.lg.asp.accommodation.dto.TokugimuJuriTsuchiDto;
import jp.lg.asp.accommodation.service.TokugimuJuriTsuchiReportsService;
import jp.lg.asp.accommodation.service.TokugimuJuriTsuchiService;
import jp.lg.asp.accommodation.util.SessionHelper;

/**
 * 特別徴収義務者申請受理通知 単体テスト（コントローラ）
 *
 * <p>チェックリスト「TokugimuShiteiTsuchiController」の #1〜#10 に1対1で対応する。</p>
 */
@ExtendWith(MockitoExtension.class)
class TokugimuShiteiTsuchiControllerTest {

    @Mock TokugimuJuriTsuchiService tokugimuJuriTsuchiService;
    @Mock TokugimuJuriTsuchiReportsService reportsService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks TokugimuJuriTsuchiController controller;

    private static final String SCREEN_ID = ScreenManagement.TOKUGIMU_JURI_TSUCHI;

    private MockHttpSession sessionWith(String shiteiNo, String gassanShiteiNo) {
        MockHttpSession session = new MockHttpSession();
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setShiteiNo(shiteiNo);
        dto.setGassanShiteiNo(gassanShiteiNo);
        session.setAttribute(SessionHelper.SHITEI_GASSAN_KEY, dto);
        return session;
    }

    private TokugimuJuriTsuchiDto dto(String shiteiNo) {
        TokugimuJuriTsuchiDto dto = new TokugimuJuriTsuchiDto();
        dto.setShiteiNo(shiteiNo);
        dto.setHakkoYmd(LocalDate.of(2025, 4, 1));
        return dto;
    }

    // ==================================================================
    // #1 index 正常系
    // ==================================================================

    @Test
    @DisplayName("#1 index 正常系 セッションに指定番号あり（合算指定番号なし）：通知書画面を返す")
    void index_指定番号ありで通知書画面を返す() {
        TokugimuJuriTsuchiDto expected = dto("0001");
        when(tokugimuJuriTsuchiService.getTokugimuInfo("0001")).thenReturn(expected);

        Model model = new ExtendedModelMap();
        String view = controller.index(sessionWith("0001", null), model);

        assertThat(view).isEqualTo("reports/tokugimuJuriTsuchi");
        assertThat(model.asMap()).containsEntry("dto", expected);
        assertThat(model.asMap()).doesNotContainKey("errorMessage");
        verify(tokugimuJuriTsuchiService, times(1)).getTokugimuInfo("0001");
        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // ==================================================================
    // #2 index 正常系
    // ==================================================================

    @Test
    @DisplayName("#2 index 正常系 セッションに指定番号あり＋合算指定番号あり：エラーメッセージを出力せず通知書画面を返す")
    void index_合算指定番号ありでもエラーなく通知書画面を返す() {
        TokugimuJuriTsuchiDto expected = dto("0001");
        when(tokugimuJuriTsuchiService.getTokugimuInfo("0001")).thenReturn(expected);

        Model model = new ExtendedModelMap();
        String view = controller.index(sessionWith("0001", "G001"), model);

        assertThat(view).isEqualTo("reports/tokugimuJuriTsuchi");
        assertThat(model.asMap()).doesNotContainKey("errorMessage");
        assertThat(model.asMap()).containsEntry("dto", expected);
        verify(tokugimuJuriTsuchiService, times(1)).getTokugimuInfo("0001");
        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // ==================================================================
    // #3 index 正常系
    // ==================================================================

    @Test
    @DisplayName("#3 index 正常系 セッションに合算指定番号のみあり（指定番号なし）：検索モーダルを表示する")
    void index_合算指定番号のみの場合はモーダル表示() {
        Model model = new ExtendedModelMap();
        String view = controller.index(sessionWith(null, "G001"), model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
        verify(tokugimuJuriTsuchiService, never()).getTokugimuInfo(any());
        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // ==================================================================
    // #4 index 正常系
    // ==================================================================

    @Test
    @DisplayName("#4 index 正常系 セッションに両方なし（ShiteiGassanSearchDto が null）：検索モーダルを表示する")
    void index_セッションdtoがnullの場合はモーダル表示() {
        MockHttpSession session = new MockHttpSession();

        Model model = new ExtendedModelMap();
        String view = controller.index(session, model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
        verify(tokugimuJuriTsuchiService, never()).getTokugimuInfo(any());
        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // ==================================================================
    // #5 index 正常系
    // ==================================================================

    @Test
    @DisplayName("#5 index 正常系 セッションに両方なし（指定番号が空文字）：検索モーダルを表示する")
    void index_指定番号が空文字の場合はモーダル表示() {
        Model model = new ExtendedModelMap();
        String view = controller.index(sessionWith("", null), model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
        verify(tokugimuJuriTsuchiService, never()).getTokugimuInfo(any());
        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // ==================================================================
    // #6 index 正常系
    // ==================================================================

    @Test
    @DisplayName("#6 index 正常系 取得した dto の hakkoYmd が null の場合：発行年月日に当日が設定される")
    void index_hakkoYmdがnullの場合は当日が設定される() {
        TokugimuJuriTsuchiDto returned = dto("0001");
        returned.setHakkoYmd(null);
        when(tokugimuJuriTsuchiService.getTokugimuInfo("0001")).thenReturn(returned);

        Model model = new ExtendedModelMap();
        String view = controller.index(sessionWith("0001", null), model);

        assertThat(view).isEqualTo("reports/tokugimuJuriTsuchi");
        TokugimuJuriTsuchiDto resultDto = (TokugimuJuriTsuchiDto) model.asMap().get("dto");
        assertThat(resultDto.getHakkoYmd()).isEqualTo(LocalDate.now());
    }

    // ==================================================================
    // #7 index 異常系
    // ==================================================================

    /**
     * ※現行実装は空の dto を model["dto"] に設定して "reports/tokugimuJuriTsuchi" を返すため、
     * 実装側の修正が必要。
     */
    @Test
    @DisplayName("#7 index 異常系 getTokugimuInfo が null を返す場合：エラーとなる")
    void index_getTokugimuInfoがnullの場合はエラーとなる() {
        when(tokugimuJuriTsuchiService.getTokugimuInfo("0001")).thenReturn(null);

        Model model = new ExtendedModelMap();
        String view = controller.index(sessionWith("0001", null), model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
        assertThat(model.asMap()).containsKey("errorMessage");
        assertThat(model.asMap()).doesNotContainKey("dto");
        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // ==================================================================
    // #8 generatePdf 正常系
    // ==================================================================

    @Test
    @DisplayName("#8 generatePdf 正常系 PDF生成処理")
    void generatePdf_PDF生成処理() {
        TokugimuJuriTsuchiDto input = dto("0001");
        byte[] pdfBytes = new byte[]{1, 2, 3};
        when(reportsService.generateTsuchiPdf(input)).thenReturn(pdfBytes);

        ResponseEntity<byte[]> response = controller.generatePdf(input);

        verify(reportsService, times(1)).generateTsuchiPdf(input);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // ==================================================================
    // #9 preview 正常系
    // ==================================================================

    @Test
    @DisplayName("#9 preview 正常系 プレビュー処理")
    void preview_プレビュー処理() {
        TokugimuJuriTsuchiDto input = dto("0001");
        when(reportsService.generateTsuchiPdf(input)).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<byte[]> response = controller.preview(input);

        verify(reportsService, times(1)).generateTsuchiPdf(input);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("inline");
        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // ==================================================================
    // #10 print 正常系
    // ==================================================================

    @Test
    @DisplayName("#10 print 正常系 印刷処理")
    void print_印刷処理() {
        TokugimuJuriTsuchiDto input = dto("0001");
        when(reportsService.generateTsuchiPdf(input)).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<byte[]> response = controller.print(input);

        verify(reportsService, times(1)).generateTsuchiPdf(input);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst("X-Print-Action")).isEqualTo("true");
        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }
}

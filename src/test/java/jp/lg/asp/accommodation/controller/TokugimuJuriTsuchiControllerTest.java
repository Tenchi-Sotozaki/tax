package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
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
 * <p>チェックリストの #1〜#10 に1対1で対応する。
 * チェックリストはあるべき仕様で書かれている。テストが通るように期待値を実装へ寄せないこと。</p>
 */
@ExtendWith(MockitoExtension.class)
class TokugimuJuriTsuchiControllerTest {

    @Mock TokugimuJuriTsuchiService tokugimuJuriTsuchiService;
    @Mock TokugimuJuriTsuchiReportsService reportsService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks TokugimuJuriTsuchiController controller;

    private static final String SHITEI_NO = "0001";
    private static final String VIEW_TSUCHI = "reports/tokugimuJuriTsuchi";
    private static final String VIEW_MODAL = "tokugimu/tTokugimuReport";

    // ------------------------------------------------------------------
    // テストデータ生成
    // ------------------------------------------------------------------

    /** セッションに ShiteiGassanSearchDto を積んだセッションを返す */
    private MockHttpSession session(String shiteiNo, String gassanShiteiNo) {
        MockHttpSession session = new MockHttpSession();
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setShiteiNo(shiteiNo);
        dto.setGassanShiteiNo(gassanShiteiNo);
        session.setAttribute(SessionHelper.SHITEI_GASSAN_KEY, dto);
        return session;
    }

    private TokugimuJuriTsuchiDto dto(LocalDate hakkoYmd) {
        TokugimuJuriTsuchiDto dto = new TokugimuJuriTsuchiDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setHakkoYmd(hakkoYmd);
        return dto;
    }

    // ==================================================================
    // index
    // ==================================================================

    @Test
    @DisplayName("#1 index 正常系 セッションに指定番号あり（合算指定番号なし）：通知書画面を返す")
    void index_指定番号ありは通知書画面を返す() {
        TokugimuJuriTsuchiDto serviceDto = dto(LocalDate.of(2026, 8, 31));
        when(tokugimuJuriTsuchiService.getTokugimuInfo(SHITEI_NO)).thenReturn(serviceDto);

        Model model = new ExtendedModelMap();

        String view = controller.index(session(SHITEI_NO, null), model);

        assertThat(view).isEqualTo(VIEW_TSUCHI);
        assertThat(model.asMap().get("dto")).isSameAs(serviceDto);
        assertThat(model.asMap()).doesNotContainKey("errorMessage");
        verify(tokugimuJuriTsuchiService, times(1)).getTokugimuInfo(SHITEI_NO);
        verify(accessChecker).checkAccess(ScreenManagement.TOKUGIMU_JURI_TSUCHI);
    }

    @Test
    @DisplayName("#2 index 正常系 セッションに指定番号あり＋合算指定番号あり：エラーメッセージを設定したうえで通知書画面を返す")
    void index_合算指定番号ありはエラーメッセージを設定して画面を表示する() {
        TokugimuJuriTsuchiDto serviceDto = dto(LocalDate.of(2026, 8, 31));
        when(tokugimuJuriTsuchiService.getTokugimuInfo(SHITEI_NO)).thenReturn(serviceDto);

        Model model = new ExtendedModelMap();

        String view = controller.index(session(SHITEI_NO, "G001"), model);

        assertThat(view).isEqualTo(VIEW_TSUCHI);
        assertThat(model.asMap().get("errorMessage"))
                .isEqualTo("合算申告の特別徴収義務者が指定されています。"
                        + "受理通知書は合算申告対象外の特別徴収義務者を指定してください。");
        assertThat(model.asMap().get("dto"))
                .as("エラーメッセージを出しつつ画面自体は表示すること")
                .isSameAs(serviceDto);
        verify(accessChecker).checkAccess(ScreenManagement.TOKUGIMU_JURI_TSUCHI);
    }

    @Test
    @DisplayName("#3 index 正常系 セッションに合算指定番号のみあり（指定番号なし）：検索モーダルを表示する")
    void index_合算指定番号のみは検索モーダル() {
        Model model = new ExtendedModelMap();

        String view = controller.index(session(null, "G001"), model);

        assertThat(view).isEqualTo(VIEW_MODAL);
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
        verify(tokugimuJuriTsuchiService, never()).getTokugimuInfo(anyString());
        verify(accessChecker).checkAccess(ScreenManagement.TOKUGIMU_JURI_TSUCHI);
    }

    @Test
    @DisplayName("#4 index 正常系 セッションに両方なし（ShiteiGassanSearchDto が null）：検索モーダルを表示する")
    void index_セッションDTOがnullは検索モーダル() {
        Model model = new ExtendedModelMap();

        String view = controller.index(new MockHttpSession(), model);

        assertThat(view).isEqualTo(VIEW_MODAL);
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
        verify(tokugimuJuriTsuchiService, never()).getTokugimuInfo(anyString());
        verify(accessChecker).checkAccess(ScreenManagement.TOKUGIMU_JURI_TSUCHI);
    }

    @Test
    @DisplayName("#5 index 正常系 セッションに両方なし（指定番号が空文字）：検索モーダルを表示する")
    void index_指定番号が空文字は検索モーダル() {
        Model model = new ExtendedModelMap();

        String view = controller.index(session("", null), model);

        assertThat(view).isEqualTo(VIEW_MODAL);
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
        verify(tokugimuJuriTsuchiService, never()).getTokugimuInfo(anyString());
        verify(accessChecker).checkAccess(ScreenManagement.TOKUGIMU_JURI_TSUCHI);
    }

    @Test
    @DisplayName("#6 index 正常系 取得した dto の hakkoYmd が null の場合：発行年月日に当日が設定される")
    void index_hakkoYmdがnullなら当日が設定される() {
        when(tokugimuJuriTsuchiService.getTokugimuInfo(SHITEI_NO)).thenReturn(dto(null));

        Model model = new ExtendedModelMap();

        String view = controller.index(session(SHITEI_NO, null), model);

        assertThat(view).isEqualTo(VIEW_TSUCHI);
        TokugimuJuriTsuchiDto result = (TokugimuJuriTsuchiDto) model.asMap().get("dto");
        assertThat(result.getHakkoYmd()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("#7 index 異常系 getTokugimuInfo が null を返す場合：エラーとなる")
    void index_特別徴収義務者情報が取得できない場合はエラー() {
        when(tokugimuJuriTsuchiService.getTokugimuInfo(SHITEI_NO)).thenReturn(null);

        Model model = new ExtendedModelMap();

        String view = controller.index(session(SHITEI_NO, null), model);

        assertThat(model.asMap().get("errorMessage"))
                .as("エラーメッセージが設定されること")
                .isNotNull();
        assertThat(view)
                .as("通知書画面を通常表示しないこと")
                .isNotEqualTo(VIEW_TSUCHI);
        assertThat(model.asMap())
                .as("空のDTOを画面に渡さないこと")
                .doesNotContainKey("dto");
    }

    // ==================================================================
    // generatePdf / preview / print
    // ==================================================================

    @Test
    @DisplayName("#8 generatePdf 正常系 PDF生成処理")
    void generatePdf_PDFを返す() {
        TokugimuJuriTsuchiDto input = dto(LocalDate.now());
        when(reportsService.generateTsuchiPdf(input)).thenReturn(new byte[] { 1, 2, 3 });

        ResponseEntity<byte[]> response = controller.generatePdf(input);

        verify(reportsService, times(1)).generateTsuchiPdf(input);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        verify(accessChecker).checkAccess(ScreenManagement.TOKUGIMU_JURI_TSUCHI);
    }

    @Test
    @DisplayName("#9 preview 正常系 プレビュー処理")
    void preview_inlineヘッダーを返す() {
        TokugimuJuriTsuchiDto input = dto(LocalDate.now());
        when(reportsService.generateTsuchiPdf(input)).thenReturn(new byte[] { 1, 2, 3 });

        ResponseEntity<byte[]> response = controller.preview(input);

        verify(reportsService, times(1)).generateTsuchiPdf(input);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains("inline");
        verify(accessChecker).checkAccess(ScreenManagement.TOKUGIMU_JURI_TSUCHI);
    }

    @Test
    @DisplayName("#10 print 正常系 印刷処理")
    void print_PDFとX_Print_Actionヘッダーを返す() {
        TokugimuJuriTsuchiDto input = dto(LocalDate.now());
        when(reportsService.generateTsuchiPdf(input)).thenReturn(new byte[] { 1, 2, 3 });

        ResponseEntity<byte[]> response = controller.print(input);

        verify(reportsService, times(1)).generateTsuchiPdf(input);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst("X-Print-Action")).isEqualTo("true");
        verify(accessChecker).checkAccess(ScreenManagement.TOKUGIMU_JURI_TSUCHI);
    }
}

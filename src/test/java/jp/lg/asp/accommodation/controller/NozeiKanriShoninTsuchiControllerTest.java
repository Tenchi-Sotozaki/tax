package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.NozeiKanriShoninTsuchiDto;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.entity.Nokan;
import jp.lg.asp.accommodation.service.NokanService;
import jp.lg.asp.accommodation.service.NozeiKanriShoninTsuchiReportsService;
import jp.lg.asp.accommodation.service.NozeiKanriShoninTsuchiService;
import jp.lg.asp.accommodation.util.SessionHelper;

/**
 * 納税管理人承認(不承認)通知書 単体テスト（コントローラ）
 *
 * <p>チェックリストの #1〜#14 に1対1で対応する。</p>
 */
@ExtendWith(MockitoExtension.class)
class NozeiKanriShoninTsuchiControllerTest {

    @Mock NozeiKanriShoninTsuchiService nozeiKanriShoninTsuchiService;
    @Mock NozeiKanriShoninTsuchiReportsService reportsService;
    @Mock NokanService nokanService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks NozeiKanriShoninTsuchiController controller;

    // ------------------------------------------------------------------
    // テストデータ生成ヘルパー
    // ------------------------------------------------------------------

    /** セッションに ShiteiGassanSearchDto をセットして返す */
    private MockHttpSession sessionWith(String shiteiNo, String gassanShiteiNo) {
        MockHttpSession session = new MockHttpSession();
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setShiteiNo(shiteiNo);
        dto.setGassanShiteiNo(gassanShiteiNo);
        session.setAttribute(SessionHelper.SHITEI_GASSAN_KEY, dto);
        return session;
    }

    /** Nokan エンティティを生成する */
    private Nokan nokan(String kbn) {
        Nokan n = new Nokan();
        n.setKbn(kbn);
        n.setJichitaiCd("01100");
        n.setShiteiNo("S001");
        return n;
    }

    /** 最低限のフィールドを持つ NozeiKanriShoninTsuchiDto を生成する */
    private NozeiKanriShoninTsuchiDto dto(LocalDate hakkoYmd) {
        NozeiKanriShoninTsuchiDto dto = new NozeiKanriShoninTsuchiDto();
        dto.setShiteiNo("S001");
        dto.setHakkoYmd(hakkoYmd);
        return dto;
    }

    // ==================================================================
    // #1 index
    // ==================================================================

    @Test
    @DisplayName("#1 index 正常系 セッションに指定番号あり（合算指定番号なし）：通知書画面を返す")
    void index_指定番号ありで通知書画面を返す() {
        NozeiKanriShoninTsuchiDto expected = dto(LocalDate.of(2025, 4, 1));
        when(nokanService.findByJichitaiCdAndShiteiNo("S001")).thenReturn(Optional.of(nokan("1")));
        when(nozeiKanriShoninTsuchiService.getNozeiKanriInfo("S001")).thenReturn(expected);

        Model model = new ExtendedModelMap();
        String view = controller.index(sessionWith("S001", null), model);

        assertThat(view).isEqualTo("reports/nozeiKanrininShoninTsuchi");
        assertThat(model.asMap()).containsEntry("dto", expected);
        verify(nozeiKanriShoninTsuchiService, times(1)).getNozeiKanriInfo("S001");
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.NOZEI_KANRININ_SHONIN_TSUCHI);
    }

    // ==================================================================
    // #2 index
    // ==================================================================

    @Test
    @DisplayName("#2 index 正常系 セッションに指定番号あり＋合算指定番号あり：合算指定番号は参照されず通知書画面を返す")
    void index_合算指定番号ありでも指定番号のみ使用する() {
        NozeiKanriShoninTsuchiDto expected = dto(LocalDate.of(2025, 4, 1));
        when(nokanService.findByJichitaiCdAndShiteiNo("S001")).thenReturn(Optional.of(nokan("1")));
        when(nozeiKanriShoninTsuchiService.getNozeiKanriInfo("S001")).thenReturn(expected);

        Model model = new ExtendedModelMap();
        String view = controller.index(sessionWith("S001", "G001"), model);

        assertThat(view).isEqualTo("reports/nozeiKanrininShoninTsuchi");
        verify(nozeiKanriShoninTsuchiService, times(1)).getNozeiKanriInfo("S001");
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.NOZEI_KANRININ_SHONIN_TSUCHI);
    }

    // ==================================================================
    // #3 index
    // ==================================================================

    @Test
    @DisplayName("#3 index 正常系 セッションに合算指定番号のみあり（指定番号なし）：検索モーダルを表示する")
    void index_合算指定番号のみの場合はモーダル表示() {
        Model model = new ExtendedModelMap();
        String view = controller.index(sessionWith(null, "G001"), model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
        verify(nokanService, never()).findByJichitaiCdAndShiteiNo(any());
        verify(nozeiKanriShoninTsuchiService, never()).getNozeiKanriInfo(any());
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.NOZEI_KANRININ_SHONIN_TSUCHI);
    }

    // ==================================================================
    // #4 index
    // ==================================================================

    @Test
    @DisplayName("#4 index 正常系 セッションに両方なし（ShiteiGassanSearchDto が null）：検索モーダルを表示する")
    void index_セッションdtoがnullの場合はモーダル表示() {
        MockHttpSession session = new MockHttpSession();
        // ShiteiGassanSearchDto をセットしない（null）

        Model model = new ExtendedModelMap();
        String view = controller.index(session, model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
        verify(nokanService, never()).findByJichitaiCdAndShiteiNo(any());
        verify(nozeiKanriShoninTsuchiService, never()).getNozeiKanriInfo(any());
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.NOZEI_KANRININ_SHONIN_TSUCHI);
    }

    // ==================================================================
    // #5 index
    // ==================================================================

    @Test
    @DisplayName("#5 index 正常系 セッションに両方なし（指定番号が空文字）：検索モーダルを表示する")
    void index_指定番号が空文字の場合はモーダル表示() {
        Model model = new ExtendedModelMap();
        String view = controller.index(sessionWith("", null), model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.NOZEI_KANRININ_SHONIN_TSUCHI);
    }

    // ==================================================================
    // #6 index
    // ==================================================================

    @Test
    @DisplayName("#6 index 正常系 取得した dto の hakkoYmd が null の場合：発行年月日に当日が設定される")
    void index_hakkoYmdがnullの場合は当日が設定される() {
        NozeiKanriShoninTsuchiDto returned = dto(null);
        when(nokanService.findByJichitaiCdAndShiteiNo("S001")).thenReturn(Optional.of(nokan("1")));
        when(nozeiKanriShoninTsuchiService.getNozeiKanriInfo("S001")).thenReturn(returned);

        Model model = new ExtendedModelMap();
        String view = controller.index(sessionWith("S001", null), model);

        assertThat(view).isEqualTo("reports/nozeiKanrininShoninTsuchi");
        NozeiKanriShoninTsuchiDto resultDto = (NozeiKanriShoninTsuchiDto) model.asMap().get("dto");
        assertThat(resultDto.getHakkoYmd()).isEqualTo(LocalDate.now());
    }

    // ==================================================================
    // #7 index
    // ==================================================================

    @Test
    @DisplayName("#7 index 異常系 納税管理人が未登録の場合：エラーメッセージを設定して戻る")
    void index_nokanが存在しない場合はエラーメッセージを設定() {
        when(nokanService.findByJichitaiCdAndShiteiNo("S001")).thenReturn(Optional.empty());

        Model model = new ExtendedModelMap();
        String view = controller.index(sessionWith("S001", null), model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
        assertThat(model.asMap()).containsEntry("errorMessage", "納税管理人情報が登録されていません。");
        verify(nozeiKanriShoninTsuchiService, never()).getNozeiKanriInfo(any());
    }

    // ==================================================================
    // #8 index
    // ==================================================================

    @Test
    @DisplayName("#8 index 異常系 納税管理人の kbn が \"3\"（選任免除）の場合：エラーメッセージを設定して戻る")
    void index_nokanKbnが3の場合はエラーメッセージを設定() {
        when(nokanService.findByJichitaiCdAndShiteiNo("S001")).thenReturn(Optional.of(nokan("3")));

        Model model = new ExtendedModelMap();
        String view = controller.index(sessionWith("S001", null), model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
        assertThat(model.asMap()).containsEntry("errorMessage",
                "納税管理人が選任免除のため、承認(不承認)通知書は発行できません。");
        verify(nozeiKanriShoninTsuchiService, never()).getNozeiKanriInfo(any());
    }

    // ==================================================================
    // #9 index
    // ==================================================================

    @Test
    @DisplayName("#9 index 異常系 getNozeiKanriInfo が RuntimeException をスローする場合：エラーメッセージを設定して画面を表示する")
    void index_getNozeiKanriInfoが例外をスローした場合はエラーメッセージを設定して画面を返す() {
        when(nokanService.findByJichitaiCdAndShiteiNo("S001")).thenReturn(Optional.of(nokan("1")));
        when(nozeiKanriShoninTsuchiService.getNozeiKanriInfo("S001"))
                .thenThrow(new RuntimeException("エラー"));

        Model model = new ExtendedModelMap();
        String view = controller.index(sessionWith("S001", null), model);

        assertThat(view).isEqualTo("reports/nozeiKanrininShoninTsuchi");
        assertThat(model.asMap()).containsEntry("errorMessage", "指定番号: S001 の情報が見つかりません。");
        NozeiKanriShoninTsuchiDto resultDto = (NozeiKanriShoninTsuchiDto) model.asMap().get("dto");
        assertThat(resultDto.getHakkoYmd()).isEqualTo(LocalDate.now());
    }

    // ==================================================================
    // #10 generatePdf
    // ==================================================================

    @Test
    @DisplayName("#10 generatePdf 正常系 hakkoYmd が設定されている場合")
    void generatePdf_hakkoYmdが設定されている場合はPDFを返す() {
        NozeiKanriShoninTsuchiDto input = dto(LocalDate.now());
        when(reportsService.generateTsuchiPdf(input)).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<byte[]> response = controller.generatePdf(input);

        verify(reportsService, times(1)).generateTsuchiPdf(input);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.NOZEI_KANRININ_SHONIN_TSUCHI);
    }

    // ==================================================================
    // #11 generatePdf
    // ==================================================================

    @Test
    @DisplayName("#11 generatePdf 異常系 hakkoYmd が null の場合")
    void generatePdf_hakkoYmdがnullの場合は400を返す() {
        NozeiKanriShoninTsuchiDto input = dto(null);

        ResponseEntity<byte[]> response = controller.generatePdf(input);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(reportsService, never()).generateTsuchiPdf(any());
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.NOZEI_KANRININ_SHONIN_TSUCHI);
    }

    // ==================================================================
    // #12 generatePdf
    // ==================================================================

    @Test
    @DisplayName("#12 generatePdf 異常系 reportsService.generateTsuchiPdf が例外をスローした場合")
    void generatePdf_reportsServiceが例外をスローした場合は500を返す() {
        NozeiKanriShoninTsuchiDto input = dto(LocalDate.now());
        when(reportsService.generateTsuchiPdf(input)).thenThrow(new RuntimeException("PDF生成失敗"));

        ResponseEntity<byte[]> response = controller.generatePdf(input);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.NOZEI_KANRININ_SHONIN_TSUCHI);
    }

    // ==================================================================
    // #13 preview
    // ==================================================================

    @Test
    @DisplayName("#13 preview 正常系 hakkoYmd が設定されている場合")
    void preview_hakkoYmdが設定されている場合はinlineヘッダーを返す() {
        NozeiKanriShoninTsuchiDto input = dto(LocalDate.now());
        when(reportsService.generateTsuchiPdf(input)).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<byte[]> response = controller.preview(input);

        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("inline");
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.NOZEI_KANRININ_SHONIN_TSUCHI);
    }

    // ==================================================================
    // #14 print
    // ==================================================================

    @Test
    @DisplayName("#14 print 正常系 hakkoYmd が設定されている場合")
    void print_hakkoYmdが設定されている場合はPDFとX_Print_Actionヘッダーを返す() {
        NozeiKanriShoninTsuchiDto input = dto(LocalDate.now());
        when(reportsService.generateTsuchiPdf(input)).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<byte[]> response = controller.print(input);

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst("X-Print-Action")).isEqualTo("true");
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.NOZEI_KANRININ_SHONIN_TSUCHI);
    }
}

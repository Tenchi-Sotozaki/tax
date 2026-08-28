package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;

import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.dto.TokugimuShiteiTsuchiDto;
import jp.lg.asp.accommodation.service.TokugimuShiteiTsuchiReportsService;
import jp.lg.asp.accommodation.service.TokugimuShiteiTsuchiService;
import jp.lg.asp.accommodation.util.SessionHelper;

@ExtendWith(MockitoExtension.class)
class TokugimuShiteiTsuchiControllerTest {

    @Mock TokugimuShiteiTsuchiService tokugimuShiteiTsuchiService;
    @Mock TokugimuShiteiTsuchiReportsService reportsService;
    @Mock ScreenAccessChecker accessChecker;
    @InjectMocks TokugimuShiteiTsuchiController controller;

    private static final String SCREEN_ID = ScreenManagement.TOKUGIMU_SHITEI_TSUCHI;

    private HttpSession sessionWithShiteiNo(String shiteiNo) {
        MockHttpSession session = new MockHttpSession();
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setShiteiNo(shiteiNo);
        SessionHelper.saveShiteiGassan(session, dto);
        return session;
    }

    // ── index ─────────────────────────────────────────────────────

    @Test
    void index_セッションに指定番号あり_getTokugimuInfoが値を返す場合() {
        HttpSession session = sessionWithShiteiNo("S0000001");
        TokugimuShiteiTsuchiDto info = new TokugimuShiteiTsuchiDto();
        info.setShiteiNo("S0000001");
        info.setHakkoYmd(LocalDate.of(2025, 4, 1));
        when(tokugimuShiteiTsuchiService.getTokugimuInfo("S0000001")).thenReturn(info);
        Model model = new ExtendedModelMap();

        String view = controller.index(session, model);

        assertThat(view).isEqualTo("reports/tokugimuShiteiTsuchi");
        assertThat(model.asMap().get("dto")).isNotNull();
        verify(accessChecker).checkAccess(SCREEN_ID);
    }

    @Test
    void index_セッションのShiteiGassanSearchDtoがnullの場合() {
        MockHttpSession session = new MockHttpSession();
        Model model = new ExtendedModelMap();

        String view = controller.index(session, model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
        assertThat(model.asMap().get("showShiteiGassanModal")).isEqualTo(true);
        verify(accessChecker).checkAccess(SCREEN_ID);
    }

    @Test
    void index_セッションのshiteiNoが空文字の場合() {
        HttpSession session = sessionWithShiteiNo("");
        Model model = new ExtendedModelMap();

        String view = controller.index(session, model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
        assertThat(model.asMap().get("showShiteiGassanModal")).isEqualTo(true);
    }

    @Test
    void index_getTokugimuInfoの戻り値のhakkoYmdがnullの場合() {
        HttpSession session = sessionWithShiteiNo("S0000001");
        TokugimuShiteiTsuchiDto info = new TokugimuShiteiTsuchiDto();
        info.setShiteiNo("S0000001");
        info.setHakkoYmd(null);
        when(tokugimuShiteiTsuchiService.getTokugimuInfo("S0000001")).thenReturn(info);
        Model model = new ExtendedModelMap();

        controller.index(session, model);

        TokugimuShiteiTsuchiDto dto = (TokugimuShiteiTsuchiDto) model.asMap().get("dto");
        assertThat(dto.getHakkoYmd()).isEqualTo(LocalDate.now());
    }

    // ── generatePdf ───────────────────────────────────────────────

    @Test
    void generatePdf_PDF生成処理() {
        TokugimuShiteiTsuchiDto dto = new TokugimuShiteiTsuchiDto();
        dto.setShiteiNo("S0000001");
        byte[] pdfData = new byte[]{1, 2, 3};
        when(reportsService.generateTsuchiPdf(dto)).thenReturn(pdfData);

        ResponseEntity<byte[]> response = controller.generatePdf(dto);

        verify(reportsService).generateTsuchiPdf(dto);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        verify(accessChecker).checkAccess(SCREEN_ID);
    }

    // ── preview ───────────────────────────────────────────────────

    @Test
    void preview_プレビュー処理() {
        TokugimuShiteiTsuchiDto dto = new TokugimuShiteiTsuchiDto();
        dto.setShiteiNo("S0000001");
        when(reportsService.generateTsuchiPdf(dto)).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<byte[]> response = controller.preview(dto);

        verify(reportsService).generateTsuchiPdf(dto);
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("inline");
        verify(accessChecker).checkAccess(SCREEN_ID);
    }

    // ── print ─────────────────────────────────────────────────────

    @Test
    void print_印刷処理() {
        TokugimuShiteiTsuchiDto dto = new TokugimuShiteiTsuchiDto();
        dto.setShiteiNo("S0000001");
        when(reportsService.generateTsuchiPdf(dto)).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<byte[]> response = controller.print(dto);

        verify(reportsService).generateTsuchiPdf(dto);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst("X-Print-Action")).isEqualTo("true");
        verify(accessChecker).checkAccess(SCREEN_ID);
    }
}

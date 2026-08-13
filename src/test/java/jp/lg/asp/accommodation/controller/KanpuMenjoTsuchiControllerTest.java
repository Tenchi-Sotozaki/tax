package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;

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

import jakarta.servlet.http.HttpSession;
import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.KanpuMenjoTsuchiDto;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.dto.TokugimuForm;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.service.KanpuMenjoTsuchiReportsService;
import jp.lg.asp.accommodation.service.ReportsCommonService;
import jp.lg.asp.accommodation.service.TokugimuService;
import jp.lg.asp.accommodation.util.SessionHelper;

/**
 * 徴収不能額の還付又は納入義務の免除決定通知書 Controller（ACCOMMODATION_TAX-362）の単体テスト。
 *
 * 画面表示の分岐と、PDF・プレビュー・印刷のレスポンスを検証する。
 */
@ExtendWith(MockitoExtension.class)
class KanpuMenjoTsuchiControllerTest {

    @Mock TokugimuService tokugimuService;
    @Mock KanpuMenjoTsuchiReportsService kanpuMenjoTsuchiReportsService;
    @Mock JichitaiRepository jichitaiRepository;
    @Mock ReportsCommonService reportsCommonService;
    @Mock ScreenAccessChecker accessChecker;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks KanpuMenjoTsuchiController controller;

    private static final String SHITEI_NO = "00100001";
    private static final byte[] PDF = "%PDF-1.4 dummy".getBytes();

    private HttpSession sessionWith(String shiteiNo) {
        MockHttpSession session = new MockHttpSession();
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setShiteiNo(shiteiNo);
        SessionHelper.saveShiteiGassan(session, dto);
        return session;
    }

    private KanpuMenjoTsuchiDto dto() {
        KanpuMenjoTsuchiDto dto = new KanpuMenjoTsuchiDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setHakkoYmd(LocalDate.of(2026, 4, 1));
        return dto;
    }

    private TokugimuForm tokugimuForm() {
        TokugimuForm form = new TokugimuForm();
        form.setName("株式会社ホテルA");
        form.setFacilityName("ホテルA 札幌");
        form.setTokugimuAddress("札幌市中央区北1条西1丁目");
        return form;
    }

    // ===================================================================
    // index — 画面表示
    // ===================================================================

    @Test
    void index_指定番号が無ければ特別徴収義務者指定モーダルを表示する() {
        Model model = new ExtendedModelMap();

        String view = controller.index(new MockHttpSession(), model);

        assertThat(view).isEqualTo("reports/kanpuMenjoTsuchi");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
        verify(tokugimuService, never()).getTokugimuByShiteiNo(any());
    }

    @Test
    void index_特別徴収義務者が見つからなければエラー画面を返す() {
        when(tokugimuService.getTokugimuByShiteiNo(SHITEI_NO)).thenReturn(null);
        Model model = new ExtendedModelMap();

        String view = controller.index(sessionWith(SHITEI_NO), model);

        assertThat(view).isEqualTo("error");
        assertThat(model.asMap()).containsKey("errorMessage");
    }

    @Test
    void index_例外が発生したらエラー画面を返す() {
        when(tokugimuService.getTokugimuByShiteiNo(SHITEI_NO))
                .thenThrow(new RuntimeException("DB接続エラー"));
        Model model = new ExtendedModelMap();

        String view = controller.index(sessionWith(SHITEI_NO), model);

        assertThat(view).isEqualTo("error");
        assertThat(model.asMap().get("errorMessage").toString()).contains("エラー");
    }

    // ===================================================================
    // PDF / プレビュー / 印刷
    // ===================================================================

    @Test
    void generatePdf_添付ファイルとしてPDFを返す() {
        KanpuMenjoTsuchiDto dto = dto();
        when(kanpuMenjoTsuchiReportsService.generateTsuchiPdf(dto)).thenReturn(PDF);

        ResponseEntity<byte[]> response = controller.generatePdf(dto, null);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("attachment");
        assertThat(response.getHeaders().getContentLength()).isEqualTo(PDF.length);
        assertThat(response.getBody()).isEqualTo(PDF);
    }

    @Test
    void generatePdf_ファイル名に指定番号が含まれる() {
        KanpuMenjoTsuchiDto dto = dto();
        when(kanpuMenjoTsuchiReportsService.generateTsuchiPdf(dto)).thenReturn(PDF);

        ResponseEntity<byte[]> response = controller.generatePdf(dto, null);

        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains(SHITEI_NO);
    }

    @Test
    void generatePdf_サービスが例外を投げたら500を返す() {
        KanpuMenjoTsuchiDto dto = dto();
        when(kanpuMenjoTsuchiReportsService.generateTsuchiPdf(dto))
                .thenThrow(new RuntimeException("PDF生成に失敗しました"));

        ResponseEntity<byte[]> response = controller.generatePdf(dto, null);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    @Test
    void preview_インラインでPDFを返す() {
        KanpuMenjoTsuchiDto dto = dto();
        when(kanpuMenjoTsuchiReportsService.generateTsuchiPdf(dto)).thenReturn(PDF);

        ResponseEntity<byte[]> response = controller.preview(dto, null);

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("inline");
        assertThat(response.getBody()).isEqualTo(PDF);
    }

    @Test
    void print_インラインでPDFを返す() {
        KanpuMenjoTsuchiDto dto = dto();
        when(kanpuMenjoTsuchiReportsService.generateTsuchiPdf(dto)).thenReturn(PDF);

        ResponseEntity<byte[]> response = controller.print(dto, null);

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("inline");
        assertThat(response.getBody()).isEqualTo(PDF);
    }

    @Test
    void print_サービスが例外を投げたら500を返す() {
        KanpuMenjoTsuchiDto dto = dto();
        when(kanpuMenjoTsuchiReportsService.generateTsuchiPdf(dto))
                .thenThrow(new RuntimeException("PDF生成に失敗しました"));

        ResponseEntity<byte[]> response = controller.print(dto, null);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }
}

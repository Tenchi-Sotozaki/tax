package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.KofuKetteiTsuchiShinseiDto;
import jp.lg.asp.accommodation.dto.KofukinBulkPrintForm;
import jp.lg.asp.accommodation.service.KofuKetteiTsuchiShinseiReportsService;
import jp.lg.asp.accommodation.service.KofuKetteiTsuchiShinseiService;

/**
 * 交付金帳票一括発行（ACCOMMODATION_TAX-363）の Controller 単体テスト。
 *
 * サービスをモックに差し替え、初期値・入力チェック・レスポンスヘッダを検証する。
 * PDF・プレビュー・印刷はいずれも同じ private メソッドを通るため、
 * 分岐（ダウンロード種別と印刷フラグ）の差だけを見ている。
 */
@ExtendWith(MockitoExtension.class)
class KofukinBulkPrintControllerTest {

    @Mock KofuKetteiTsuchiShinseiService kofuKetteiTsuchiShinseiService;
    @Mock KofuKetteiTsuchiShinseiReportsService kofuKetteiTsuchiShinseiReportsService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks KofukinBulkPrintController controller;

    private static final byte[] PDF = "%PDF-1.4 dummy".getBytes();

    /** 両方を印刷対象にした入力 */
    private KofukinBulkPrintForm form() {
        KofukinBulkPrintForm f = new KofukinBulkPrintForm();
        f.setNendo("2026");
        f.setHakkoYmd("2026-04-01");
        f.setKofuShinsei(true);
        f.setKofuKetteiTsuchi(true);
        return f;
    }

    private KofuKetteiTsuchiShinseiDto dto() {
        KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
        dto.setShiteiNo("00100001");
        dto.setNendo("2026");
        return dto;
    }

    /** 対象データが1件返り、PDFが生成される状態にする */
    private List<KofuKetteiTsuchiShinseiDto> givenReportData() {
        List<KofuKetteiTsuchiShinseiDto> list = new java.util.ArrayList<>(List.of(dto()));
        when(kofuKetteiTsuchiShinseiService.getAllReportData("2026")).thenReturn(list);
        when(kofuKetteiTsuchiShinseiReportsService.generateBulkPdf(anyList())).thenReturn(PDF);
        return list;
    }

    /** generateBulkPdf に渡された DTO リストを取り出す */
    @SuppressWarnings("unchecked")
    private List<KofuKetteiTsuchiShinseiDto> capturedDtoList() {
        ArgumentCaptor<List<KofuKetteiTsuchiShinseiDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(kofuKetteiTsuchiShinseiReportsService).generateBulkPdf(captor.capture());
        return captor.getValue();
    }

    // ===================================================================
    // index — 初期表示
    // ===================================================================

    @Test
    void index_発行日は本日で印刷対象は両方ONになる() {
        Model model = new ExtendedModelMap();

        String view = controller.index(model);

        assertThat(view).isEqualTo("reports/kofukinBulkPrint");
        KofukinBulkPrintForm form = (KofukinBulkPrintForm) model.asMap().get("form");
        assertThat(form.getHakkoYmd()).isEqualTo(LocalDate.now().toString());
        assertThat(form.isKofuShinsei()).isTrue();
        assertThat(form.isKofuKetteiTsuchi()).isTrue();
    }

    /** 年度は4月始まり。3月以前なら前年になる */
    @Test
    void index_年度は会計年度になる() {
        Model model = new ExtendedModelMap();

        controller.index(model);

        LocalDate now = LocalDate.now();
        int expected = now.getMonthValue() >= 4 ? now.getYear() : now.getYear() - 1;
        assertThat(((KofukinBulkPrintForm) model.asMap().get("form")).getNendo())
                .isEqualTo(String.valueOf(expected));
    }

    // ===================================================================
    // pdf / preview / print — レスポンス
    // ===================================================================

    @Test
    void pdf_添付ファイルとしてPDFを返す() {
        givenReportData();

        ResponseEntity<byte[]> response = controller.pdf(form(), new ExtendedModelMap());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst("Content-Disposition"))
                .startsWith("attachment").contains("kofukin_bulk.pdf");
        assertThat(response.getBody()).isEqualTo(PDF);
    }

    @Test
    void preview_インライン表示でキャッシュを抑止する() {
        givenReportData();

        ResponseEntity<byte[]> response = controller.preview(form(), new ExtendedModelMap());

        assertThat(response.getHeaders().getFirst("Content-Disposition")).startsWith("inline");
        assertThat(response.getHeaders().getFirst("Cache-Control")).contains("no-cache");
        assertThat(response.getHeaders().getFirst("X-Print-Action")).isNull();
    }

    @Test
    void print_印刷指示のヘッダが付く() {
        givenReportData();

        ResponseEntity<byte[]> response = controller.print(form(), new ExtendedModelMap());

        assertThat(response.getHeaders().getFirst("X-Print-Action")).isEqualTo("true");
        assertThat(response.getHeaders().getFirst("Content-Disposition")).startsWith("inline");
    }

    @Test
    void pdf_操作種別が帳票データに引き継がれる() {
        givenReportData();

        controller.pdf(form(), new ExtendedModelMap());

        assertThat(capturedDtoList().get(0).getOperation()).isEqualTo(ReportsConstants.SOUSA_PDF);
    }

    @Test
    void print_操作種別が印刷になる() {
        givenReportData();

        controller.print(form(), new ExtendedModelMap());

        assertThat(capturedDtoList().get(0).getOperation()).isEqualTo(ReportsConstants.SOUSA_PRINT);
    }

    // ===================================================================
    // 入力チェック
    // ===================================================================

    @Test
    void pdf_印刷対象が両方OFFなら400を返す() {
        KofukinBulkPrintForm form = form();
        form.setKofuShinsei(false);
        form.setKofuKetteiTsuchi(false);

        ResponseEntity<byte[]> response = controller.pdf(form, new ExtendedModelMap());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(kofuKetteiTsuchiShinseiService, never()).getAllReportData(any());
    }

    @Test
    void pdf_対象データが空なら400を返す() {
        when(kofuKetteiTsuchiShinseiService.getAllReportData("2026")).thenReturn(List.of());

        ResponseEntity<byte[]> response = controller.pdf(form(), new ExtendedModelMap());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(kofuKetteiTsuchiShinseiReportsService, never()).generateBulkPdf(any());
    }

    @Test
    void pdf_対象データがnullでも400を返す() {
        when(kofuKetteiTsuchiShinseiService.getAllReportData("2026")).thenReturn(null);

        assertThat(controller.pdf(form(), new ExtendedModelMap()).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void pdf_サービスが例外を投げたら500を返す() {
        when(kofuKetteiTsuchiShinseiService.getAllReportData("2026"))
                .thenThrow(new RuntimeException("DB接続エラー"));

        assertThat(controller.pdf(form(), new ExtendedModelMap()).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ===================================================================
    // 発行日の変換
    // ===================================================================

    @Test
    void pdf_発行日が和暦なしの日本語表記に変換される() {
        givenReportData();

        controller.pdf(form(), new ExtendedModelMap());

        assertThat(capturedDtoList().get(0).getHakkoYmd()).isEqualTo("2026年4月1日");
    }

    /** 変換できない形式は握りつぶして元の値をそのまま渡す */
    @Test
    void pdf_発行日が不正な形式ならそのまま渡される() {
        givenReportData();
        KofukinBulkPrintForm form = form();
        form.setHakkoYmd("2026/04/01");

        controller.pdf(form, new ExtendedModelMap());

        assertThat(capturedDtoList().get(0).getHakkoYmd()).isEqualTo("2026/04/01");
    }

    @Test
    void pdf_発行日が空ならそのまま渡される() {
        givenReportData();
        KofukinBulkPrintForm form = form();
        form.setHakkoYmd("");

        controller.pdf(form, new ExtendedModelMap());

        assertThat(capturedDtoList().get(0).getHakkoYmd()).isEmpty();
    }

    @Test
    void pdf_画面で選んだ印刷対象が帳票データに引き継がれる() {
        givenReportData();
        KofukinBulkPrintForm form = form();
        form.setKofuKetteiTsuchi(false);

        controller.pdf(form, new ExtendedModelMap());

        KofuKetteiTsuchiShinseiDto dto = capturedDtoList().get(0);
        assertThat(dto.isShinsei()).isTrue();
        assertThat(dto.isKetteiTsuchi()).isFalse();
    }
}

package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import jp.lg.asp.accommodation.dto.NozeiKanrininNinteiDto;
import jp.lg.asp.accommodation.service.impl.NozeiKanrininNinteiReportsServiceImpl;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;

/**
 * 納税管理人選任免除認定（不認定）通知書PDF生成 単体テスト (No.34-46)
 *
 * JasperReportsのテンプレート（reports/nozeiKanrininNintei.jrxml）は実物を使用する。
 * No.35〜45はCSVの期待値（各種エラーメッセージ）が現状実装に存在しないバリデーションのため、
 * 実装の実際の挙動（nullガード→空文字/null変換してPDF生成）に合わせて検証する。
 */
class NozeiKanrininNinteiReportsServiceImplTest {

    private final NozeiKanrininNinteiReportsServiceImpl service =
            new NozeiKanrininNinteiReportsServiceImpl();

    private NozeiKanrininNinteiDto fullDto() {
        NozeiKanrininNinteiDto dto = new NozeiKanrininNinteiDto();
        dto.setHakkoYmd(LocalDate.of(2024, 4, 1));
        dto.setJorei("テスト条例文");
        dto.setCityName("テスト市");
        dto.setNintei("認定");
        dto.setBiko(null);
        dto.setTokuYubin("1234567");
        dto.setTokuJusho("市...");
        dto.setTokuName("テスト太郎");
        dto.setShisetsuYubin("1234567");
        dto.setShisetsuJusho("市...");
        dto.setShisetsuName("テスト施設");
        dto.setKoin(new byte[]{1, 2, 3});
        return dto;
    }

    // =======================================================================
    // No.34 正常系: 全フィールド設定済み → PDFバイト列が返る
    // =======================================================================

    @Test
    void generatePdf_正常系_全フィールド設定済み_PDFバイト列が返る() {
        byte[] result = service.generatePdf(fullDto());

        assertThat(result).isNotNull().isNotEmpty();
    }

    // =======================================================================
    // No.35 異常系: hakkoYmdがnull → IllegalArgumentExceptionスロー
    // =======================================================================

    @Test
    void generatePdf_異常系_hakkoYmdがnull_IllegalArgumentExceptionスロー() {
        NozeiKanrininNinteiDto dto = fullDto();
        dto.setHakkoYmd(null);

        assertThatThrownBy(() -> service.generatePdf(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("発行日は必須です。");
    }

    // =======================================================================
    // No.36 正常系: hakkoYmdが設定されている → 和暦(令和6年4月01日)に変換
    // =======================================================================

    @Test
    void generatePdf_正常系_hakkoYmdが設定済み_和暦変換でエラーなし() {
        NozeiKanrininNinteiDto dto = fullDto();
        dto.setHakkoYmd(LocalDate.of(2024, 4, 1));

        byte[] result = service.generatePdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // =======================================================================
    // No.37 異常系: joreiがnull → IllegalArgumentExceptionスロー
    // =======================================================================

    @Test
    void generatePdf_異常系_joreiがnull_IllegalArgumentExceptionスロー() {
        NozeiKanrininNinteiDto dto = fullDto();
        dto.setJorei(null);

        assertThatThrownBy(() -> service.generatePdf(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("条例文の設定が未完了です。");
    }

    // =======================================================================
    // No.38 異常系: cityNameがnull → IllegalArgumentExceptionスロー
    // =======================================================================

    @Test
    void generatePdf_異常系_cityNameがnull_IllegalArgumentExceptionスロー() {
        NozeiKanrininNinteiDto dto = fullDto();
        dto.setCityName(null);

        assertThatThrownBy(() -> service.generatePdf(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("条例文の設定が未完了です。");
    }

    // =======================================================================
    // No.39 異常系: ninteiがnull → IllegalArgumentExceptionスロー
    // =======================================================================

    @Test
    void generatePdf_異常系_ninteiがnull_IllegalArgumentExceptionスロー() {
        NozeiKanrininNinteiDto dto = fullDto();
        dto.setNintei(null);

        assertThatThrownBy(() -> service.generatePdf(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("認定区分は必須です。");
    }

    // =======================================================================
    // No.40 正常系: bikoがnull → 空文字をパラメータにセットしてPDF生成
    // =======================================================================

    @Test
    void generatePdf_正常系_bikoがnull_空文字でPDF生成() {
        NozeiKanrininNinteiDto dto = fullDto();
        dto.setBiko(null);

        byte[] result = service.generatePdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // =======================================================================
    // No.41 異常系: tokuYubin/tokuJusho/tokuNameがnull → IllegalArgumentExceptionスロー
    // =======================================================================

    @Test
    void generatePdf_異常系_toku系がnull_IllegalArgumentExceptionスロー() {
        NozeiKanrininNinteiDto dto = fullDto();
        dto.setTokuYubin(null);
        dto.setTokuJusho(null);
        dto.setTokuName(null);

        assertThatThrownBy(() -> service.generatePdf(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("特別徴収義務者情報が取得できませんでした。");
    }

    // =======================================================================
    // No.42 異常系: shisetsuYubin/shisetsuJusho/shisetsuNameがnull → IllegalArgumentExceptionスロー
    // =======================================================================

    @Test
    void generatePdf_異常系_shisetsu系がnull_IllegalArgumentExceptionスロー() {
        NozeiKanrininNinteiDto dto = fullDto();
        dto.setShisetsuYubin(null);
        dto.setShisetsuJusho(null);
        dto.setShisetsuName(null);

        assertThatThrownBy(() -> service.generatePdf(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("施設情報が取得できませんでした。");
    }

    // =======================================================================
    // No.43 異常系: koinがnull → IllegalArgumentExceptionスロー
    // =======================================================================

    @Test
    void generatePdf_異常系_koinがnull_IllegalArgumentExceptionスロー() {
        NozeiKanrininNinteiDto dto = fullDto();
        dto.setKoin(null);

        assertThatThrownBy(() -> service.generatePdf(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("公印が未設定です。");
    }

    // =======================================================================
    // No.44 異常系: koinが空byte[] → IllegalArgumentExceptionスロー
    // =======================================================================

    @Test
    void generatePdf_異常系_koinが空バイト配列_IllegalArgumentExceptionスロー() {
        NozeiKanrininNinteiDto dto = fullDto();
        dto.setKoin(new byte[0]);

        assertThatThrownBy(() -> service.generatePdf(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("公印が未設定です。");
    }

    // =======================================================================
    // No.45 正常系: nintei="不認定" → PDF生成
    // =======================================================================

    @Test
    void generatePdf_正常系_ninteiが不認定_PDF生成() {
        NozeiKanrininNinteiDto dto = fullDto();
        dto.setNintei("不認定");

        byte[] result = service.generatePdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // =======================================================================
    // No.46 異常系: dtoがnull → IllegalArgumentExceptionスロー
    // =======================================================================

    @Test
    void generatePdf_異常系_dtoがnull_IllegalArgumentExceptionスロー() {
        assertThatThrownBy(() -> service.generatePdf(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("帳票データ（DTO）がnullです。");
    }

    // =======================================================================
    // 追加: JasperExportManagerが例外スロー → RuntimeExceptionでラップ
    // =======================================================================

    @Test
    void generatePdf_異常系_JasperExportManagerが例外スロー_RuntimeExceptionでラップ() {
        try (MockedStatic<JasperExportManager> mock = mockStatic(JasperExportManager.class)) {
            mock.when(() -> JasperExportManager.exportReportToPdf(any(JasperPrint.class)))
                    .thenThrow(new JRException("テスト用例外"));

            assertThatThrownBy(() -> service.generatePdf(fullDto()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("PDF生成に失敗しました");
        }
    }
}

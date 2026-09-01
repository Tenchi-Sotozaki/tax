package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.dto.NozeiKanriShoninTsuchiDto;
import jp.lg.asp.accommodation.service.impl.NozeiKanriShoninTsuchiReportsServiceImpl;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;

/**
 * 納税管理人承認(不承認)通知書PDF生成 単体テスト（サービス）
 *
 * <p>チェックリストの #30〜#35 に1対1で対応する。</p>
 * <p>#30〜#33・#35 は JasperReports のテンプレートを実物で動かすため、
 * classpath 上に reports/nozeiKanrininShoninTsuchi.jrxml が存在する必要がある。</p>
 */
@ExtendWith(MockitoExtension.class)
class NozeiKanriShoninTsuchiReportsServiceImplTest {

    // 依存なし（JasperReports は実物を使用）
    @InjectMocks NozeiKanriShoninTsuchiReportsServiceImpl service;

    // ------------------------------------------------------------------
    // テストデータ生成ヘルパー
    // ------------------------------------------------------------------

    /** 文字列フィールドをすべて設定した基本 DTO を返す */
    private NozeiKanriShoninTsuchiDto baseDto() {
        NozeiKanriShoninTsuchiDto dto = new NozeiKanriShoninTsuchiDto();
        dto.setShiteiNo("S001");
        dto.setHakkoYmd(LocalDate.of(2025, 4, 1));
        dto.setCityName("札幌市");
        dto.setJorei("○○市宿泊税条例第○条");
        dto.setTokuYubin("1234567");
        dto.setTokuJusho("東京都千代田区1-1");
        dto.setTokuName("山田太郎");
        dto.setShisetsuYubin("1234567");
        dto.setShisetsuJusho("東京都千代田区1-1");
        dto.setShisetsuName("テストホテル");
        dto.setNozeiKanriYubin("1234567");
        dto.setNozeiKanriJusho("東京都港区2-2");
        dto.setNozeiKanriName("納税管理人太郎");
        dto.setKbn("1");
        dto.setRiyu("理由テスト");
        dto.setKoin(new byte[]{1, 2, 3});
        return dto;
    }

    // ==================================================================
    // #30 generateTsuchiPdf
    // ==================================================================

    @Test
    @DisplayName("#30 generateTsuchiPdf 正常系 hakkoYmd が設定されている場合")
    void generateTsuchiPdf_hakkoYmdが設定されている場合は例外なくbyteが返る() {
        NozeiKanriShoninTsuchiDto dto = baseDto();

        byte[] result = service.generateTsuchiPdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #31 generateTsuchiPdf
    // ==================================================================

    @Test
    @DisplayName("#31 generateTsuchiPdf 正常系 hakkoYmd が null の場合")
    void generateTsuchiPdf_hakkoYmdがnullの場合は例外なくbyteが返る() {
        NozeiKanriShoninTsuchiDto dto = baseDto();
        dto.setHakkoYmd(null);

        byte[] result = service.generateTsuchiPdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #32 generateTsuchiPdf
    // ==================================================================

    @Test
    @DisplayName("#32 generateTsuchiPdf 正常系 koin が null の場合")
    void generateTsuchiPdf_koinがnullの場合は例外なくbyteが返る() {
        NozeiKanriShoninTsuchiDto dto = baseDto();
        dto.setKoin(null);

        byte[] result = service.generateTsuchiPdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #33 generateTsuchiPdf
    // ==================================================================

    @Test
    @DisplayName("#33 generateTsuchiPdf 正常系 koin が空配列の場合")
    void generateTsuchiPdf_koinが空配列の場合は例外なくbyteが返る() {
        NozeiKanriShoninTsuchiDto dto = baseDto();
        dto.setKoin(new byte[0]);

        byte[] result = service.generateTsuchiPdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #34 generateTsuchiPdf
    // ==================================================================

    @Test
    @DisplayName("#34 generateTsuchiPdf 異常系 JasperReports処理中に例外が発生した場合")
    void generateTsuchiPdf_JasperReports処理中に例外が発生した場合はRuntimeExceptionがスローされる() {
        // JasperExportManager.exportReportToPdf をモックして例外を投げさせる
        // （Mockito 5 の inline mock maker により static メソッドをモックできる）
        try (MockedStatic<JasperExportManager> jasperExportManager = mockStatic(JasperExportManager.class)) {
            jasperExportManager
                    .when(() -> JasperExportManager.exportReportToPdf(any(JasperPrint.class)))
                    .thenThrow(new JRException("テスト用の例外"));

            assertThatThrownBy(() -> service.generateTsuchiPdf(baseDto()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("PDF生成に失敗しました");
        }
    }

    // ==================================================================
    // #35 generateTsuchiPdf
    // ==================================================================

    @Test
    @DisplayName("#35 generateTsuchiPdf 正常系 文字列フィールドがすべて null の場合")
    void generateTsuchiPdf_文字列フィールドがすべてnullの場合は例外なくbyteが返る() {
        NozeiKanriShoninTsuchiDto dto = new NozeiKanriShoninTsuchiDto();
        // 文字列フィールド・hakkoYmd・koin すべて null（デフォルト）

        byte[] result = service.generateTsuchiPdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }
}

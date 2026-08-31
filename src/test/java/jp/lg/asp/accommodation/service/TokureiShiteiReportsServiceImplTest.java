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

import jp.lg.asp.accommodation.dto.TokureiShiteiDto;
import jp.lg.asp.accommodation.service.impl.TokureiShiteiReportsServiceImpl;
import net.sf.jasperreports.engine.JasperExportManager;

/**
 * 納入申告書の提出期限等の特例適用者指定通知帳票 単体テスト（サービス）
 *
 * <p>チェックリストの #24〜#30 に1対1で対応する。</p>
 * <p>#24〜#29 は JasperReports のテンプレートを実物で動かすため、
 * classpath 上に reports/tokureiShitei.jrxml が存在する必要がある。</p>
 */
@ExtendWith(MockitoExtension.class)
class TokureiShiteiReportsServiceImplTest {

    // 依存なし（JasperReports は実物を使用）
    @InjectMocks TokureiShiteiReportsServiceImpl service;

    // ------------------------------------------------------------------
    // テストデータ生成ヘルパー
    // ------------------------------------------------------------------

    /** 文字列フィールドをすべて設定した基本 DTO を返す */
    private TokureiShiteiDto baseDto() {
        TokureiShiteiDto dto = new TokureiShiteiDto();
        dto.setShiteiNo("0001");
        dto.setHakkoYmd(LocalDate.of(2025, 4, 1));
        dto.setTekiyoYmd(LocalDate.of(2025, 4, 1));
        dto.setShonin("1");
        dto.setRiyu("理由テスト");
        dto.setTokuName("山田太郎");
        dto.setTokuYubin("1234567");
        dto.setTokuJusho("東京都千代田区1-1");
        dto.setShisetsuName("テストホテル");
        dto.setShisetsuYubin("1234567");
        dto.setShisetsuJusho("東京都千代田区1-1");
        dto.setCity("札幌市");
        dto.setJorei("条例テスト");
        dto.setBiko("備考テスト");
        dto.setKoin(new byte[]{1, 2, 3});
        return dto;
    }

    // ==================================================================
    // #24 generateTsuchiPdf
    // ==================================================================

    @Test
    @DisplayName("#24 generateTsuchiPdf 正常系 hakkoYmd と tekiyoYmd が設定されている場合")
    void generateTsuchiPdf_hakkoYmdとtekiyoYmdが設定されている場合は例外なくbyteが返る() {
        TokureiShiteiDto dto = baseDto();

        byte[] result = service.generateTsuchiPdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #25 generateTsuchiPdf
    // ==================================================================

    @Test
    @DisplayName("#25 generateTsuchiPdf 正常系 hakkoYmd が null の場合")
    void generateTsuchiPdf_hakkoYmdがnullの場合は例外なくbyteが返る() {
        TokureiShiteiDto dto = baseDto();
        dto.setHakkoYmd(null);

        byte[] result = service.generateTsuchiPdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #26 generateTsuchiPdf
    // ==================================================================

    @Test
    @DisplayName("#26 generateTsuchiPdf 正常系 tekiyoYmd が null の場合")
    void generateTsuchiPdf_tekiyoYmdがnullの場合は例外なくbyteが返る() {
        TokureiShiteiDto dto = baseDto();
        dto.setTekiyoYmd(null);

        byte[] result = service.generateTsuchiPdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #27 generateTsuchiPdf
    // ==================================================================

    @Test
    @DisplayName("#27 generateTsuchiPdf 正常系 koin が null の場合")
    void generateTsuchiPdf_koinがnullの場合は例外なくbyteが返る() {
        TokureiShiteiDto dto = baseDto();
        dto.setKoin(null);

        byte[] result = service.generateTsuchiPdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #28 generateTsuchiPdf
    // ==================================================================

    @Test
    @DisplayName("#28 generateTsuchiPdf 正常系 koin が空配列の場合")
    void generateTsuchiPdf_koinが空配列の場合は例外なくbyteが返る() {
        TokureiShiteiDto dto = baseDto();
        dto.setKoin(new byte[0]);

        byte[] result = service.generateTsuchiPdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #29 generateTsuchiPdf
    // ==================================================================

    @Test
    @DisplayName("#29 generateTsuchiPdf 正常系 文字列フィールドがすべて null の場合")
    void generateTsuchiPdf_文字列フィールドがすべてnullの場合は例外なくbyteが返る() {
        TokureiShiteiDto dto = new TokureiShiteiDto();
        // 文字列フィールドはすべて null（デフォルト）
        // hakkoYmd・tekiyoYmd・koin も null

        byte[] result = service.generateTsuchiPdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #30 generateTsuchiPdf
    // ==================================================================

    @Test
    @DisplayName("#30 generateTsuchiPdf 異常系 JasperReports処理中に例外が発生した場合")
    void generateTsuchiPdf_JasperReports処理中に例外が発生した場合はRuntimeExceptionがスローされる() {
        TokureiShiteiDto dto = baseDto();

        try (MockedStatic<JasperExportManager> mocked = mockStatic(JasperExportManager.class)) {
            mocked.when(() -> JasperExportManager.exportReportToPdf(any()))
                    .thenThrow(new RuntimeException("JasperReports error"));

            assertThatThrownBy(() -> service.generateTsuchiPdf(dto))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}

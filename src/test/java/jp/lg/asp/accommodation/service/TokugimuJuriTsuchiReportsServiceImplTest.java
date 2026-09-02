package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import jp.lg.asp.accommodation.dto.TokugimuJuriTsuchiDto;
import jp.lg.asp.accommodation.service.impl.TokugimuJuriTsuchiReportsServiceImpl;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;

/**
 * 特別徴収義務者申請受理通知 単体テスト（PDF生成サービス）
 *
 * <p>チェックリストの #23〜#28 に1対1で対応する。
 * JasperReports のテンプレートは実物を使用する。</p>
 */
class TokugimuJuriTsuchiReportsServiceImplTest {

    private final TokugimuJuriTsuchiReportsServiceImpl service = new TokugimuJuriTsuchiReportsServiceImpl();

    /** 文字列フィールドを一通り埋めたDTOを返す */
    private TokugimuJuriTsuchiDto dto() {
        TokugimuJuriTsuchiDto dto = new TokugimuJuriTsuchiDto();
        dto.setShiteiNo("0001");
        dto.setHakkoYmd(LocalDate.of(2025, 4, 1));
        dto.setTokuName("山田太郎");
        dto.setTokuYubin("〒1234567");
        dto.setTokuJusho("東京都千代田区1-1");
        dto.setShisetsuName("テストホテル");
        dto.setShisetsuYubin("〒7654321");
        dto.setShisetsuJusho("北海道札幌市");
        dto.setCityName("○○市長");
        dto.setJorei("○○市宿泊税条例");
        dto.setBiko("備考テスト");
        dto.setKoin(new byte[] { 1, 2, 3 });
        return dto;
    }

    @Test
    @DisplayName("#23 generateTsuchiPdf 正常系 hakkoYmd が設定されている場合")
    void generateTsuchiPdf_hakkoYmdあり() {
        assertThat(service.generateTsuchiPdf(dto())).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("#24 generateTsuchiPdf 正常系 hakkoYmd が null の場合")
    void generateTsuchiPdf_hakkoYmdがnull() {
        TokugimuJuriTsuchiDto dto = dto();
        dto.setHakkoYmd(null);

        assertThat(service.generateTsuchiPdf(dto)).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("#25 generateTsuchiPdf 正常系 koin が null の場合")
    void generateTsuchiPdf_koinがnull() {
        TokugimuJuriTsuchiDto dto = dto();
        dto.setKoin(null);

        assertThat(service.generateTsuchiPdf(dto)).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("#26 generateTsuchiPdf 正常系 koin が空配列の場合")
    void generateTsuchiPdf_koinが空配列() {
        TokugimuJuriTsuchiDto dto = dto();
        dto.setKoin(new byte[0]);

        assertThat(service.generateTsuchiPdf(dto)).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("#27 generateTsuchiPdf 正常系 文字列フィールドがすべて null の場合")
    void generateTsuchiPdf_文字列フィールドがすべてnull() {
        TokugimuJuriTsuchiDto dto = new TokugimuJuriTsuchiDto();
        dto.setHakkoYmd(LocalDate.of(2025, 4, 1));

        assertThat(service.generateTsuchiPdf(dto)).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("#28 generateTsuchiPdf 異常系 JasperReports処理中に例外が発生した場合")
    void generateTsuchiPdf_Jasper例外はRuntimeException() {
        // JasperExportManager.exportReportToPdf をモックして例外を投げさせる
        // （Mockito 5 の inline mock maker により static メソッドをモックできる）
        try (MockedStatic<JasperExportManager> jasperExportManager = mockStatic(JasperExportManager.class)) {
            jasperExportManager
                    .when(() -> JasperExportManager.exportReportToPdf(any(JasperPrint.class)))
                    .thenThrow(new JRException("テスト用の例外"));

            assertThatThrownBy(() -> service.generateTsuchiPdf(dto()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("PDF生成に失敗しました");
        }
    }
}

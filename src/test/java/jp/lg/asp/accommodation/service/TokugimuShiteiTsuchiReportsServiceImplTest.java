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

import jp.lg.asp.accommodation.dto.TokugimuJuriTsuchiDto;
import jp.lg.asp.accommodation.service.impl.TokugimuJuriTsuchiReportsServiceImpl;
import net.sf.jasperreports.engine.JasperExportManager;

/**
 * 特別徴収義務者申請受理通知 単体テスト（ReportsServiceImpl）
 *
 * <p>チェックリスト「TokugimuShiteiTsuchiReportsServiceImpl」の #19〜#35 に1対1で対応する。</p>
 * <p>#19〜#34 は JasperReports のテンプレートを実物で動かすため、
 * classpath 上に reports/tokugimuJuriTsuchi.jrxml が存在する必要がある。</p>
 */
@ExtendWith(MockitoExtension.class)
class TokugimuShiteiTsuchiReportsServiceImplTest {

    @InjectMocks TokugimuJuriTsuchiReportsServiceImpl service;

    private TokugimuJuriTsuchiDto baseDto() {
        TokugimuJuriTsuchiDto dto = new TokugimuJuriTsuchiDto();
        dto.setShiteiNo("0001");
        dto.setHakkoYmd(LocalDate.of(2025, 4, 1));
        dto.setTokuName("山田太郎");
        dto.setTokuYubin("〒1234567");
        dto.setTokuJusho("東京都千代田区1-1");
        dto.setShisetsuName("テストホテル");
        dto.setShisetsuYubin("〒1234567");
        dto.setShisetsuJusho("東京都千代田区1-1");
        dto.setCityName("札幌市");
        dto.setJorei("条例テスト");
        dto.setBiko("備考テスト");
        dto.setKoin(new byte[]{1, 2, 3});
        return dto;
    }

    // ==================================================================
    // #19 generateTsuchiPdf 正常系
    // ==================================================================

    @Test
    @DisplayName("#19 generateTsuchiPdf 正常系 hakkoYmd が設定されている場合")
    void generateTsuchiPdf_hakkoYmdが設定されている場合は例外なくbyteが返る() {
        TokugimuJuriTsuchiDto dto = baseDto();
        dto.setHakkoYmd(LocalDate.of(2025, 4, 1));

        byte[] result = service.generateTsuchiPdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #20 generateTsuchiPdf 正常系
    // ==================================================================

    @Test
    @DisplayName("#20 generateTsuchiPdf 正常系 hakkoYmd が null の場合")
    void generateTsuchiPdf_hakkoYmdがnullの場合は例外なくbyteが返る() {
        TokugimuJuriTsuchiDto dto = baseDto();
        dto.setHakkoYmd(null);

        byte[] result = service.generateTsuchiPdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #21 generateTsuchiPdf 正常系
    // ==================================================================

    @Test
    @DisplayName("#21 generateTsuchiPdf 正常系 koin が null の場合")
    void generateTsuchiPdf_koinがnullの場合は例外なくbyteが返る() {
        TokugimuJuriTsuchiDto dto = baseDto();
        dto.setKoin(null);

        byte[] result = service.generateTsuchiPdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #22 generateTsuchiPdf 正常系
    // ==================================================================

    @Test
    @DisplayName("#22 generateTsuchiPdf 正常系 koin が空配列の場合")
    void generateTsuchiPdf_koinが空配列の場合はnullに変換されて例外なくbyteが返る() {
        TokugimuJuriTsuchiDto dto = baseDto();
        dto.setKoin(new byte[0]);

        byte[] result = service.generateTsuchiPdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #23 generateTsuchiPdf 正常系
    // ==================================================================

    @Test
    @DisplayName("#23 generateTsuchiPdf 正常系 cityName（自治体名）が null の場合")
    void generateTsuchiPdf_cityNameがnullの場合は例外なくbyteが返る() {
        TokugimuJuriTsuchiDto dto = baseDto();
        dto.setCityName(null);

        byte[] result = service.generateTsuchiPdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #24 generateTsuchiPdf 正常系
    // ==================================================================

    @Test
    @DisplayName("#24 generateTsuchiPdf 正常系 jorei（条例）が null の場合")
    void generateTsuchiPdf_joreiがnullの場合は例外なくbyteが返る() {
        TokugimuJuriTsuchiDto dto = baseDto();
        dto.setJorei(null);

        byte[] result = service.generateTsuchiPdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #25 generateTsuchiPdf 正常系
    // ==================================================================

    @Test
    @DisplayName("#25 generateTsuchiPdf 正常系 tokuName（特別徴収義務者名）が null の場合")
    void generateTsuchiPdf_tokuNameがnullの場合は例外なくbyteが返る() {
        TokugimuJuriTsuchiDto dto = baseDto();
        dto.setTokuName(null);

        byte[] result = service.generateTsuchiPdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #26 generateTsuchiPdf 正常系
    // ==================================================================

    @Test
    @DisplayName("#26 generateTsuchiPdf 正常系 shiteiNo（指定番号）が null の場合")
    void generateTsuchiPdf_shiteiNoがnullの場合は例外なくbyteが返る() {
        TokugimuJuriTsuchiDto dto = baseDto();
        dto.setShiteiNo(null);

        byte[] result = service.generateTsuchiPdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #27 generateTsuchiPdf 正常系
    // ==================================================================

    @Test
    @DisplayName("#27 generateTsuchiPdf 正常系 shisetsuYubinNo（施設郵便番号）が null の場合")
    void generateTsuchiPdf_shisetsuYubinNoがnullの場合は例外なくbyteが返る() {
        TokugimuJuriTsuchiDto dto = baseDto();
        dto.setShisetsuYubin(null);

        byte[] result = service.generateTsuchiPdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #28 generateTsuchiPdf 正常系
    // ==================================================================

    @Test
    @DisplayName("#28 generateTsuchiPdf 正常系 shisetsuJusho（施設住所）が null の場合")
    void generateTsuchiPdf_shisetsuJushoがnullの場合は例外なくbyteが返る() {
        TokugimuJuriTsuchiDto dto = baseDto();
        dto.setShisetsuJusho(null);

        byte[] result = service.generateTsuchiPdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #29 generateTsuchiPdf 正常系
    // ==================================================================

    @Test
    @DisplayName("#29 generateTsuchiPdf 正常系 shisetsuName（施設名）が null の場合")
    void generateTsuchiPdf_shisetsuNameがnullの場合は例外なくbyteが返る() {
        TokugimuJuriTsuchiDto dto = baseDto();
        dto.setShisetsuName(null);

        byte[] result = service.generateTsuchiPdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #30 generateTsuchiPdf 正常系
    // ==================================================================

    @Test
    @DisplayName("#30 generateTsuchiPdf 正常系 tokuYubinNo（宛名郵便番号）が null の場合")
    void generateTsuchiPdf_tokuYubinNoがnullの場合は例外なくbyteが返る() {
        TokugimuJuriTsuchiDto dto = baseDto();
        dto.setTokuYubin(null);

        byte[] result = service.generateTsuchiPdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #31 generateTsuchiPdf 正常系
    // ==================================================================

    @Test
    @DisplayName("#31 generateTsuchiPdf 正常系 tokuJusho（宛名住所）が null の場合")
    void generateTsuchiPdf_tokuJushoがnullの場合は例外なくbyteが返る() {
        TokugimuJuriTsuchiDto dto = baseDto();
        dto.setTokuJusho(null);

        byte[] result = service.generateTsuchiPdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #32 generateTsuchiPdf 正常系
    // ==================================================================

    @Test
    @DisplayName("#32 generateTsuchiPdf 正常系 riyu（理由）が null の場合")
    void generateTsuchiPdf_riyuがnullの場合は例外なくbyteが返る() {
        TokugimuJuriTsuchiDto dto = baseDto();
        dto.setBiko(null);

        byte[] result = service.generateTsuchiPdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #33 generateTsuchiPdf 正常系
    // ==================================================================

    @Test
    @DisplayName("#33 generateTsuchiPdf 正常系 city（自治体区分名）が null の場合")
    void generateTsuchiPdf_cityがnullの場合は例外なくbyteが返る() {
        TokugimuJuriTsuchiDto dto = baseDto();
        dto.setCityName(null);

        byte[] result = service.generateTsuchiPdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #34 generateTsuchiPdf 正常系
    // ==================================================================

    @Test
    @DisplayName("#34 generateTsuchiPdf 正常系 文字列項目がすべて null の場合")
    void generateTsuchiPdf_文字列項目がすべてnullの場合は例外なくbyteが返る() {
        TokugimuJuriTsuchiDto dto = new TokugimuJuriTsuchiDto();

        byte[] result = service.generateTsuchiPdf(dto);

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #35 generateTsuchiPdf 異常系
    // ==================================================================

    @Test
    @DisplayName("#35 generateTsuchiPdf 異常系 JasperReports処理中に例外が発生した場合")
    void generateTsuchiPdf_JasperReports処理中に例外が発生した場合はRuntimeExceptionがスローされる() {
        TokugimuJuriTsuchiDto dto = baseDto();

        try (MockedStatic<JasperExportManager> mocked = mockStatic(JasperExportManager.class)) {
            mocked.when(() -> JasperExportManager.exportReportToPdf(any()))
                    .thenThrow(new RuntimeException("JasperReports error"));

            assertThatThrownBy(() -> service.generateTsuchiPdf(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("PDF生成に失敗しました");
        }
    }
}

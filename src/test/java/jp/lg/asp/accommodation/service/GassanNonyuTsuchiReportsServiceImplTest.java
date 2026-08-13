package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import jp.lg.asp.accommodation.dto.GassanNonyuTsuchiDto;
import jp.lg.asp.accommodation.service.impl.GassanNonyuTsuchiReportsServiceImpl;

/**
 * 合算申告納入承認通知書 PDF生成（ACCOMMODATION_TAX-356）の単体テスト。
 *
 * このクラスは注入される依存を持たないため、モックは使わず実際に JasperReports を動かす。
 * 検証しているのは次の2点。
 *   1. 入力値の null 安全化（dto.getXxx() != null ? ... : ""）が効いていること
 *   2. jrxml のフィールド定義と ReportsDto の項目が整合していること
 * jrxml のコンパイルが走るため、他の単体テストより時間がかかる。
 */
class GassanNonyuTsuchiReportsServiceImplTest {

    private final GassanNonyuTsuchiReportsServiceImpl service = new GassanNonyuTsuchiReportsServiceImpl();

    /** 公印の代わりに使う 1x1 の PNG */
    private static final byte[] KOIN_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    /** 全項目を埋めた入力 */
    private GassanNonyuTsuchiDto fullDto() {
        GassanNonyuTsuchiDto dto = new GassanNonyuTsuchiDto();
        dto.setShiteiNo("00100001");
        dto.setGassanShiteiNo("G0010001");
        dto.setTokuName("株式会社ホテルA");
        dto.setTokuJusho("札幌市中央区北1条西1丁目");
        dto.setTekiyoStYmd(LocalDate.of(2026, 4, 1));
        dto.setHakkoYmd(LocalDate.of(2026, 4, 1));
        dto.setBiko("備考テスト");
        dto.setCity("札幌市");
        dto.setJorei("札幌市宿泊税条例第5条");
        dto.setKoin(KOIN_PNG);
        dto.setNonyuKigen("2026年5月31日");
        return dto;
    }

    /** PDF として成立しているか */
    private void assertPdf(byte[] pdf) {
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void generateTsuchiPdf_全項目を設定するとPDFが生成される() {
        assertPdf(service.generateTsuchiPdf(fullDto()));
    }

    @Test
    void generateTsuchiPdf_全項目nullでも例外にならずPDFが生成される() {
        assertPdf(service.generateTsuchiPdf(new GassanNonyuTsuchiDto()));
    }

    @Test
    void generateTsuchiPdf_公印が空配列でも例外にならずPDFが生成される() {
        GassanNonyuTsuchiDto dto = fullDto();
        dto.setKoin(new byte[0]);

        assertPdf(service.generateTsuchiPdf(dto));
    }
}

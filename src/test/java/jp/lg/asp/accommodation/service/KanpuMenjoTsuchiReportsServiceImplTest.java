package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import jp.lg.asp.accommodation.dto.KanpuMenjoTsuchiDto;
import jp.lg.asp.accommodation.service.impl.KanpuMenjoTsuchiReportsServiceImpl;

/**
 * 徴収不能額の還付又は納入義務の免除決定通知書 PDF生成（ACCOMMODATION_TAX-362）の単体テスト。
 *
 * このクラスは注入される依存を持たないため、モックは使わず実際に JasperReports を動かす。
 * 検証しているのは次の2点。
 *   1. 入力値の null 安全化（dto.getXxx() != null ? ... : ""）が効いていること
 *   2. jrxml のフィールド定義と ReportsDto の項目が整合していること
 * jrxml のコンパイルが走るため、他の単体テストより時間がかかる。
 */
class KanpuMenjoTsuchiReportsServiceImplTest {

    private final KanpuMenjoTsuchiReportsServiceImpl service = new KanpuMenjoTsuchiReportsServiceImpl();

    /** 公印の代わりに使う 1x1 の PNG */
    private static final byte[] KOIN_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    /** 全項目を埋めた入力 */
    private KanpuMenjoTsuchiDto fullDto() {
        KanpuMenjoTsuchiDto dto = new KanpuMenjoTsuchiDto();
        dto.setShiteiNo("00100001");
        dto.setCityName("札幌市");
        dto.setJorei("札幌市宿泊税条例第5条");
        dto.setHakkoYmd(LocalDate.of(2026, 4, 1));
        dto.setTokuJusho("札幌市中央区北1条西1丁目");
        dto.setTokuName("株式会社ホテルA");
        dto.setShinsei_kbn("1");
        dto.setKettei_naiyou("1");
        dto.setShisetsuJusho("札幌市中央区北2条西2丁目");
        dto.setShisetsuName("ホテルA 札幌");
        dto.setJuriYmd(LocalDate.of(2026, 3, 25));
        dto.setShinseiYm("2026-05");
        dto.setZeigaku("10,000");
        dto.setKanpuMenjoGaku("1,000");
        dto.setRiyu("テスト用の理由");
        dto.setBiko("備考テスト");
        dto.setKoin(KOIN_PNG);
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
        assertPdf(service.generateTsuchiPdf(new KanpuMenjoTsuchiDto()));
    }

    @Test
    void generateTsuchiPdf_公印が空配列でも例外にならずPDFが生成される() {
        KanpuMenjoTsuchiDto dto = fullDto();
        dto.setKoin(new byte[0]);

        assertPdf(service.generateTsuchiPdf(dto));
    }

    @Test
    void generateTsuchiPdf_申請年月がハイフン区切りでも変換されPDFが生成される() {
        KanpuMenjoTsuchiDto dto = fullDto();
        dto.setShinseiYm("2026-05");

        assertPdf(service.generateTsuchiPdf(dto));
    }

    @Test
    void generateTsuchiPdf_申請年月が6桁数字でも変換されPDFが生成される() {
        KanpuMenjoTsuchiDto dto = fullDto();
        dto.setShinseiYm("202605");

        assertPdf(service.generateTsuchiPdf(dto));
    }

    @Test
    void generateTsuchiPdf_申請年月が想定外の形式でも例外にならない() {
        KanpuMenjoTsuchiDto dto = fullDto();
        dto.setShinseiYm("令和8年5月");

        assertPdf(service.generateTsuchiPdf(dto));
    }
}

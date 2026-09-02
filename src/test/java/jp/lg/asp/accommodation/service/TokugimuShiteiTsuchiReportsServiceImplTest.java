package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Collection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import jp.lg.asp.accommodation.dto.TokugimuShiteiTsuchiDto;
import jp.lg.asp.accommodation.dto.TokugimuShiteiTsuchiReportsDto;
import jp.lg.asp.accommodation.service.impl.TokugimuShiteiTsuchiReportsServiceImpl;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * 特別徴収義務者指定通知 単体テスト（PDF生成サービス）
 *
 * <p>チェックリストの #19〜#35 に1対1で対応する。
 * JasperReports のテンプレート（reports/tokugimuShiteiTsuchi.jrxml）は実物を使用する。</p>
 *
 * <p>帳票DTOへの変換結果は private メソッド buildParams の戻り値でしか確認できないため、
 * リフレクションで呼び出して JRBeanCollectionDataSource から取り出している。
 * 実装側には手を入れていない。</p>
 */
class TokugimuShiteiTsuchiReportsServiceImplTest {

    private final TokugimuShiteiTsuchiReportsServiceImpl service = new TokugimuShiteiTsuchiReportsServiceImpl();

    // ------------------------------------------------------------------
    // テストデータ生成・ヘルパー
    // ------------------------------------------------------------------

    /** 全項目を埋めたDTOを返す */
    private TokugimuShiteiTsuchiDto dto() {
        TokugimuShiteiTsuchiDto dto = new TokugimuShiteiTsuchiDto();
        dto.setShiteiNo("0001");
        dto.setHakkoYmd(LocalDate.of(2025, 4, 1));
        dto.setRiyu("理由テスト");
        dto.setTokuName("山田太郎");
        dto.setTokuYubinNo("1234567");
        dto.setTokuJusho("東京都千代田区1-1");
        dto.setShisetsuName("テストホテル");
        dto.setShisetsuYubinNo("0600001");
        dto.setShisetsuJusho("北海道札幌市");
        dto.setCityName("○○市");
        dto.setJorei("○○市宿泊税条例");
        dto.setCity("市");
        dto.setKoin(new byte[] { 1, 2, 3 });
        return dto;
    }

    /** private な buildParams を呼び出して帳票DTOを取り出す */
    private TokugimuShiteiTsuchiReportsDto buildParams(TokugimuShiteiTsuchiDto dto) throws Exception {
        Method method = TokugimuShiteiTsuchiReportsServiceImpl.class
                .getDeclaredMethod("buildParams", TokugimuShiteiTsuchiDto.class);
        method.setAccessible(true);
        JRDataSource dataSource = (JRDataSource) method.invoke(service, dto);
        Collection<?> data = ((JRBeanCollectionDataSource) dataSource).getData();
        return (TokugimuShiteiTsuchiReportsDto) data.iterator().next();
    }

    // ==================================================================
    // generateTsuchiPdf
    // ==================================================================

    @Test
    @DisplayName("#19 generateTsuchiPdf 正常系 hakkoYmd が設定されている場合")
    void generateTsuchiPdf_hakkoYmdは和暦に変換される() throws Exception {
        TokugimuShiteiTsuchiDto dto = dto();

        assertThat(service.generateTsuchiPdf(dto)).isNotNull().isNotEmpty();
        assertThat(buildParams(dto).getHakkoYmd()).isEqualTo("令和7年4月1日");
    }

    @Test
    @DisplayName("#20 generateTsuchiPdf 正常系 hakkoYmd が null の場合")
    void generateTsuchiPdf_hakkoYmdがnullは空文字() throws Exception {
        TokugimuShiteiTsuchiDto dto = dto();
        dto.setHakkoYmd(null);

        assertThat(service.generateTsuchiPdf(dto)).isNotNull().isNotEmpty();
        assertThat(buildParams(dto).getHakkoYmd()).isEmpty();
    }

    @Test
    @DisplayName("#21 generateTsuchiPdf 正常系 koin が null の場合")
    void generateTsuchiPdf_koinがnullはnullのまま() throws Exception {
        TokugimuShiteiTsuchiDto dto = dto();
        dto.setKoin(null);

        assertThat(service.generateTsuchiPdf(dto)).isNotNull().isNotEmpty();
        assertThat(buildParams(dto).getKoin()).isNull();
    }

    @Test
    @DisplayName("#22 generateTsuchiPdf 正常系 koin が空配列の場合")
    void generateTsuchiPdf_koinが空配列はnullに変換される() throws Exception {
        TokugimuShiteiTsuchiDto dto = dto();
        dto.setKoin(new byte[0]);

        assertThat(service.generateTsuchiPdf(dto)).isNotNull().isNotEmpty();
        assertThat(buildParams(dto).getKoin())
                .as("length=0 は null 扱い")
                .isNull();
    }

    @Test
    @DisplayName("#23 generateTsuchiPdf 正常系 cityName（自治体名）が null の場合")
    void generateTsuchiPdf_cityNameがnullは空文字() throws Exception {
        TokugimuShiteiTsuchiDto dto = dto();
        dto.setCityName(null);

        assertThat(service.generateTsuchiPdf(dto)).isNotNull().isNotEmpty();
        assertThat(buildParams(dto).getCityName()).isEmpty();
    }

    @Test
    @DisplayName("#24 generateTsuchiPdf 正常系 jorei（条例）が null の場合")
    void generateTsuchiPdf_joreiがnullは空文字() throws Exception {
        TokugimuShiteiTsuchiDto dto = dto();
        dto.setJorei(null);

        assertThat(service.generateTsuchiPdf(dto)).isNotNull().isNotEmpty();
        assertThat(buildParams(dto).getJorei()).isEmpty();
    }

    @Test
    @DisplayName("#25 generateTsuchiPdf 正常系 tokuName（特別徴収義務者名）が null の場合")
    void generateTsuchiPdf_tokuNameがnullは空文字() throws Exception {
        TokugimuShiteiTsuchiDto dto = dto();
        dto.setTokuName(null);

        assertThat(service.generateTsuchiPdf(dto)).isNotNull().isNotEmpty();
        assertThat(buildParams(dto).getTokuName()).isEmpty();
    }

    @Test
    @DisplayName("#26 generateTsuchiPdf 正常系 shiteiNo（指定番号）が null の場合")
    void generateTsuchiPdf_shiteiNoがnullは空文字() throws Exception {
        TokugimuShiteiTsuchiDto dto = dto();
        dto.setShiteiNo(null);

        assertThat(service.generateTsuchiPdf(dto)).isNotNull().isNotEmpty();
        assertThat(buildParams(dto).getShiteiNo()).isEmpty();
    }

    @Test
    @DisplayName("#27 generateTsuchiPdf 正常系 shisetsuYubinNo（施設郵便番号）が null の場合")
    void generateTsuchiPdf_shisetsuYubinNoがnullは空文字() throws Exception {
        TokugimuShiteiTsuchiDto dto = dto();
        dto.setShisetsuYubinNo(null);

        assertThat(service.generateTsuchiPdf(dto)).isNotNull().isNotEmpty();
        assertThat(buildParams(dto).getShisetsuYubinNo()).isEmpty();
    }

    @Test
    @DisplayName("#28 generateTsuchiPdf 正常系 shisetsuJusho（施設住所）が null の場合")
    void generateTsuchiPdf_shisetsuJushoがnullは空文字() throws Exception {
        TokugimuShiteiTsuchiDto dto = dto();
        dto.setShisetsuJusho(null);

        assertThat(service.generateTsuchiPdf(dto)).isNotNull().isNotEmpty();
        assertThat(buildParams(dto).getShisetsuJusho()).isEmpty();
    }

    @Test
    @DisplayName("#29 generateTsuchiPdf 正常系 shisetsuName（施設名）が null の場合")
    void generateTsuchiPdf_shisetsuNameがnullは空文字() throws Exception {
        TokugimuShiteiTsuchiDto dto = dto();
        dto.setShisetsuName(null);

        assertThat(service.generateTsuchiPdf(dto)).isNotNull().isNotEmpty();
        assertThat(buildParams(dto).getShisetsuName()).isEmpty();
    }

    @Test
    @DisplayName("#30 generateTsuchiPdf 正常系 tokuYubinNo（宛名郵便番号）が null の場合")
    void generateTsuchiPdf_tokuYubinNoがnullは空文字() throws Exception {
        TokugimuShiteiTsuchiDto dto = dto();
        dto.setTokuYubinNo(null);

        assertThat(service.generateTsuchiPdf(dto)).isNotNull().isNotEmpty();
        assertThat(buildParams(dto).getTokuYubinNo()).isEmpty();
    }

    @Test
    @DisplayName("#31 generateTsuchiPdf 正常系 tokuJusho（宛名住所）が null の場合")
    void generateTsuchiPdf_tokuJushoがnullは空文字() throws Exception {
        TokugimuShiteiTsuchiDto dto = dto();
        dto.setTokuJusho(null);

        assertThat(service.generateTsuchiPdf(dto)).isNotNull().isNotEmpty();
        assertThat(buildParams(dto).getTokuJusho()).isEmpty();
    }

    @Test
    @DisplayName("#32 generateTsuchiPdf 正常系 riyu（理由）が null の場合")
    void generateTsuchiPdf_riyuがnullは空文字() throws Exception {
        TokugimuShiteiTsuchiDto dto = dto();
        dto.setRiyu(null);

        assertThat(service.generateTsuchiPdf(dto)).isNotNull().isNotEmpty();
        assertThat(buildParams(dto).getRiyu()).isEmpty();
    }

    @Test
    @DisplayName("#33 generateTsuchiPdf 正常系 city（自治体区分名）が null の場合")
    void generateTsuchiPdf_cityがnullは空文字() throws Exception {
        TokugimuShiteiTsuchiDto dto = dto();
        dto.setCity(null);

        assertThat(service.generateTsuchiPdf(dto)).isNotNull().isNotEmpty();
        assertThat(buildParams(dto).getCity()).isEmpty();
    }

    @Test
    @DisplayName("#34 generateTsuchiPdf 正常系 文字列項目がすべて null の場合")
    void generateTsuchiPdf_全項目nullはすべて空文字() throws Exception {
        TokugimuShiteiTsuchiDto dto = new TokugimuShiteiTsuchiDto();

        assertThat(service.generateTsuchiPdf(dto)).isNotNull().isNotEmpty();

        TokugimuShiteiTsuchiReportsDto reportsDto = buildParams(dto);
        assertThat(reportsDto.getShiteiNo()).isEmpty();
        assertThat(reportsDto.getHakkoYmd()).isEmpty();
        assertThat(reportsDto.getRiyu()).isEmpty();
        assertThat(reportsDto.getTokuName()).isEmpty();
        assertThat(reportsDto.getTokuYubinNo()).isEmpty();
        assertThat(reportsDto.getTokuJusho()).isEmpty();
        assertThat(reportsDto.getShisetsuName()).isEmpty();
        assertThat(reportsDto.getShisetsuYubinNo()).isEmpty();
        assertThat(reportsDto.getShisetsuJusho()).isEmpty();
        assertThat(reportsDto.getCityName()).isEmpty();
        assertThat(reportsDto.getJorei()).isEmpty();
        assertThat(reportsDto.getCity()).isEmpty();
        assertThat(reportsDto.getKoin()).as("koin のみ null").isNull();
    }

    @Test
    @DisplayName("#35 generateTsuchiPdf 異常系 JasperReports処理中に例外が発生した場合（不正なjrxml等）")
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

package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.KanpuMenjoTsuchiDto;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.service.impl.KanpuMenjoTsuchiReportsServiceImpl;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;

/**
 * 徴収不能額の還付又は納入義務の免除決定通知書PDF生成 単体テスト (No.19-47)
 */
@ExtendWith(MockitoExtension.class)
class KanpuMenjoTsuchiReportsServiceImplTest {

    @Mock ReportsCommonService reportsCommonService;
    @Mock JichitaiRepository jichitaiRepository;

    @InjectMocks KanpuMenjoTsuchiReportsServiceImpl service;

    private KanpuMenjoTsuchiDto fullDto() {
        KanpuMenjoTsuchiDto dto = new KanpuMenjoTsuchiDto();
        dto.setShiteiNo("S001");
        dto.setShinsei_kbn("還付");
        dto.setKettei_naiyou("承認");
        dto.setHakkoYmd(LocalDate.of(2025, 6, 15));
        dto.setJuriYmd(LocalDate.of(2025, 3, 31));
        dto.setShinseiYm("2025-06");
        dto.setZeigaku("1000000");
        dto.setKanpuMenjoGaku("50000");
        dto.setKoin(new byte[]{1, 2, 3});
        dto.setCityName("大阪市");
        dto.setTokuName("テスト");
        dto.setTokuYubin("〒1234567");
        dto.setTokuJusho("住所");
        dto.setShisetsuName("施設");
        dto.setShisetsuYubin("〒7654321");
        dto.setShisetsuJusho("施設所在地");
        return dto;
    }

    private Jichitai jichitai(String name) {
        Jichitai j = new Jichitai();
        j.setName(name);
        return j;
    }

    // =======================================================================
    // No.19 findJichitai - 正常系: 自治体コード存在 → Jichitai返す
    // =======================================================================

    @Test
    void findJichitai_正常系_自治体コード存在_Jichitai返す() {
        when(jichitaiRepository.findById("011001")).thenReturn(Optional.of(jichitai("大阪市")));

        Jichitai result = service.findJichitai("011001");

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("大阪市");
    }

    // =======================================================================
    // No.20 findJichitai - 異常系: 自治体コード存在しない → IllegalArgumentExceptionスロー
    // =======================================================================

    @Test
    void findJichitai_異常系_自治体コード存在しない_IllegalArgumentExceptionスロー() {
        when(jichitaiRepository.findById("999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findJichitai("999999"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("自治体情報が見つかりませんでした。");
    }

    // =======================================================================
    // No.21 findJichitai - 境界値: jichitaiCdがnull → IllegalArgumentExceptionスロー
    // =======================================================================

    @Test
    void findJichitai_境界値_jichitaiCdがnull_IllegalArgumentExceptionスロー() {
        when(jichitaiRepository.findById(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findJichitai(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("自治体情報が見つかりませんでした。");
    }

    // =======================================================================
    // No.22 generateTsuchiPdf - 正常系: 全フィールド設定済み → PDF byte[]返す
    // =======================================================================

    @Test
    void generateTsuchiPdf_正常系_全フィールド設定済み_PDFバイト列返す() {
        when(reportsCommonService.getReportsDefText(ReportsConstants.KANPU_MENJO_SHINSEI_JOREI))
                .thenReturn("条例文");
        try (MockedStatic<net.sf.jasperreports.engine.JasperCompileManager> compileMock =
                     mockStatic(net.sf.jasperreports.engine.JasperCompileManager.class);
             MockedStatic<net.sf.jasperreports.engine.JasperFillManager> fillMock =
                     mockStatic(net.sf.jasperreports.engine.JasperFillManager.class);
             MockedStatic<JasperExportManager> exportMock =
                     mockStatic(JasperExportManager.class)) {

            net.sf.jasperreports.engine.JasperReport jasperReport =
                    mock(net.sf.jasperreports.engine.JasperReport.class);
            JasperPrint jasperPrint = mock(JasperPrint.class);

            compileMock.when(() -> net.sf.jasperreports.engine.JasperCompileManager
                    .compileReport(any(java.io.InputStream.class))).thenReturn(jasperReport);
            fillMock.when(() -> net.sf.jasperreports.engine.JasperFillManager
                    .fillReport(any(net.sf.jasperreports.engine.JasperReport.class),
                            any(java.util.Map.class),
                            any(net.sf.jasperreports.engine.JRDataSource.class)))
                    .thenReturn(jasperPrint);
            exportMock.when(() -> JasperExportManager.exportReportToPdf(any(JasperPrint.class)))
                    .thenReturn(new byte[]{1, 2, 3});

            byte[] result = service.generateTsuchiPdf(fullDto());

            assertThat(result).isEqualTo(new byte[]{1, 2, 3});
        }
    }

    // =======================================================================
    // No.23 generateTsuchiPdf - 異常系: JasperExportManagerがスロー → RuntimeExceptionでラップ
    // =======================================================================

    @Test
    void generateTsuchiPdf_異常系_JasperExportManagerがスロー_RuntimeExceptionでラップ() {
        when(reportsCommonService.getReportsDefText(any())).thenReturn("条例文");
        try (MockedStatic<net.sf.jasperreports.engine.JasperCompileManager> compileMock =
                     mockStatic(net.sf.jasperreports.engine.JasperCompileManager.class);
             MockedStatic<net.sf.jasperreports.engine.JasperFillManager> fillMock =
                     mockStatic(net.sf.jasperreports.engine.JasperFillManager.class);
             MockedStatic<JasperExportManager> exportMock =
                     mockStatic(JasperExportManager.class)) {

            net.sf.jasperreports.engine.JasperReport jasperReport =
                    mock(net.sf.jasperreports.engine.JasperReport.class);
            JasperPrint jasperPrint = mock(JasperPrint.class);

            compileMock.when(() -> net.sf.jasperreports.engine.JasperCompileManager
                    .compileReport(any(java.io.InputStream.class))).thenReturn(jasperReport);
            fillMock.when(() -> net.sf.jasperreports.engine.JasperFillManager
                    .fillReport(any(net.sf.jasperreports.engine.JasperReport.class),
                            any(java.util.Map.class),
                            any(net.sf.jasperreports.engine.JRDataSource.class)))
                    .thenReturn(jasperPrint);
            exportMock.when(() -> JasperExportManager.exportReportToPdf(any(JasperPrint.class)))
                    .thenThrow(new JRException("テスト用エラー"));

            assertThatThrownBy(() -> service.generateTsuchiPdf(fullDto()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("PDF生成に失敗しました");
        }
    }

    // JasperReportsをフルモック化してfillReportの引数をキャプチャするヘルパー
    @SuppressWarnings("unchecked")
    private jp.lg.asp.accommodation.dto.KanpuMenjoTsuchiReportsDto captureReportsDto(
            KanpuMenjoTsuchiDto dto) throws Exception {
        when(reportsCommonService.getReportsDefText(any())).thenReturn("条例文");
        try (MockedStatic<net.sf.jasperreports.engine.JasperCompileManager> compileMock =
                     mockStatic(net.sf.jasperreports.engine.JasperCompileManager.class);
             MockedStatic<net.sf.jasperreports.engine.JasperFillManager> fillMock =
                     mockStatic(net.sf.jasperreports.engine.JasperFillManager.class);
             MockedStatic<JasperExportManager> exportMock =
                     mockStatic(JasperExportManager.class)) {

            compileMock.when(() -> net.sf.jasperreports.engine.JasperCompileManager
                    .compileReport(any(java.io.InputStream.class)))
                    .thenReturn(mock(net.sf.jasperreports.engine.JasperReport.class));
            exportMock.when(() -> JasperExportManager
                    .exportReportToPdf(any(JasperPrint.class)))
                    .thenReturn(new byte[]{1});

            org.mockito.ArgumentCaptor<net.sf.jasperreports.engine.JRDataSource> captor =
                    org.mockito.ArgumentCaptor.forClass(net.sf.jasperreports.engine.JRDataSource.class);
            fillMock.when(() -> net.sf.jasperreports.engine.JasperFillManager
                    .fillReport(
                            any(net.sf.jasperreports.engine.JasperReport.class),
                            any(java.util.Map.class),
                            captor.capture()))
                    .thenReturn(mock(JasperPrint.class));

            service.generateTsuchiPdf(dto);

            net.sf.jasperreports.engine.data.JRBeanCollectionDataSource ds =
                    (net.sf.jasperreports.engine.data.JRBeanCollectionDataSource) captor.getValue();
            java.lang.reflect.Field f = ds.getClass().getDeclaredField("data");
            f.setAccessible(true);
            java.util.Collection<jp.lg.asp.accommodation.dto.KanpuMenjoTsuchiReportsDto> col =
                    (java.util.Collection<jp.lg.asp.accommodation.dto.KanpuMenjoTsuchiReportsDto>) f.get(ds);
            return col.iterator().next();
        }
    }

    // =======================================================================
    // No.24 buildParams - 正常系: hakkoYmdが令和の日付 → 和暦に変換
    // =======================================================================

    @Test
    void buildParams_正常系_hakkoYmdが令和の日付_和暦に変換() throws Exception {
        KanpuMenjoTsuchiDto dto = fullDto();
        dto.setHakkoYmd(LocalDate.of(2025, 6, 15));

        jp.lg.asp.accommodation.dto.KanpuMenjoTsuchiReportsDto reportsDto = captureReportsDto(dto);

        assertThat(reportsDto.getHakkoYmd()).isEqualTo("令和7年6月15日");
    }

    // =======================================================================
    // No.25 buildParams - 正常系: juriYmdが令和の日付 → 和暦に変換
    // =======================================================================

    @Test
    void buildParams_正常系_juriYmdが令和の日付_和暦に変換() throws Exception {
        KanpuMenjoTsuchiDto dto = fullDto();
        dto.setJuriYmd(LocalDate.of(2025, 3, 31));

        jp.lg.asp.accommodation.dto.KanpuMenjoTsuchiReportsDto reportsDto = captureReportsDto(dto);

        assertThat(reportsDto.getJuriYmd()).isEqualTo("令和7年3月31日");
    }

    // =======================================================================
    // No.26 buildParams - 正常系: shinseiYmが"2025-06" → 和暦に変換
    // =======================================================================

    @Test
    void buildParams_正常系_shinseiYmが2025_06_和暦に変換() throws Exception {
        KanpuMenjoTsuchiDto dto = fullDto();
        dto.setShinseiYm("2025-06");

        jp.lg.asp.accommodation.dto.KanpuMenjoTsuchiReportsDto reportsDto = captureReportsDto(dto);

        assertThat(reportsDto.getShinseiYm()).isEqualTo("令和7年6月");
    }

    // =======================================================================
    // No.27 buildParams - 正常系: zeigakuが大きい値 → カンマ区切りに変換
    // =======================================================================

    @Test
    void buildParams_正常系_zeigakuが大きい値_カンマ区切りに変換() throws Exception {
        KanpuMenjoTsuchiDto dto = fullDto();
        dto.setZeigaku("1000000");

        jp.lg.asp.accommodation.dto.KanpuMenjoTsuchiReportsDto reportsDto = captureReportsDto(dto);

        assertThat(reportsDto.getZeigaku()).isEqualTo("1,000,000");
    }

    // =======================================================================
    // No.28 buildParams - 正常系: kanpuMenjoGakuが値あり → カンマ区切りに変換
    // =======================================================================

    @Test
    void buildParams_正常系_kanpuMenjoGakuが値あり_カンマ区切りに変換() throws Exception {
        KanpuMenjoTsuchiDto dto = fullDto();
        dto.setKanpuMenjoGaku("50000");

        jp.lg.asp.accommodation.dto.KanpuMenjoTsuchiReportsDto reportsDto = captureReportsDto(dto);

        assertThat(reportsDto.getKanpuMenjoGaku()).isEqualTo("50,000");
    }

    // =======================================================================
    // No.29 buildParams - 正常系: koinにバイト列あり → そのまま設定
    // =======================================================================

    @Test
    void buildParams_正常系_koinにバイト列あり_そのまま設定() throws Exception {
        KanpuMenjoTsuchiDto dto = fullDto();
        dto.setKoin(new byte[]{1, 2, 3});

        jp.lg.asp.accommodation.dto.KanpuMenjoTsuchiReportsDto reportsDto = captureReportsDto(dto);

        assertThat(reportsDto.getKoin()).isEqualTo(new byte[]{1, 2, 3});
    }

    // =======================================================================
    // No.30 buildParams - 正常系: joreiがnullでない → そのまま設定
    // =======================================================================

    @Test
    void buildParams_正常系_joreiがnullでない_そのまま設定() throws Exception {
        jp.lg.asp.accommodation.dto.KanpuMenjoTsuchiReportsDto reportsDto = captureReportsDto(fullDto());

        assertThat(reportsDto.getJorei()).isEqualTo("条例文");
    }

    // =======================================================================
    // No.31 buildParams - 異常系: hakkoYmdがnull → IllegalArgumentExceptionスロー
    // =======================================================================

    @Test
    void buildParams_異常系_hakkoYmdがnull_IllegalArgumentExceptionスロー() {
        when(reportsCommonService.getReportsDefText(any())).thenReturn("条例文");
        KanpuMenjoTsuchiDto dto = fullDto();
        dto.setHakkoYmd(null);

        assertThatThrownBy(() -> service.generateTsuchiPdf(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("発行年月日は必須です。");
    }

    // =======================================================================
    // No.32 buildParams - 異常系: juriYmdがnull → IllegalArgumentExceptionスロー
    // =======================================================================

    @Test
    void buildParams_異常系_juriYmdがnull_IllegalArgumentExceptionスロー() {
        when(reportsCommonService.getReportsDefText(any())).thenReturn("条例文");
        KanpuMenjoTsuchiDto dto = fullDto();
        dto.setJuriYmd(null);

        assertThatThrownBy(() -> service.generateTsuchiPdf(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("申請受理年月日は必須です。");
    }

    // =======================================================================
    // No.33 buildParams - 異常系: shinseiYmがnull → IllegalArgumentExceptionスロー
    // =======================================================================

    @Test
    void buildParams_異常系_shinseiYmがnull_IllegalArgumentExceptionスロー() {
        when(reportsCommonService.getReportsDefText(any())).thenReturn("条例文");
        KanpuMenjoTsuchiDto dto = fullDto();
        dto.setShinseiYm(null);

        assertThatThrownBy(() -> service.generateTsuchiPdf(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("対象年月は必須です。");
    }

    // =======================================================================
    // No.34 buildParams - 異常系: shinseiYmが空文字 → IllegalArgumentExceptionスロー
    // =======================================================================

    @Test
    void buildParams_異常系_shinseiYmが空文字_IllegalArgumentExceptionスロー() {
        when(reportsCommonService.getReportsDefText(any())).thenReturn("条例文");
        KanpuMenjoTsuchiDto dto = fullDto();
        dto.setShinseiYm("");

        assertThatThrownBy(() -> service.generateTsuchiPdf(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("対象年月は必須です。");
    }

    // =======================================================================
    // No.35 buildParams - 異常系: shinseiYmが不正フォーマット("202506") → RuntimeExceptionでラップ
    // =======================================================================

    @Test
    void buildParams_異常系_shinseiYmが不正フォーマット_RuntimeExceptionでラップ() {
        when(reportsCommonService.getReportsDefText(any())).thenReturn("条例文");
        KanpuMenjoTsuchiDto dto = fullDto();
        dto.setShinseiYm("202506");

        assertThatThrownBy(() -> service.generateTsuchiPdf(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("PDF生成に失敗しました");
    }

    // =======================================================================
    // No.36 buildParams - 異常系: zeigakuが数値変換失敗 → IllegalArgumentExceptionスロー
    // =======================================================================

    @Test
    void buildParams_異常系_zeigakuが数値変換失敗_IllegalArgumentExceptionスロー() {
        when(reportsCommonService.getReportsDefText(any())).thenReturn("条例文");
        KanpuMenjoTsuchiDto dto = fullDto();
        dto.setZeigaku("abc");

        assertThatThrownBy(() -> service.generateTsuchiPdf(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("申請した税額が不正です。");
    }

    // =======================================================================
    // No.37 buildParams - 異常系: joreiがnull → IllegalArgumentExceptionスロー
    // =======================================================================

    @Test
    void buildParams_異常系_joreiがnull_IllegalArgumentExceptionスロー() {
        when(reportsCommonService.getReportsDefText(any())).thenReturn(null);

        assertThatThrownBy(() -> service.generateTsuchiPdf(fullDto()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("帳票出力項目が未設定です。管理者にお問い合わせください。");
    }

    // =======================================================================
    // No.38 buildParams - 異常系: 全フィールドnull(getReportsDefTextがnull) → IllegalArgumentExceptionスロー
    // =======================================================================

    @Test
    void buildParams_異常系_全フィールドnull_IllegalArgumentExceptionスロー() {
        when(reportsCommonService.getReportsDefText(any())).thenReturn(null);
        KanpuMenjoTsuchiDto dto = new KanpuMenjoTsuchiDto();
        dto.setShinsei_kbn("還付");
        dto.setKettei_naiyou("承認");

        assertThatThrownBy(() -> service.generateTsuchiPdf(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("帳票出力項目が未設定です。管理者にお問い合わせください。");
    }

    // =======================================================================
    // No.39 buildParams - 境界値: koinが空バイト配列 → IllegalArgumentExceptionスロー
    // =======================================================================

    @Test
    void buildParams_境界値_koinが空バイト配列_IllegalArgumentExceptionスロー() {
        when(reportsCommonService.getReportsDefText(any())).thenReturn("条例文");
        KanpuMenjoTsuchiDto dto = fullDto();
        dto.setKoin(new byte[]{});

        assertThatThrownBy(() -> service.generateTsuchiPdf(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("公印が未設定です。管理者にお問い合わせください。");
    }

    // =======================================================================
    // No.40 buildParams - 境界値: koinがnull → IllegalArgumentExceptionスロー
    // =======================================================================

    @Test
    void buildParams_境界値_koinがnull_IllegalArgumentExceptionスロー() {
        when(reportsCommonService.getReportsDefText(any())).thenReturn("条例文");
        KanpuMenjoTsuchiDto dto = fullDto();
        dto.setKoin(null);

        assertThatThrownBy(() -> service.generateTsuchiPdf(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("公印が未設定です。管理者にお問い合わせください。");
    }

    // =======================================================================
    // No.41 buildParams - 境界値: zeigakuがnull → IllegalArgumentExceptionスロー
    // =======================================================================

    @Test
    void buildParams_境界値_zeigakuがnull_IllegalArgumentExceptionスロー() {
        when(reportsCommonService.getReportsDefText(any())).thenReturn("条例文");
        KanpuMenjoTsuchiDto dto = fullDto();
        dto.setZeigaku(null);

        assertThatThrownBy(() -> service.generateTsuchiPdf(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("申請した税額は必須です。");
    }

    // =======================================================================
    // No.42 buildParams - 境界値: zeigakuが空文字 → IllegalArgumentExceptionスロー
    // =======================================================================

    @Test
    void buildParams_境界値_zeigakuが空文字_IllegalArgumentExceptionスロー() {
        when(reportsCommonService.getReportsDefText(any())).thenReturn("条例文");
        KanpuMenjoTsuchiDto dto = fullDto();
        dto.setZeigaku("");

        assertThatThrownBy(() -> service.generateTsuchiPdf(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("申請した税額は必須です。");
    }

    // =======================================================================
    // No.43 buildParams - 境界値: zeigakuが"0" → "0"のまま
    // =======================================================================

    @Test
    void buildParams_境界値_zeigakuが0_0のまま() throws Exception {
        KanpuMenjoTsuchiDto dto = fullDto();
        dto.setZeigaku("0");

        jp.lg.asp.accommodation.dto.KanpuMenjoTsuchiReportsDto reportsDto = captureReportsDto(dto);

        assertThat(reportsDto.getZeigaku()).isEqualTo("0");
    }

    // =======================================================================
    // No.44 buildParams - 境界値: hakkoYmdが令和元年(2019-05-01) → 和暦変換
    // =======================================================================

    @Test
    void buildParams_境界値_hakkoYmdが令和元年_和暦変換() throws Exception {
        KanpuMenjoTsuchiDto dto = fullDto();
        dto.setHakkoYmd(LocalDate.of(2019, 5, 1));

        jp.lg.asp.accommodation.dto.KanpuMenjoTsuchiReportsDto reportsDto = captureReportsDto(dto);

        assertThat(reportsDto.getHakkoYmd()).isEqualTo("令和1年5月1日");
    }

    // =======================================================================
    // No.45 buildParams - 境界値: shinseiYmが令和元年(2019-05) → 和暦変換
    // =======================================================================

    @Test
    void buildParams_境界値_shinseiYmが令和元年_和暦変換() throws Exception {
        KanpuMenjoTsuchiDto dto = fullDto();
        dto.setShinseiYm("2019-05");

        jp.lg.asp.accommodation.dto.KanpuMenjoTsuchiReportsDto reportsDto = captureReportsDto(dto);

        assertThat(reportsDto.getShinseiYm()).isEqualTo("令和1年5月");
    }

    // =======================================================================
    // No.46 generateTsuchiPdf - 境界値: shinsei_kbnがnull → IllegalArgumentExceptionスロー
    // =======================================================================

    @Test
    void generateTsuchiPdf_境界値_shinsei_kbnがnull_IllegalArgumentExceptionスロー() {
        try (MockedStatic<net.sf.jasperreports.engine.JasperCompileManager> compileMock =
                     mockStatic(net.sf.jasperreports.engine.JasperCompileManager.class)) {
            compileMock.when(() -> net.sf.jasperreports.engine.JasperCompileManager
                    .compileReport(any(java.io.InputStream.class)))
                    .thenReturn(mock(net.sf.jasperreports.engine.JasperReport.class));

            KanpuMenjoTsuchiDto dto = fullDto();
            dto.setShinsei_kbn(null);

            assertThatThrownBy(() -> service.generateTsuchiPdf(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("申請の区分は必須です。");
        }
    }

    // =======================================================================
    // No.47 generateTsuchiPdf - 境界値: kettei_naiyouがnull → IllegalArgumentExceptionスロー
    // =======================================================================

    @Test
    void generateTsuchiPdf_境界値_kettei_naiyouがnull_IllegalArgumentExceptionスロー() {
        try (MockedStatic<net.sf.jasperreports.engine.JasperCompileManager> compileMock =
                     mockStatic(net.sf.jasperreports.engine.JasperCompileManager.class)) {
            compileMock.when(() -> net.sf.jasperreports.engine.JasperCompileManager
                    .compileReport(any(java.io.InputStream.class)))
                    .thenReturn(mock(net.sf.jasperreports.engine.JasperReport.class));

            KanpuMenjoTsuchiDto dto = fullDto();
            dto.setKettei_naiyou(null);

            assertThatThrownBy(() -> service.generateTsuchiPdf(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("決定の内容は必須です。");
        }
    }
}

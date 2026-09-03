package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.NozeiKanrininNinteiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.NozeiKanrininNinteiReportsServiceImpl;
import jp.lg.asp.accommodation.service.impl.NozeiKanrininNinteiServiceImpl;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;

@ExtendWith(MockitoExtension.class)
class NozeiKanrininNinteiServiceImplTest {

    @Mock TokugimuRepository tokugimuRepository;
    @Mock AtenaRepository atenaRepository;
    @Mock JichitaiRepository jichitaiRepository;
    @Mock ReportsCommonService reportsCommonService;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks NozeiKanrininNinteiServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "001001";

    @BeforeEach
    void setUp() {
        lenient().when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    private Jichitai jichitai() {
        Jichitai j = new Jichitai();
        j.setName("テスト市");
        j.setKbnName("市");
        return j;
    }

    private Tokugimu tokugimu() {
        Tokugimu t = new Tokugimu();
        t.setAtenaNo(new BigDecimal("1001"));
        t.setShisetsuYubinNo("1234567");
        t.setShisetsuJusho("市...");
        t.setShisetsuName("テスト施設");
        return t;
    }

    private Atena atena() {
        Atena a = new Atena();
        a.setYubinNo("1234567");
        a.setJusho("市...");
        a.setName("テスト太郎");
        return a;
    }

    // =======================================================================
    // No.26 正常系: 全情報あり → 全フィールドが設定されたDTOを返す
    // =======================================================================

    @Test
    void getNinteiInfo_正常系_全情報あり_全フィールドが設定される() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai()));
        when(reportsCommonService.getReportsDefText(any())).thenReturn("テスト条例文1");
        when(reportsCommonService.getReportsDefData(any())).thenReturn(new byte[]{1});
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tokugimu()));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(eq(JICHITAI_CD), any()))
                .thenReturn(Optional.of(atena()));

        NozeiKanrininNinteiDto dto = service.getNinteiInfo(SHITEI_NO);

        assertThat(dto.getShiteiNo()).isEqualTo(SHITEI_NO);
        assertThat(dto.getCityName()).isEqualTo("テスト市市");
        assertThat(dto.getJorei()).isEqualTo("テスト条例文1");
        assertThat(dto.getNintei()).isEqualTo("認定");
        assertThat(dto.getKoin()).isEqualTo(new byte[]{1});
        assertThat(dto.getTokuYubin()).isEqualTo("〒1234567");
        assertThat(dto.getTokuJusho()).isEqualTo("市...");
        assertThat(dto.getTokuName()).isEqualTo("テスト太郎");
        assertThat(dto.getShisetsuYubin()).isEqualTo("〒1234567");
        assertThat(dto.getShisetsuJusho()).isEqualTo("市...");
        assertThat(dto.getShisetsuName()).isEqualTo("テスト施設");
    }

    // =======================================================================
    // No.27 異常系: 条例設定値がnull → RuntimeExceptionスロー
    // =======================================================================

    @Test
    void getNinteiInfo_異常系_条例設定値がnull_RuntimeExceptionスロー() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai()));
        when(reportsCommonService.getReportsDefText(any())).thenReturn(null);

        assertThatThrownBy(() -> service.getNinteiInfo(SHITEI_NO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("帳票出力項目が未設定です。管理者にお問い合わせください。");
    }

    // =======================================================================
    // No.28 異常系: 条例設定値が空文字 → RuntimeExceptionスロー
    // =======================================================================

    @Test
    void getNinteiInfo_異常系_条例設定値が空文字_RuntimeExceptionスロー() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai()));
        when(reportsCommonService.getReportsDefText(any())).thenReturn("");

        assertThatThrownBy(() -> service.getNinteiInfo(SHITEI_NO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("帳票出力項目が未設定です。管理者にお問い合わせください。");
    }

    // =======================================================================
    // No.29 異常系: 自治体情報がnull・条例設定値もnull → RuntimeExceptionスロー
    // =======================================================================

    @Test
    void getNinteiInfo_異常系_自治体情報がnullかつ条例設定値もnull_RuntimeExceptionスロー() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.empty());
        when(reportsCommonService.getReportsDefText(any())).thenReturn(null);

        assertThatThrownBy(() -> service.getNinteiInfo(SHITEI_NO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("帳票出力項目が未設定です。管理者にお問い合わせください。");
    }

    // =======================================================================
    // No.30 正常系: 自治体情報がnull・条例設定値あり → 設定値を使用
    // =======================================================================

    @Test
    void getNinteiInfo_正常系_自治体情報がnullかつ条例設定値あり_設定値を使用() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.empty());
        when(reportsCommonService.getReportsDefText(any())).thenReturn("条例文1");
        when(reportsCommonService.getReportsDefData(any())).thenReturn(new byte[]{1});
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tokugimu()));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(eq(JICHITAI_CD), any()))
                .thenReturn(Optional.of(atena()));

        NozeiKanrininNinteiDto dto = service.getNinteiInfo(SHITEI_NO);

        assertThat(dto.getJorei()).isEqualTo("条例文1");
    }

    // =======================================================================
    // No.31 異常系: 公印(koin)がnull → RuntimeExceptionスロー
    // =======================================================================

    @Test
    void getNinteiInfo_異常系_koinがnull_RuntimeExceptionスロー() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai()));
        when(reportsCommonService.getReportsDefText(any())).thenReturn("条例文");
        when(reportsCommonService.getReportsDefData(any())).thenReturn(null);

        assertThatThrownBy(() -> service.getNinteiInfo(SHITEI_NO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("公印が未設定です。管理者にお問い合わせください。");
    }

    // =======================================================================
    // No.32 異常系: 特別徴収義務者がない → RuntimeExceptionスロー
    // =======================================================================

    @Test
    void getNinteiInfo_異常系_特別徴収義務者がない_RuntimeExceptionスロー() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai()));
        when(reportsCommonService.getReportsDefText(any())).thenReturn("条例文");
        when(reportsCommonService.getReportsDefData(any())).thenReturn(new byte[]{1});
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.getNinteiInfo(SHITEI_NO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(SHITEI_NO);
    }

    // =======================================================================
    // No.33 異常系: 宛名情報がない → RuntimeExceptionスロー
    // =======================================================================

    @Test
    void getNinteiInfo_異常系_宛名情報がない_RuntimeExceptionスロー() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai()));
        when(reportsCommonService.getReportsDefText(any())).thenReturn("条例文");
        when(reportsCommonService.getReportsDefData(any())).thenReturn(new byte[]{1});
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tokugimu()));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(eq(JICHITAI_CD), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getNinteiInfo(SHITEI_NO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("宛名情報が見つかりません");
    }
}

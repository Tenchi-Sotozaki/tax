package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.KofuKetteiTsuchiShinseiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.FurikomiKoza;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.ReportsDef;
import jp.lg.asp.accommodation.entity.Shoreikin;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.FurikomiKozaRepository;
import jp.lg.asp.accommodation.repository.ReportsDefRepository;
import jp.lg.asp.accommodation.repository.ShoreikinRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.KofuKetteiTsuchiShinseiServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class kofuKetteiTsuchiShinseiServiceImplTest {

    @Mock TokugimuRepository tokugimuRepository;
    @Mock AtenaRepository atenaRepository;
    @Mock ShoreikinRepository shoreikinRepository;
    @Mock ReportsDefRepository reportsDefRepository;
    @Mock ReportsCommonService reportsCommonService;
    @Mock JichitaiContext jichitaiContext;
    @Mock FurikomiKozaRepository furikomiKozaRepository;

    @InjectMocks KofuKetteiTsuchiShinseiServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "00100001";
    private static final String NENDO = "2024";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        Jichitai jichitai = new Jichitai();
        jichitai.setName("テスト");
        jichitai.setKbnName("市");;
        when(reportsCommonService.getJichitaiInfo()).thenReturn(jichitai);
        when(reportsCommonService.getReportsDefText(any())).thenReturn("テスト条例");
        when(reportsCommonService.getReportsDefData(any())).thenReturn(new byte[0]);
		when(reportsDefRepository.findByIdAndJichitaiCd(eq("KOFU_HAKKO_YOSHIKI"), any()))
				.thenReturn(Optional.of(new ReportsDef()));
		when(reportsDefRepository.findByIdAndJichitaiCd(eq("KOFU_JOKEN"), any()))
				.thenReturn(Optional.of(new ReportsDef()));
    }

    @Test
    void getReportData_年度指定_正常取得() {
        Tokugimu tokugimu = new Tokugimu();
        tokugimu.setShiteiNo(SHITEI_NO);
        tokugimu.setAtenaNo(BigDecimal.valueOf(1001));
        tokugimu.setShisetsuName("テスト施設");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.of(tokugimu));

        Atena atena = new Atena();
        atena.setName("テスト太郎");
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.valueOf(1001)))
                .thenReturn(Optional.of(atena));

        Shoreikin shoreikin = new Shoreikin();
        shoreikin.setKofuZeigaku(500000L);
        shoreikin.setKofuGaku(100000L);
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(JICHITAI_CD, SHITEI_NO, NENDO))
                .thenReturn(Optional.of(shoreikin));
        
        FurikomiKoza koza = new FurikomiKoza();
        koza.setBankName("テスト銀行");
        koza.setBranchName("本店");
        koza.setMeigi("テストタロウ");
        koza.setKozaNo("1234567");
        when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(Optional.of(koza));

        KofuKetteiTsuchiShinseiDto result = service.getReportData(SHITEI_NO, NENDO);

        assertThat(result).isNotNull();
        assertThat(result.getTokuName()).isEqualTo("テスト太郎");
        assertThat(result.getNonyugaku()).isEqualTo("500,000");
        assertThat(result.getKofugaku()).isEqualTo("100,000");
    }

    @Test
    void getReportData_特別徴収義務者なしはnull() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.empty());

        KofuKetteiTsuchiShinseiDto result = service.getReportData(SHITEI_NO, NENDO);

        assertThat(result).isNull();
    }

    @Test
    void getReportData_宛名なしはnull() {
        Tokugimu tokugimu = new Tokugimu();
        tokugimu.setShiteiNo(SHITEI_NO);
        tokugimu.setAtenaNo(BigDecimal.valueOf(9999));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.of(tokugimu));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.valueOf(9999)))
                .thenReturn(Optional.empty());

        KofuKetteiTsuchiShinseiDto result = service.getReportData(SHITEI_NO, NENDO);

        assertThat(result).isNull();
    }

    @Test
    void getReportData_奨励金なしの場合はnullを返す() {
        Tokugimu tokugimu = new Tokugimu();
        tokugimu.setShiteiNo(SHITEI_NO);
        tokugimu.setAtenaNo(BigDecimal.valueOf(1001));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.of(tokugimu));
        Atena atena = new Atena();
        atena.setName("テスト太郎");
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.valueOf(1001)))
                .thenReturn(Optional.of(atena));
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(JICHITAI_CD, SHITEI_NO, NENDO))
                .thenReturn(Optional.empty());

        KofuKetteiTsuchiShinseiDto result = service.getReportData(SHITEI_NO, NENDO);

        assertThat(result).isNull();
    }
}
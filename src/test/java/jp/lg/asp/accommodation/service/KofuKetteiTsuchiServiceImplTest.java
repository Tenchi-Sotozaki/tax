package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.KofuKetteiTsuchiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Shoreikin;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.ReportsDefRepository;
import jp.lg.asp.accommodation.repository.ShoreikinRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.KofuKetteiTsuchiServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KofuKetteiTsuchiServiceImplTest {

    @Mock TokugimuRepository tokugimuRepository;
    @Mock AtenaRepository atenaRepository;
    @Mock ShoreikinRepository shoreikinRepository;
    @Mock ReportsDefRepository reportsDefRepository;
    @Mock ReportsCommonService reportsCommonService;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks KofuKetteiTsuchiServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "00100001";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        Jichitai jichitai = new Jichitai();
        jichitai.setName("テスト市");
        when(reportsCommonService.getJichitaiInfo()).thenReturn(jichitai);
        when(reportsCommonService.getReportsDefText(any())).thenReturn("テスト条例");
        when(reportsCommonService.getReportsDefData(any())).thenReturn(new byte[0]);
        when(reportsDefRepository.findByIdAndJichitaiCd(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void getReportData_正常取得() {
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
        shoreikin.setShiteiNo(SHITEI_NO);
        shoreikin.setKofuGaku(100000L);
        when(shoreikinRepository.findActiveByJichitaiCd(JICHITAI_CD)).thenReturn(List.of(shoreikin));

        KofuKetteiTsuchiDto result = service.getReportData(SHITEI_NO);

        assertThat(result).isNotNull();
        assertThat(result.getTokugimuName()).isEqualTo("テスト太郎");
        assertThat(result.getShisetsuName()).isEqualTo("テスト施設");
        assertThat(result.getKofugaku()).isEqualTo("100000");
    }

    @Test
    void getReportData_特別徴収義務者なしはnull() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.empty());

        KofuKetteiTsuchiDto result = service.getReportData(SHITEI_NO);

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

        KofuKetteiTsuchiDto result = service.getReportData(SHITEI_NO);

        assertThat(result).isNull();
    }

    @Test
    void getReportData_奨励金なしはkofugaku0() {
        Tokugimu tokugimu = new Tokugimu();
        tokugimu.setShiteiNo(SHITEI_NO);
        tokugimu.setAtenaNo(BigDecimal.valueOf(1001));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.of(tokugimu));
        Atena atena = new Atena();
        atena.setName("テスト太郎");
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.valueOf(1001)))
                .thenReturn(Optional.of(atena));
        when(shoreikinRepository.findActiveByJichitaiCd(JICHITAI_CD)).thenReturn(List.of());

        KofuKetteiTsuchiDto result = service.getReportData(SHITEI_NO);

        assertThat(result).isNotNull();
        assertThat(result.getKofugaku()).isEqualTo("0");
    }
}

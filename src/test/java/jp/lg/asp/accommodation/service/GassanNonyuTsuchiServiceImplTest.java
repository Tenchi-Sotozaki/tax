package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
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
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.GassanNonyuTsuchiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Gassan;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Nokigen;
import jp.lg.asp.accommodation.entity.NokigenId;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.GassanRepository;
import jp.lg.asp.accommodation.repository.NokigenRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.GassanNonyuTsuchiServiceImpl;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class GassanNonyuTsuchiServiceImplTest {

    @Mock TokugimuRepository tokugimuRepository;
    @Mock AtenaRepository atenaRepository;
    @Mock GassanRepository gassanRepository;
    @Mock NokigenRepository nokigenRepository;
    @Mock ReportsCommonService reportsCommonService;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks GassanNonyuTsuchiServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "00000001";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        Jichitai jichitai = new Jichitai();
        jichitai.setName("占冠村");
        jichitai.setNendoStMonth("4");
        when(reportsCommonService.getJichitaiInfo()).thenReturn(jichitai);
        when(reportsCommonService.getReportsDefText(any())).thenReturn("宿泊税条例");
        when(reportsCommonService.getReportsDefData(any())).thenReturn(new byte[0]);
    }

    private Nokigen buildNokigen(String... dates) {
        Nokigen nokigen = new Nokigen();
        nokigen.setNokigen1st(dates[0]);
        nokigen.setNokigen2nd(dates[1]);
        nokigen.setNokigen3rd(dates[2]);
        nokigen.setNokigen4th(dates[3]);
        nokigen.setNokigen5th(dates[4]);
        nokigen.setNokigen6th(dates[5]);
        nokigen.setNokigen7th(dates[6]);
        nokigen.setNokigen8th(dates[7]);
        nokigen.setNokigen9th(dates[8]);
        nokigen.setNokigen10th(dates[9]);
        nokigen.setNokigen11th(dates[10]);
        nokigen.setNokigen12th(dates[11]);
        return nokigen;
    }

    @Test
    void getGassanNonyuTsuchiInfo_success_withGassan() {
        Tokugimu tokugimu = new Tokugimu();
        tokugimu.setShiteiNo(SHITEI_NO);
        tokugimu.setAtenaNo(BigDecimal.ONE);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.of(tokugimu));

        Atena atena = new Atena();
        atena.setName("テスト事業者");
        atena.setYubinNo("060-0001");
        atena.setJusho("北海道");
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.of(atena));

        // 適用開始年月: 2024年4月 → 年度開始月4月なので1期
        Gassan gassan = new Gassan();
        gassan.setGassanShiteiNo("90000001");
        gassan.setTekiyoStYmd(LocalDate.of(2024, 4, 1));
        when(gassanRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(gassan));

        // 2024年度の納入期限マスタ: 1期=20240531
        Nokigen nokigen = buildNokigen(
                "20240531", "20240630", "20240731", "20240831",
                "20240930", "20241031", "20241130", "20241231",
                "20250131", "20250228", "20250331", "20250430");
        when(nokigenRepository.findById(new NokigenId(JICHITAI_CD, "2024")))
                .thenReturn(Optional.of(nokigen));

        GassanNonyuTsuchiDto result = service.getGassanNonyuTsuchiInfo(SHITEI_NO);

        assertThat(result).isNotNull();
        assertThat(result.getTokuName()).isEqualTo("テスト事業者");
        assertThat(result.getGassanShiteiNo()).isEqualTo("90000001");
        assertThat(result.getCity()).isEqualTo("占冠村");
        assertThat(result.getNonyuKigen()).isEqualTo("5月31日");
    }

    @Test
    void getGassanNonyuTsuchiInfo_nonyuKigen_12ki() {
        // 適用開始年月: 2025年3月 → 年度開始月4月なので12期、年度は2024
        Tokugimu tokugimu = new Tokugimu();
        tokugimu.setShiteiNo(SHITEI_NO);
        tokugimu.setAtenaNo(BigDecimal.ONE);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.of(tokugimu));

        Atena atena = new Atena();
        atena.setName("テスト事業者");
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.of(atena));

        Gassan gassan = new Gassan();
        gassan.setGassanShiteiNo("90000001");
        gassan.setTekiyoStYmd(LocalDate.of(2025, 3, 1));
        when(gassanRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(gassan));

        Nokigen nokigen = buildNokigen(
                "20240531", "20240630", "20240731", "20240831",
                "20240930", "20241031", "20241130", "20241231",
                "20250131", "20250228", "20250331", "20250430");
        when(nokigenRepository.findById(new NokigenId(JICHITAI_CD, "2024")))
                .thenReturn(Optional.of(nokigen));

        GassanNonyuTsuchiDto result = service.getGassanNonyuTsuchiInfo(SHITEI_NO);

        assertThat(result.getNonyuKigen()).isEqualTo("4月30日");
    }

    @Test
    void getGassanNonyuTsuchiInfo_nokigenNotFound_nonyuKigenIsNull() {
        Tokugimu tokugimu = new Tokugimu();
        tokugimu.setShiteiNo(SHITEI_NO);
        tokugimu.setAtenaNo(BigDecimal.ONE);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.of(tokugimu));

        Atena atena = new Atena();
        atena.setName("テスト事業者");
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.of(atena));

        Gassan gassan = new Gassan();
        gassan.setGassanShiteiNo("90000001");
        gassan.setTekiyoStYmd(LocalDate.of(2024, 4, 1));
        when(gassanRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(gassan));

        when(nokigenRepository.findById(any())).thenReturn(Optional.empty());

        GassanNonyuTsuchiDto result = service.getGassanNonyuTsuchiInfo(SHITEI_NO);

        assertThat(result.getNonyuKigen()).isNull();
    }

    @Test
    void getGassanNonyuTsuchiInfo_tokugimuNotFound_returnsNull() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThat(service.getGassanNonyuTsuchiInfo(SHITEI_NO)).isNull();
    }

    @Test
    void getGassanNonyuTsuchiInfo_atenaNotFound_returnsNull() {
        Tokugimu tokugimu = new Tokugimu();
        tokugimu.setAtenaNo(BigDecimal.ONE);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(any(), any(), any(), any()))
                .thenReturn(Optional.of(tokugimu));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(any(), any())).thenReturn(Optional.empty());

        assertThat(service.getGassanNonyuTsuchiInfo(SHITEI_NO)).isNull();
    }

    @Test
    void getGassanNonyuTsuchiInfo_noGassan_gassanShiteiNoIsNull() {
        Tokugimu tokugimu = new Tokugimu();
        tokugimu.setShiteiNo(SHITEI_NO);
        tokugimu.setAtenaNo(BigDecimal.ONE);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(any(), any(), any(), any()))
                .thenReturn(Optional.of(tokugimu));

        Atena atena = new Atena();
        atena.setName("事業者");
        when(atenaRepository.findByJichitaiCdAndAtenaNo(any(), any())).thenReturn(Optional.of(atena));
        when(gassanRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of());

        GassanNonyuTsuchiDto result = service.getGassanNonyuTsuchiInfo(SHITEI_NO);

        assertThat(result.getGassanShiteiNo()).isNull();
        assertThat(result.getNonyuKigen()).isNull();
    }
}

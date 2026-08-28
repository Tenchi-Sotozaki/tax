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

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.TokugimuJuriTsuchiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.TokugimuJuriTsuchiServiceImpl;

@ExtendWith(MockitoExtension.class)
class TokugimuJuriTsuchiServiceImplTest {

    @Mock TokugimuRepository tokugimuRepository;
    @Mock AtenaRepository atenaRepository;
    @Mock ReportsCommonService reportsCommonService;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks TokugimuJuriTsuchiServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "00000001";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        Jichitai jichitai = new Jichitai();
        jichitai.setName("占冠村");
        when(reportsCommonService.getJichitaiInfo()).thenReturn(jichitai);
        when(reportsCommonService.getReportsDefText(any())).thenReturn("宿泊税条例");
    }

    @Test
    void getTokugimuInfo_success() {
        Tokugimu tokugimu = new Tokugimu();
        tokugimu.setShiteiNo(SHITEI_NO);
        tokugimu.setAtenaNo(BigDecimal.ONE);
        tokugimu.setShisetsuName("テスト施設");
        tokugimu.setShisetsuYubinNo("060-0001");
        tokugimu.setShisetsuJusho("北海道占冠村");
        tokugimu.setBiko("");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.of(tokugimu));

        Atena atena = new Atena();
        atena.setName("テスト事業者");
        atena.setYubinNo("060-0001");
        atena.setJusho("北海道占冠村");
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.of(atena));

        TokugimuJuriTsuchiDto result = service.getTokugimuInfo(SHITEI_NO);

        assertThat(result).isNotNull();
        assertThat(result.getShiteiNo()).isEqualTo(SHITEI_NO);
        assertThat(result.getTokuName()).isEqualTo("テスト事業者");
        assertThat(result.getShisetsuName()).isEqualTo("テスト施設");
        assertThat(result.getCityName()).isEqualTo("占冠村");
        assertThat(result.getTokuYubin()).isEqualTo("〒060-0001");
        assertThat(result.getTokuJusho()).isEqualTo("北海道占冠村");
    }

    @Test
    void getTokugimuInfo_tokugimuNotFound_returnsNull() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThat(service.getTokugimuInfo(SHITEI_NO)).isNull();
    }

    @Test
    void getTokugimuInfo_atenaNotFound_returnsNull() {
        Tokugimu tokugimu = new Tokugimu();
        tokugimu.setAtenaNo(BigDecimal.ONE);
        tokugimu.setBiko("");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(any(), any(), any(), any()))
                .thenReturn(Optional.of(tokugimu));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(any(), any())).thenReturn(Optional.empty());

        assertThat(service.getTokugimuInfo(SHITEI_NO)).isNull();
    }

    @Test
    void getTokugimuInfo_noYubinNo_addressWithoutPrefix() {
        Tokugimu tokugimu = new Tokugimu();
        tokugimu.setShiteiNo(SHITEI_NO);
        tokugimu.setAtenaNo(BigDecimal.ONE);
        tokugimu.setShisetsuName("施設");
        tokugimu.setShisetsuYubinNo(null);
        tokugimu.setShisetsuJusho("北海道");
        tokugimu.setBiko("");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(any(), any(), any(), any()))
                .thenReturn(Optional.of(tokugimu));

        Atena atena = new Atena();
        atena.setName("事業者");
        atena.setYubinNo(null);
        atena.setJusho("北海道");
        when(atenaRepository.findByJichitaiCdAndAtenaNo(any(), any())).thenReturn(Optional.of(atena));

        TokugimuJuriTsuchiDto result = service.getTokugimuInfo(SHITEI_NO);

        assertThat(result.getTokuJusho()).isEqualTo("北海道");
    }
}

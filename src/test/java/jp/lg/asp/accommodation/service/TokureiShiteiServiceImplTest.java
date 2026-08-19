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
import jp.lg.asp.accommodation.dto.TokureiShiteiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Nokan;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.NokanRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.TokureiShiteiServiceImpl;

@ExtendWith(MockitoExtension.class)
class TokureiShiteiServiceImplTest {

    @Mock TokugimuRepository tokugimuRepository;
    @Mock AtenaRepository atenaRepository;
    @Mock NokanRepository nokanRepository;
    @Mock ReportsCommonService reportsCommonService;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks TokureiShiteiServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "00000001";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        Jichitai jichitai = new Jichitai();
        jichitai.setName("占冠村");
        when(reportsCommonService.getJichitaiInfo()).thenReturn(jichitai);
        when(reportsCommonService.getReportsDefText(any())).thenReturn("宿泊税条例");
        when(reportsCommonService.getReportsDefData(any())).thenReturn(new byte[0]);
    }

    @Test
    void getTokugimuInfo_success() {
        Tokugimu tokugimu = new Tokugimu();
        tokugimu.setShiteiNo(SHITEI_NO);
        tokugimu.setAtenaNo(BigDecimal.ONE);
        tokugimu.setShisetsuName("テスト施設");
        tokugimu.setShisetsuYubinNo("060-0001");
        tokugimu.setShisetsuJusho("北海道");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.of(tokugimu));

        Atena atena = new Atena();
        atena.setName("テスト事業者");
        atena.setYubinNo("060-0001");
        atena.setJusho("北海道");
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.of(atena));
        
        Nokan nokan = new Nokan();
        nokan.setKbn("承認");
        when(nokanRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(Optional.of(nokan));

        TokureiShiteiDto result = service.getTokugimuInfo(SHITEI_NO);

        assertThat(result).isNotNull();
        assertThat(result.getShiteiNo()).isEqualTo(SHITEI_NO);
        assertThat(result.getTokuName()).isEqualTo("テスト事業者");
        assertThat(result.getCity()).isEqualTo("占冠村");
<<<<<<< HEAD
        assertThat(result.getShonin()).isEqualTo("承認");
        assertThat(result.getTokuJusho()).contains("〒060-0001");
        assertThat(result.getShisetsuJusho()).contains("〒060-0001");
=======
        
        assertThat(result.getTokuYubin()).isEqualTo("〒060-0001");
        assertThat(result.getTokuJusho()).isEqualTo("北海道");

        assertThat(result.getShisetsuYubin()).isEqualTo("〒060-0001");
        assertThat(result.getShisetsuJusho()).isEqualTo("北海道");
>>>>>>> master
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
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(any(), any(), any(), any()))
                .thenReturn(Optional.of(tokugimu));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(any(), any())).thenReturn(Optional.empty());

        assertThat(service.getTokugimuInfo(SHITEI_NO)).isNull();
    }
}

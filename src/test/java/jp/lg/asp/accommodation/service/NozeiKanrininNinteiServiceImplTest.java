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

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.NozeiKanrininNinteiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.NozeiKanrininNinteiServiceImpl;

@ExtendWith(MockitoExtension.class)
class NozeiKanrininNinteiServiceImplTest {

    @Mock TokugimuRepository tokugimuRepository;
    @Mock AtenaRepository atenaRepository;
    @Mock JichitaiRepository jichitaiRepository;
    @Mock ReportsCommonService reportsCommonService;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks NozeiKanrininNinteiServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "00000001";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    @Test
    void getNinteiInfo_success() {
        Jichitai jichitai = new Jichitai();
        jichitai.setName("占冠");
        jichitai.setKbnName("村");
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));

        Tokugimu tokugimu = new Tokugimu();
        tokugimu.setShiteiNo(SHITEI_NO);
        tokugimu.setAtenaNo(BigDecimal.ONE);
        tokugimu.setShisetsuName("テスト施設");
        tokugimu.setShisetsuYubinNo("060-0001");
        tokugimu.setShisetsuJusho("北海道占冠村");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of(tokugimu));

        Atena atena = new Atena();
        atena.setName("テスト事業者");
        atena.setYubinNo("060-0001");
        atena.setJusho("北海道占冠村");
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE)).thenReturn(Optional.of(atena));
        when(reportsCommonService.getReportsDefData(any())).thenReturn(new byte[0]);

        NozeiKanrininNinteiDto result = service.getNinteiInfo(SHITEI_NO);

        assertThat(result.getShiteiNo()).isEqualTo(SHITEI_NO);
        assertThat(result.getCityName()).isEqualTo("占冠村");
        assertThat(result.getTokuName()).isEqualTo("テスト事業者");
        assertThat(result.getShisetsuName()).isEqualTo("テスト施設");
    }

    @Test
    void getNinteiInfo_tokugimuNotFound_throwsException() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.empty());
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of());
        when(reportsCommonService.getReportsDefData(any())).thenReturn(new byte[0]);

        assertThatThrownBy(() -> service.getNinteiInfo(SHITEI_NO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("特別徴収義務者が見つかりません");
    }

    @Test
    void getNinteiInfo_atenaNotFound_throwsException() {
        Jichitai jichitai = new Jichitai();
        jichitai.setName("占冠村");
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));

        Tokugimu tokugimu = new Tokugimu();
        tokugimu.setAtenaNo(BigDecimal.ONE);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of(tokugimu));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE)).thenReturn(Optional.empty());
        when(reportsCommonService.getReportsDefData(any())).thenReturn(new byte[0]);

        assertThatThrownBy(() -> service.getNinteiInfo(SHITEI_NO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("宛名情報が見つかりません");
    }
}

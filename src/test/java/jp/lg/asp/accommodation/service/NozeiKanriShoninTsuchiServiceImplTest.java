package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
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
import jp.lg.asp.accommodation.dto.NozeiKanriShoninTsuchiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Nokan;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.NokanRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.NozeiKanriShoninTsuchiServiceImpl;

@ExtendWith(MockitoExtension.class)
class NozeiKanriShoninTsuchiServiceImplTest {

    @Mock TokugimuRepository tokugimuRepository;
    @Mock AtenaRepository atenaRepository;
    @Mock NokanRepository nokanRepository;
    @Mock JichitaiRepository jichitaiRepository;
    @Mock ReportsCommonService reportsCommonService;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks NozeiKanriShoninTsuchiServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "00000001";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(reportsCommonService.getReportsDefData(any())).thenReturn(new byte[0]);
    }

    @Test
    void getNozeiKanriInfo_success_withNokan() {
        Jichitai jichitai = new Jichitai();
        jichitai.setName("占冠村");
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));

        Tokugimu tokugimu = new Tokugimu();
        tokugimu.setShiteiNo(SHITEI_NO);
        tokugimu.setAtenaNo(BigDecimal.ONE);
        tokugimu.setShisetsuName("テスト施設");
        tokugimu.setShisetsuYubinNo("060-0001");
        tokugimu.setShisetsuJusho("北海道");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of(tokugimu));

        Atena atena = new Atena();
        atena.setName("テスト事業者");
        atena.setYubinNo("060-0001");
        atena.setJusho("北海道");
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE)).thenReturn(Optional.of(atena));

        Nokan nokan = new Nokan();
        nokan.setName("納税管理人");
        nokan.setYubinNo("060-0002");
        nokan.setJusho("北海道札幌市");
        when(nokanRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(Optional.of(nokan));

        NozeiKanriShoninTsuchiDto result = service.getNozeiKanriInfo(SHITEI_NO);

        assertThat(result.getCityName()).isEqualTo("占冠村");
        assertThat(result.getTokuName()).isEqualTo("テスト事業者");
        assertThat(result.getShisetsuName()).isEqualTo("テスト施設");
        assertThat(result.getNozeiKanriName()).isEqualTo("納税管理人");
        assertThat(result.getTokuJusho()).contains("〒060-0001");
    }

    @Test
    void getNozeiKanriInfo_tokugimuNotFound_throwsException() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.empty());
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of());

        assertThatThrownBy(() -> service.getNozeiKanriInfo(SHITEI_NO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("特別徴収義務者が見つかりません");
    }

    @Test
    void getNozeiKanriInfo_atenaNotFound_throwsException() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.empty());

        Tokugimu tokugimu = new Tokugimu();
        tokugimu.setAtenaNo(BigDecimal.ONE);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of(tokugimu));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getNozeiKanriInfo(SHITEI_NO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("宛名情報が見つかりません");
    }

    @Test
    void getNozeiKanriInfo_noNokan_nozeiKanriFieldsEmpty() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.empty());

        Tokugimu tokugimu = new Tokugimu();
        tokugimu.setAtenaNo(BigDecimal.ONE);
        tokugimu.setShisetsuName("施設");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of(tokugimu));

        Atena atena = new Atena();
        atena.setName("事業者");
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE)).thenReturn(Optional.of(atena));
        when(nokanRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(Optional.empty());

        NozeiKanriShoninTsuchiDto result = service.getNozeiKanriInfo(SHITEI_NO);

        assertThat(result.getNozeiKanriName()).isNull();
    }
}

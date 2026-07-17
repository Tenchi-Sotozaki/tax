package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import jp.lg.asp.accommodation.dto.ShoreikinRenkeiDto;
import jp.lg.asp.accommodation.entity.FurikomiKoza;
import jp.lg.asp.accommodation.entity.Shoreikin;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.FurikomiKozaRepository;
import jp.lg.asp.accommodation.repository.ShoreikinRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.ShoreikinRenkeiServiceImpl;

@ExtendWith(MockitoExtension.class)
class ShoreikinRenkeiServiceImplTest {

    @Mock EntityManager em;
    @Mock ShoreikinRepository shoreikinRepository;
    @Mock TokugimuRepository tokugimuRepository;
    @Mock FurikomiKozaRepository furikomiKozaRepository;

    @InjectMocks ShoreikinRenkeiServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "00100001";
    private static final String NENDO = "2024";

    @Test
    void findByKeys_存在するキー() {
        Shoreikin shoreikin = new Shoreikin();
        shoreikin.setJichitaiCd(JICHITAI_CD);
        shoreikin.setShiteiNo(SHITEI_NO);
        shoreikin.setNendo(NENDO);
        shoreikin.setKofuGaku(100000L);
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(JICHITAI_CD, SHITEI_NO, NENDO))
                .thenReturn(Optional.of(shoreikin));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of());
        when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(Optional.empty());

        ShoreikinRenkeiDto.Key key = new ShoreikinRenkeiDto.Key();
        key.setShiteiNo(SHITEI_NO);
        key.setNendo(NENDO);

        List<ShoreikinRenkeiDto> result = service.findByKeys(JICHITAI_CD, List.of(key));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getKofuGaku()).isEqualTo(100000L);
    }

    @Test
    void findByKeys_存在しないキーは空リスト() {
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(JICHITAI_CD, SHITEI_NO, NENDO))
                .thenReturn(Optional.empty());

        ShoreikinRenkeiDto.Key key = new ShoreikinRenkeiDto.Key();
        key.setShiteiNo(SHITEI_NO);
        key.setNendo(NENDO);

        List<ShoreikinRenkeiDto> result = service.findByKeys(JICHITAI_CD, List.of(key));

        assertThat(result).isEmpty();
    }

    @Test
    void findByKeys_振込口座情報あり() {
        Shoreikin shoreikin = new Shoreikin();
        shoreikin.setJichitaiCd(JICHITAI_CD);
        shoreikin.setShiteiNo(SHITEI_NO);
        shoreikin.setNendo(NENDO);
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(JICHITAI_CD, SHITEI_NO, NENDO))
                .thenReturn(Optional.of(shoreikin));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of());

        FurikomiKoza koza = new FurikomiKoza();
        koza.setBankName("テスト銀行");
        koza.setKozaNo("1234567");
        when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(Optional.of(koza));

        ShoreikinRenkeiDto.Key key = new ShoreikinRenkeiDto.Key();
        key.setShiteiNo(SHITEI_NO);
        key.setNendo(NENDO);

        List<ShoreikinRenkeiDto> result = service.findByKeys(JICHITAI_CD, List.of(key));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBankName()).isEqualTo("テスト銀行");
        assertThat(result.get(0).getKozaNo()).isEqualTo("1234567");
    }

    @Test
    void findByKeys_Tokugimu宛名情報あり() {
        Shoreikin shoreikin = new Shoreikin();
        shoreikin.setJichitaiCd(JICHITAI_CD);
        shoreikin.setShiteiNo(SHITEI_NO);
        shoreikin.setNendo(NENDO);
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(JICHITAI_CD, SHITEI_NO, NENDO))
                .thenReturn(Optional.of(shoreikin));

        jp.lg.asp.accommodation.entity.Atena atena = new jp.lg.asp.accommodation.entity.Atena();
        atena.setName("テスト太郎");
        Tokugimu tokugimu = new Tokugimu();
        tokugimu.setAtenaNo(java.math.BigDecimal.valueOf(1001));
        tokugimu.setAtena(atena);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of(tokugimu));
        when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(Optional.empty());

        ShoreikinRenkeiDto.Key key = new ShoreikinRenkeiDto.Key();
        key.setShiteiNo(SHITEI_NO);
        key.setNendo(NENDO);

        List<ShoreikinRenkeiDto> result = service.findByKeys(JICHITAI_CD, List.of(key));

        assertThat(result.get(0).getName()).isEqualTo("テスト太郎");
    }
}

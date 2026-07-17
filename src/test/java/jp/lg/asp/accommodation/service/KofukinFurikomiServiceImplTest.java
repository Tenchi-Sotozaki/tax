package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import jp.lg.asp.accommodation.dto.KofukinFurikomiDto;
import jp.lg.asp.accommodation.entity.KofukinFurikomi;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.KofukinFurikomiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.KofukinFurikomiServiceImpl;

@ExtendWith(MockitoExtension.class)
class KofukinFurikomiServiceImplTest {

    @Mock EntityManager em;
    @Mock KofukinFurikomiRepository kofukinFurikomiRepository;
    @Mock TokugimuRepository tokugimuRepository;

    @InjectMocks KofukinFurikomiServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "00100001";
    private static final String TAISHO_YM = "202404";
    private static final Integer RNO = 1;

    private KofukinFurikomi buildEntity() {
        KofukinFurikomi e = new KofukinFurikomi();
        e.setJichitaiCd(JICHITAI_CD);
        e.setShiteiNo(SHITEI_NO);
        e.setTaishoYm(TAISHO_YM);
        e.setRno(RNO);
        e.setFurikomiGaku(100000L);
        e.setFurikomiYmd(LocalDate.of(2024, 5, 1));
        return e;
    }

    @Test
    void findById_存在する場合() {
        when(kofukinFurikomiRepository.findByJichitaiCdAndShiteiNoAndTaishoYmAndRno(JICHITAI_CD, SHITEI_NO, TAISHO_YM, RNO))
                .thenReturn(Optional.of(buildEntity()));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of());

        KofukinFurikomiDto result = service.findById(JICHITAI_CD, SHITEI_NO, TAISHO_YM, RNO);

        assertThat(result).isNotNull();
        assertThat(result.getFurikomiGaku()).isEqualTo(100000L);
    }

    @Test
    void findById_存在しない場合はnull() {
        when(kofukinFurikomiRepository.findByJichitaiCdAndShiteiNoAndTaishoYmAndRno(JICHITAI_CD, SHITEI_NO, TAISHO_YM, RNO))
                .thenReturn(Optional.empty());

        KofukinFurikomiDto result = service.findById(JICHITAI_CD, SHITEI_NO, TAISHO_YM, RNO);

        assertThat(result).isNull();
    }

    @Test
    void findByKeys_複数キー取得() {
        when(kofukinFurikomiRepository.findByJichitaiCdAndShiteiNoAndTaishoYmAndRno(JICHITAI_CD, SHITEI_NO, TAISHO_YM, RNO))
                .thenReturn(Optional.of(buildEntity()));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of());

        KofukinFurikomiDto.Key key = new KofukinFurikomiDto.Key();
        key.setShiteiNo(SHITEI_NO);
        key.setTaishoYm(TAISHO_YM);
        key.setRno(RNO);

        List<KofukinFurikomiDto> result = service.findByKeys(JICHITAI_CD, List.of(key));

        assertThat(result).hasSize(1);
    }

    @Test
    void save_正常保存() {
        KofukinFurikomiDto dto = new KofukinFurikomiDto();
        dto.setJichitaiCd(JICHITAI_CD);
        dto.setShiteiNo(SHITEI_NO);
        dto.setTaishoYm("2024-04");
        dto.setRno(RNO);
        dto.setFurikomiGaku(100000L);

        service.save(dto);

        verify(kofukinFurikomiRepository).save(any(KofukinFurikomi.class));
    }

    @Test
    void delete_論理削除() {
        KofukinFurikomi entity = buildEntity();
        entity.setDelFlg("0");
        when(kofukinFurikomiRepository.findByJichitaiCdAndShiteiNoAndTaishoYmAndRno(JICHITAI_CD, SHITEI_NO, TAISHO_YM, RNO))
                .thenReturn(Optional.of(entity));

        service.delete(JICHITAI_CD, SHITEI_NO, TAISHO_YM, RNO);

        assertThat(entity.getDelFlg()).isEqualTo("1");
        verify(kofukinFurikomiRepository).save(entity);
    }

    @Test
    void delete_存在しない場合は何もしない() {
        when(kofukinFurikomiRepository.findByJichitaiCdAndShiteiNoAndTaishoYmAndRno(JICHITAI_CD, SHITEI_NO, TAISHO_YM, RNO))
                .thenReturn(Optional.empty());

        service.delete(JICHITAI_CD, SHITEI_NO, TAISHO_YM, RNO);

        verify(kofukinFurikomiRepository, never()).save(any());
    }
}

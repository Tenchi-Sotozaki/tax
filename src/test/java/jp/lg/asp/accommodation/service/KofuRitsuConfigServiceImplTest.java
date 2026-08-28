package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.KofuRitsuConfigDto;
import jp.lg.asp.accommodation.entity.KofuRitsu;
import jp.lg.asp.accommodation.entity.KofuRitsuId;
import jp.lg.asp.accommodation.repository.KofuRitsuRepository;
import jp.lg.asp.accommodation.service.impl.KofuRitsuConfigServiceImpl;

@ExtendWith(MockitoExtension.class)
class KofuRitsuConfigServiceImplTest {

    @Mock KofuRitsuRepository kofuRitsuRepository;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks KofuRitsuConfigServiceImpl service;

    private static final String JICHITAI_CD = "011002";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    // ===== findCurrent =====

    @Test
    void findCurrent_最新レコードが存在する() {
        KofuRitsu entity = new KofuRitsu();
        when(kofuRitsuRepository.findCurrentByJichitaiCd(JICHITAI_CD)).thenReturn(Optional.of(entity));

        assertThat(service.findCurrent()).isNotNull();
    }

    @Test
    void findCurrent_最新レコードが無い場合nullを返す() {
        when(kofuRitsuRepository.findCurrentByJichitaiCd(JICHITAI_CD)).thenReturn(Optional.empty());

        assertThat(service.findCurrent()).isNull();
    }

    // ===== findAll =====

    @Test
    void findAll_自治体コードで絞り込んだ一覧を返す() {
        when(kofuRitsuRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of(new KofuRitsu()));

        assertThat(service.findAll()).hasSize(1);
        verify(kofuRitsuRepository).findAllByJichitaiCd(JICHITAI_CD);
    }

    // ===== findByRno =====

    @Test
    void findByRno_正常系() {
        BigDecimal rno = BigDecimal.ONE;
        KofuRitsu entity = new KofuRitsu();
        when(kofuRitsuRepository.findById(new KofuRitsuId(JICHITAI_CD, rno))).thenReturn(Optional.of(entity));

        assertThat(service.findByRno(rno)).isEqualTo(entity);
    }

    @Test
    void findByRno_該当データが無い場合NoSuchElementExceptionをスロー() {
        BigDecimal rno = BigDecimal.ONE;
        when(kofuRitsuRepository.findById(new KofuRitsuId(JICHITAI_CD, rno))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByRno(rno)).isInstanceOf(NoSuchElementException.class);
    }

    // ===== register =====

    @Test
    void register_既存の最新レコードがある場合_newFlgを0にして新規登録() {
        KofuRitsu current = new KofuRitsu();
        current.setNewFlg("1");
        when(kofuRitsuRepository.findCurrentByJichitaiCd(JICHITAI_CD)).thenReturn(Optional.of(current));
        when(kofuRitsuRepository.findNextRno(JICHITAI_CD)).thenReturn(BigDecimal.valueOf(2));
        when(kofuRitsuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        KofuRitsuConfigDto dto = new KofuRitsuConfigDto();
        dto.setKofuRitsu(BigDecimal.valueOf(10));
        dto.setTekiyoStNendo("2024");

        service.register(dto);

        assertThat(current.getNewFlg()).isEqualTo("0");
        verify(kofuRitsuRepository, times(2)).save(any());
    }

    @Test
    void register_既存レコードが無い場合_管理番号1でnewFlg1の新規レコードを登録() {
        when(kofuRitsuRepository.findCurrentByJichitaiCd(JICHITAI_CD)).thenReturn(Optional.empty());
        when(kofuRitsuRepository.findNextRno(JICHITAI_CD)).thenReturn(BigDecimal.ONE);
        when(kofuRitsuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        KofuRitsuConfigDto dto = new KofuRitsuConfigDto();
        dto.setKofuRitsu(BigDecimal.valueOf(10));

        service.register(dto);

        verify(kofuRitsuRepository, times(1)).save(any());
    }

    // ===== update =====

    @Test
    void update_正常系_5項目が更新されrnoとnewFlgは変更されない() {
        BigDecimal rno = BigDecimal.ONE;
        KofuRitsu entity = new KofuRitsu();
        entity.setRno(rno);
        entity.setNewFlg("1");
        when(kofuRitsuRepository.findById(new KofuRitsuId(JICHITAI_CD, rno))).thenReturn(Optional.of(entity));
        when(kofuRitsuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        KofuRitsuConfigDto dto = new KofuRitsuConfigDto();
        dto.setKofuRitsu(new BigDecimal("20.00"));
        dto.setSanshutsu(100);
        dto.setKbn("2");
        dto.setSaiteigaku(new BigDecimal("500"));
        dto.setTekiyoStNendo("2025");

        service.update(rno, dto);

        assertThat(entity.getKofuRitsu()).isEqualTo(new BigDecimal("20.00"));
        assertThat(entity.getSanshutsu()).isEqualTo(100);
        assertThat(entity.getKbn()).isEqualTo("2");
        assertThat(entity.getSaiteigaku()).isEqualTo(new BigDecimal("500"));
        assertThat(entity.getTekiyoStNendo()).isEqualTo("2025");
        assertThat(entity.getRno()).isEqualTo(rno);
        assertThat(entity.getNewFlg()).isEqualTo("1");
    }

    @Test
    void update_該当データが無い場合NoSuchElementExceptionをスロー() {
        BigDecimal rno = BigDecimal.ONE;
        when(kofuRitsuRepository.findById(new KofuRitsuId(JICHITAI_CD, rno))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(rno, new KofuRitsuConfigDto()))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ===== existsByTekiyoStNendo =====

    @Test
    void existsByTekiyoStNendo_存在する場合_trueを返す() {
        when(kofuRitsuRepository.countByJichitaiCdAndTekiyoStNendo(JICHITAI_CD, "2024")).thenReturn(1L);

        assertThat(service.existsByTekiyoStNendo("2024")).isTrue();
    }

    @Test
    void existsByTekiyoStNendo_存在しない場合_falseを返す() {
        when(kofuRitsuRepository.countByJichitaiCdAndTekiyoStNendo(JICHITAI_CD, "2024")).thenReturn(0L);

        assertThat(service.existsByTekiyoStNendo("2024")).isFalse();
    }
}

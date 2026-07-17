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
import jp.lg.asp.accommodation.dto.KofuRitsuConfigDto;
import jp.lg.asp.accommodation.entity.KofuRitsu;
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

    @Test
    void findCurrent_found() {
        KofuRitsu entity = new KofuRitsu();
        when(kofuRitsuRepository.findCurrentByJichitaiCd(JICHITAI_CD)).thenReturn(Optional.of(entity));

        assertThat(service.findCurrent()).isNotNull();
    }

    @Test
    void findCurrent_notFound_returnsNull() {
        when(kofuRitsuRepository.findCurrentByJichitaiCd(JICHITAI_CD)).thenReturn(Optional.empty());

        assertThat(service.findCurrent()).isNull();
    }

    @Test
    void findAll_returnsList() {
        when(kofuRitsuRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of(new KofuRitsu()));

        assertThat(service.findAll()).hasSize(1);
    }

    @Test
    void register_existingCurrent_setsNewFlgToZeroAndCreatesNew() {
        KofuRitsu current = new KofuRitsu();
        current.setNewFlg(1);
        when(kofuRitsuRepository.findCurrentByJichitaiCd(JICHITAI_CD)).thenReturn(Optional.of(current));
        when(kofuRitsuRepository.findNextRno(JICHITAI_CD)).thenReturn(BigDecimal.valueOf(2));
        when(kofuRitsuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        KofuRitsuConfigDto dto = new KofuRitsuConfigDto();
        dto.setKofuRitsu(BigDecimal.valueOf(10));
        dto.setTekiyoStYmd(LocalDate.of(2024, 4, 1));

        service.register(dto);

        assertThat(current.getNewFlg()).isEqualTo(0);
        verify(kofuRitsuRepository, times(2)).save(any());
    }

    @Test
    void register_noCurrent_createsNew() {
        when(kofuRitsuRepository.findCurrentByJichitaiCd(JICHITAI_CD)).thenReturn(Optional.empty());
        when(kofuRitsuRepository.findNextRno(JICHITAI_CD)).thenReturn(BigDecimal.ONE);
        when(kofuRitsuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        KofuRitsuConfigDto dto = new KofuRitsuConfigDto();
        dto.setKofuRitsu(BigDecimal.valueOf(10));

        service.register(dto);

        verify(kofuRitsuRepository, times(1)).save(any());
    }
}

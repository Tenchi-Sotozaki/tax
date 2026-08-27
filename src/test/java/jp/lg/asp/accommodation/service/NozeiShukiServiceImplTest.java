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
import jp.lg.asp.accommodation.dto.NozeiShukiDto;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.NozeiShuki;
import jp.lg.asp.accommodation.entity.NozeiShukiId;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.NozeiShukiRepository;
import jp.lg.asp.accommodation.service.impl.NozeiShukiServiceImpl;

@ExtendWith(MockitoExtension.class)
class NozeiShukiServiceImplTest {

    @Mock NozeiShukiRepository nozeiShukiRepository;
    @Mock JichitaiRepository jichitaiRepository;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks NozeiShukiServiceImpl service;

    private static final String JICHITAI_CD = "011002";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    private Jichitai jichitaiWithMonth(String month) {
        Jichitai j = new Jichitai();
        j.setNendoStMonth(month);
        return j;
    }

    private NozeiShuki shukiEntity(BigDecimal seq, BigDecimal shuki) {
        NozeiShuki n = new NozeiShuki();
        n.setSeq(seq);
        n.setShuki(shuki);
        return n;
    }

    @Test
    void findAll_returnsMappedDtos() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitaiWithMonth("4")));
        when(nozeiShukiRepository.findActiveByJichitaiCd(JICHITAI_CD))
                .thenReturn(List.of(shukiEntity(BigDecimal.ONE, BigDecimal.valueOf(3))));

        List<NozeiShukiDto> result = service.findAll();

        assertThat(result).hasSize(1);
    }

    @Test
    void findByShuki_nullShuki_delegatesToFindAll() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.empty());
        when(nozeiShukiRepository.findActiveByJichitaiCd(JICHITAI_CD)).thenReturn(List.of());

        List<NozeiShukiDto> result = service.findByShuki(null);

        assertThat(result).isEmpty();
    }

    @Test
    void findByShuki_withValue_returnsFiltered() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitaiWithMonth("4")));
        when(nozeiShukiRepository.findActiveByJichitaiCdAndShuki(JICHITAI_CD, BigDecimal.valueOf(3)))
                .thenReturn(List.of(shukiEntity(BigDecimal.ONE, BigDecimal.valueOf(3))));

        List<NozeiShukiDto> result = service.findByShuki(3);

        assertThat(result).hasSize(1);
    }

    @Test
    void findBySeq_found() {
        NozeiShuki entity = shukiEntity(BigDecimal.ONE, BigDecimal.valueOf(3));
        when(nozeiShukiRepository.findById(new NozeiShukiId(JICHITAI_CD, BigDecimal.ONE)))
                .thenReturn(Optional.of(entity));

        assertThat(service.findBySeq(BigDecimal.ONE)).isNotNull();
    }

    @Test
    void findBySeq_notFound_returnsNull() {
        when(nozeiShukiRepository.findById(any())).thenReturn(Optional.empty());

        assertThat(service.findBySeq(BigDecimal.TEN)).isNull();
    }

    @Test
    void existsByShuki_true() {
        when(nozeiShukiRepository.countActiveByJichitaiCdAndShuki(JICHITAI_CD, BigDecimal.valueOf(3))).thenReturn(1L);

        assertThat(service.existsByShuki(BigDecimal.valueOf(3))).isTrue();
    }

    @Test
    void existsByShukiExcludeSeq_false() {
        when(nozeiShukiRepository.countActiveByJichitaiCdAndShukiExcludeSeq(JICHITAI_CD, BigDecimal.valueOf(3), BigDecimal.ONE))
                .thenReturn(0L);

        assertThat(service.existsByShukiExcludeSeq(BigDecimal.valueOf(3), BigDecimal.ONE)).isFalse();
    }

    @Test
    void save_newRecord_assignsSeqAndJichitaiCd() {
        NozeiShuki entity = new NozeiShuki();
        when(nozeiShukiRepository.findNextSeq(JICHITAI_CD)).thenReturn(BigDecimal.valueOf(5));
        when(nozeiShukiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NozeiShuki saved = service.save(entity);

        assertThat(saved.getSeq()).isEqualTo(BigDecimal.valueOf(5));
        assertThat(saved.getJichitaiCd()).isEqualTo(JICHITAI_CD);
        assertThat(saved.getDelFlg()).isEqualTo("0");
    }

    @Test
    void save_existingRecord_doesNotReassignSeq() {
        NozeiShuki entity = new NozeiShuki();
        entity.setSeq(BigDecimal.ONE);
        when(nozeiShukiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.save(entity);

        verify(nozeiShukiRepository, never()).findNextSeq(any());
    }

    @Test
    void delete_setsDelFlg1() {
        NozeiShuki entity = shukiEntity(BigDecimal.ONE, BigDecimal.valueOf(3));
        entity.setDelFlg("0");
        when(nozeiShukiRepository.findById(new NozeiShukiId(JICHITAI_CD, BigDecimal.ONE)))
                .thenReturn(Optional.of(entity));
        when(nozeiShukiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.delete(BigDecimal.ONE);

        assertThat(entity.getDelFlg()).isEqualTo("1");
    }

    @Test
    void delete_notFound_throwsException() {
        when(nozeiShukiRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(BigDecimal.TEN))
                .isInstanceOf(RuntimeException.class);
    }
}

package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.EltaxRenkeiDto;
import jp.lg.asp.accommodation.entity.EltaxRenkei;
import jp.lg.asp.accommodation.entity.EltaxRenkeiId;
import jp.lg.asp.accommodation.repository.EltaxRenkeiRepository;
import jp.lg.asp.accommodation.service.impl.EltaxRenkeiServiceImpl;

@ExtendWith(MockitoExtension.class)
class EltaxRenkeiServiceImplTest {

    @Mock EltaxRenkeiRepository eltaxRenkeiRepository;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks EltaxRenkeiServiceImpl service;

    private static final String JICHITAI_CD = "011002";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    private EltaxRenkei buildEntity(BigDecimal seq) {
        EltaxRenkei e = new EltaxRenkei();
        e.setSeq(seq);
        e.setFileName("test.xml");
        e.setShubetsu("1");
        e.setShoriDt(LocalDateTime.now());
        e.setShoriKekka("OK");
        return e;
    }

    @Test
    void findAll_returnsMappedDtos() {
        when(eltaxRenkeiRepository.findByJichitaiCd(JICHITAI_CD, PageRequest.of(0, 1000)))
                .thenReturn(List.of(buildEntity(BigDecimal.ONE)));

        List<EltaxRenkeiDto> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSeq()).isEqualTo(BigDecimal.ONE);
        assertThat(result.get(0).getFileName()).isEqualTo("test.xml");
    }

    @Test
    void findAll_empty_returnsEmptyList() {
        when(eltaxRenkeiRepository.findByJichitaiCd(JICHITAI_CD, PageRequest.of(0, 1000))).thenReturn(List.of());

        assertThat(service.findAll()).isEmpty();
    }

    @Test
    void findBySeq_found() {
        EltaxRenkei entity = buildEntity(BigDecimal.ONE);
        when(eltaxRenkeiRepository.findById(new EltaxRenkeiId(JICHITAI_CD, BigDecimal.ONE)))
                .thenReturn(Optional.of(entity));

        assertThat(service.findBySeq(BigDecimal.ONE)).isNotNull();
    }

    @Test
    void findBySeq_notFound_returnsNull() {
        when(eltaxRenkeiRepository.findById(any())).thenReturn(Optional.empty());

        assertThat(service.findBySeq(BigDecimal.TEN)).isNull();
    }
}

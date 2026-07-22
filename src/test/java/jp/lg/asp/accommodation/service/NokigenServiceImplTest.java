package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.entity.Nokigen;
import jp.lg.asp.accommodation.entity.NokigenId;
import jp.lg.asp.accommodation.repository.NokigenRepository;
import jp.lg.asp.accommodation.service.impl.NokigenServiceImpl;

@ExtendWith(MockitoExtension.class)
class NokigenServiceImplTest {

    @Mock NokigenRepository nokigenRepository;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks NokigenServiceImpl service;

    private static final String JICHITAI_CD = "011002";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    @Test
    void findAll_returnsListFromRepository() {
        Nokigen n = new Nokigen();
        when(nokigenRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of(n));

        List<Nokigen> result = service.findAll();

        assertThat(result).hasSize(1);
    }

    @Test
    void findByNendo_found() {
        Nokigen n = new Nokigen();
        when(nokigenRepository.findById(new NokigenId(JICHITAI_CD, "2024"))).thenReturn(Optional.of(n));

        Nokigen result = service.findByNendo("2024");

        assertThat(result).isNotNull();
    }

    @Test
    void findByNendo_notFound_returnsNull() {
        when(nokigenRepository.findById(any())).thenReturn(Optional.empty());

        assertThat(service.findByNendo("9999")).isNull();
    }

    @Test
    void existsByNendo_true() {
        when(nokigenRepository.countByJichitaiCdAndNendo(JICHITAI_CD, "2024")).thenReturn(1L);

        assertThat(service.existsByNendo("2024")).isTrue();
    }

    @Test
    void existsByNendo_false() {
        when(nokigenRepository.countByJichitaiCdAndNendo(JICHITAI_CD, "2024")).thenReturn(0L);

        assertThat(service.existsByNendo("2024")).isFalse();
    }

    @Test
    void save_convertsDateFormatAndSaves() {
        Nokigen nokigen = new Nokigen();
        nokigen.setNokigen1st("2024-04-30");
        nokigen.setNokigen2nd("");
        nokigen.setNokigen3rd(null);
        nokigen.setNokigen4th("2024-07-31");
        nokigen.setNokigen5th("");
        nokigen.setNokigen6th("");
        nokigen.setNokigen7th("");
        nokigen.setNokigen8th("");
        nokigen.setNokigen9th("");
        nokigen.setNokigen10th("");
        nokigen.setNokigen11th("");
        nokigen.setNokigen12th("");

        when(nokigenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Nokigen saved = service.save(nokigen);

        assertThat(saved.getNokigen1st()).isEqualTo("20240430");
        assertThat(saved.getNokigen2nd()).isEqualTo("");
        assertThat(saved.getNokigen3rd()).isEqualTo("");
        assertThat(saved.getNokigen4th()).isEqualTo("20240731");
        assertThat(saved.getJichitaiCd()).isEqualTo(JICHITAI_CD);
    }
}

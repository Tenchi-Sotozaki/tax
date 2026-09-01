package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.entity.Nokigen;
import jp.lg.asp.accommodation.repository.HolidayRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.NokigenRepository;
import jp.lg.asp.accommodation.service.impl.NokigenServiceImpl;

@ExtendWith(MockitoExtension.class)
class NokigenServiceImplTest {

    @Mock NokigenRepository nokigenRepository;
    @Mock HolidayRepository holidayRepository;
    @Mock JichitaiRepository jichitaiRepository;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks NokigenServiceImpl service;

    private static final String JICHITAI_CD = "011002";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    // -----------------------------------------------------------------------
    // findAll
    // -----------------------------------------------------------------------

    // TC-01: 正常系・データあり → 戻り値のリストを確認
    @Test
    void findAll_データあり_リストを返す() {
        Nokigen n1 = new Nokigen();
        Nokigen n2 = new Nokigen();
        when(nokigenRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of(n1, n2));

        List<Nokigen> result = service.findAll();

        assertThat(result).containsExactly(n1, n2);
    }

    // TC-02: データなし → 戻り値のリストが空
    @Test
    void findAll_データなし_空リストを返す() {
        when(nokigenRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of());

        List<Nokigen> result = service.findAll();

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // existsByNendo
    // -----------------------------------------------------------------------

    // TC-03: 正常系・対象年度のデータ数＞０ → 戻り値がtrue
    @Test
    void existsByNendo_対象年度のデータ数が1以上_trueを返す() {
        when(nokigenRepository.countByJichitaiCdAndNendo(JICHITAI_CD, "2024")).thenReturn(1L);

        assertThat(service.existsByNendo("2024")).isTrue();
    }

    // TC-04: データなし・対象年度のデータ数＝０ → 戻り値がfalse
    @Test
    void existsByNendo_対象年度のデータ数が0_falseを返す() {
        when(nokigenRepository.countByJichitaiCdAndNendo(JICHITAI_CD, "2024")).thenReturn(0L);

        assertThat(service.existsByNendo("2024")).isFalse();
    }
}

package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.ChoshuGenboRepository;
import jp.lg.asp.accommodation.repository.ChoshuGenboUchiRepository;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.FukaUchiRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.NokigenRepository;
import jp.lg.asp.accommodation.repository.ShunoRirekiRepository;
import jp.lg.asp.accommodation.repository.TekiyoNozeiShukiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeigakuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeiritsuRepository;
import jp.lg.asp.accommodation.service.impl.FukaServiceImpl;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class FukaServiceImplTest {

    @Mock FukaRepository fukaRepository;
    @Mock TokugimuRepository tokugimuRepository;
    @Mock ZeiritsuRepository zeiritsuRepository;
    @Mock ZeiritsuTeigakuRepository zeiritsuTeigakuRepository;
    @Mock ZeiritsuTeiritsuRepository zeiritsuTeiritsuRepository;
    @Mock FukaUchiRepository fukaUchiRepository;
    @Mock ChoshuGenboRepository choshuGenboRepository;
    @Mock ChoshuGenboUchiRepository choshuGenboUchiRepository;
    @Mock AtenaRepository atenaRepository;
    @Mock TekiyoNozeiShukiRepository nozeiShukiRepository;
    @Mock NokigenRepository nokigenRepository;
    @Mock JichitaiRepository jichitaiRepository;
    @Mock ShunoRirekiRepository shunoRirekiRepository;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks FukaServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "00000001";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    // ===== calculateTax =====

    @Test
    void calculateTax_teiritsu_calculatesCorrectly() {
        // 定率制: 10000円 × 10% = 1000円
        long result = service.calculateTax("2", 10000L, BigDecimal.valueOf(10), null);
        assertThat(result).isEqualTo(1000L);
    }

    @Test
    void calculateTax_teiritsu_truncatesDecimal() {
        // 定率制: 999円 × 10% = 99.9 → 99（切り捨て）
        long result = service.calculateTax("2", 999L, BigDecimal.valueOf(10), null);
        assertThat(result).isEqualTo(99L);
    }

    @Test
    void calculateTax_teigaku_multipliesRateByCount() {
        // 定額制: 5泊 × (200円 + 100円) = 1500円
        long result = service.calculateTax("1", 5L, BigDecimal.valueOf(200), BigDecimal.valueOf(100));
        assertThat(result).isEqualTo(1500L);
    }

    @Test
    void calculateTax_nullRates_treatedAsZero() {
        // 定率制: nullはゼロ扱い
        long result = service.calculateTax("2", 10000L, null, null);
        assertThat(result).isEqualTo(0L);
    }

    // ===== isAlreadyRegistered =====

    @Test
    void isAlreadyRegistered_existingData_returnsTrue() {
        // nendoStMonth=3(デフォルト) の場合: 202402 → nendo=2023, kibetsu=12
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(java.util.Optional.empty());
        when(fukaRepository.findLatestByNendoAndKibetsu(JICHITAI_CD, SHITEI_NO, "2023", 12))
                .thenReturn(List.of(new Fuka()));

        assertThat(service.isAlreadyRegistered(SHITEI_NO, "202402")).isTrue();
    }

    @Test
    void isAlreadyRegistered_noData_returnsFalse() {
        // nendoStMonth=4(デフォルト) の場合: 202404 → nendo=2024, kibetsu=1
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(java.util.Optional.empty());
        when(fukaRepository.findLatestByNendoAndKibetsu(JICHITAI_CD, SHITEI_NO, "2024", 1))
                .thenReturn(List.of());

        assertThat(service.isAlreadyRegistered(SHITEI_NO, "202404")).isFalse();
    }

    @Test
    void isAlreadyRegisteredByKibetsu_existingData_returnsTrue() {
        when(fukaRepository.findLatestByNendoAndKibetsu(JICHITAI_CD, SHITEI_NO, "2024", 3))
                .thenReturn(List.of(new Fuka()));

        assertThat(service.isAlreadyRegisteredByKibetsu(SHITEI_NO, "2024", 3)).isTrue();
    }
}

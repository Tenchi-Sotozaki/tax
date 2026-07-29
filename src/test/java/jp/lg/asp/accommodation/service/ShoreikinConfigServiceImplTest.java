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
import jp.lg.asp.accommodation.dto.ShoreikinConfigDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.Shoreikin;
import jp.lg.asp.accommodation.entity.ShoreikinId;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.KofuRitsuRepository;
import jp.lg.asp.accommodation.repository.ShoreikinRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.ShoreikinConfigServiceImpl;

@ExtendWith(MockitoExtension.class)
class ShoreikinConfigServiceImplTest {

    @Mock ShoreikinRepository shoreikinRepository;
    @Mock TokugimuRepository tokugimuRepository;
    @Mock AtenaRepository atenaRepository;
    @Mock FukaRepository fukaRepository;
    @Mock KofuRitsuRepository kofuRitsuRepository;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks ShoreikinConfigServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "00000001";
    private static final String NENDO = "2024";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    @Test
    void getShoreikin_existingRecord_returnsViewMode() {
        Tokugimu t = new Tokugimu();
        t.setShisetsuName("施設");
        t.setAtenaNo(BigDecimal.ONE);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.of(t));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.of(new Atena()));

        Shoreikin shoreikin = new Shoreikin();
        shoreikin.setKofuGaku(10000L);
        shoreikin.setVersion(1);
        when(shoreikinRepository.findById(new ShoreikinId(JICHITAI_CD, SHITEI_NO, NENDO)))
                .thenReturn(Optional.of(shoreikin));

        ShoreikinConfigDto result = service.getShoreikin(SHITEI_NO, NENDO);

        assertThat(result.getMode()).isEqualTo("view");
        assertThat(result.isExists()).isTrue();
    }

    @Test
    void getShoreikin_noRecord_returnsCreateMode() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(shoreikinRepository.findById(any())).thenReturn(Optional.empty());
        when(kofuRitsuRepository.findKofuRitsuByJichitaiCd(eq(JICHITAI_CD), any(LocalDate.class)))
                .thenReturn(List.of(BigDecimal.valueOf(10)));

        ShoreikinConfigDto result = service.getShoreikin(SHITEI_NO, NENDO);

        assertThat(result.getMode()).isEqualTo("create");
        assertThat(result.isExists()).isFalse();
    }

    @Test
    void createShoreikin_savesAndReturnsViewMode() {
        ShoreikinConfigDto dto = new ShoreikinConfigDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setNendo(NENDO);
        dto.setKofuGaku(10000L);
        when(shoreikinRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShoreikinConfigDto result = service.createShoreikin(dto);

        assertThat(result.getMode()).isEqualTo("view");
        assertThat(result.isExists()).isTrue();
        assertThat(result.getVersion()).isEqualTo(1);
    }

    @Test
    void updateShoreikin_versionMismatch_throwsException() {
        ShoreikinConfigDto dto = new ShoreikinConfigDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setNendo(NENDO);
        dto.setVersion(1);

        Shoreikin existing = new Shoreikin();
        existing.setVersion(2);
        when(shoreikinRepository.findById(any())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.updateShoreikin(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("他のユーザー");
    }

    @Test
    void updateShoreikin_notFound_throwsException() {
        ShoreikinConfigDto dto = new ShoreikinConfigDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setNendo(NENDO);
        when(shoreikinRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateShoreikin(dto))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void calculateKofuZeigaku_sumsTotalZeigaku() {
        Fuka f1 = new Fuka();
        f1.setKibetsu(1);
        f1.setRno(1);
        f1.setTotalZeigaku(5000L);

        Fuka f2 = new Fuka();
        f2.setKibetsu(2);
        f2.setRno(1);
        f2.setTotalZeigaku(3000L);

        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoAndDelFlgAndNewFlg(
                JICHITAI_CD, SHITEI_NO, NENDO, "0", "1"))
                .thenReturn(List.of(f1, f2));

        Long result = service.calculateKofuZeigaku(SHITEI_NO, NENDO);

        assertThat(result).isEqualTo(8000L);
    }

    @Test
    void calculateShoreikin_calculatesKofuGaku() {
        ShoreikinConfigDto dto = new ShoreikinConfigDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setNendo(NENDO);
        dto.setKofuRitsu(BigDecimal.valueOf(10));

        Fuka f = new Fuka();
        f.setKibetsu(1);
        f.setRno(1);
        f.setTotalZeigaku(10000L);
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoAndDelFlgAndNewFlg(any(), any(), any(), any(), any()))
                .thenReturn(List.of(f));

        ShoreikinConfigDto result = service.calculateShoreikin(dto);

        assertThat(result.getKofuZeigaku()).isEqualTo(10000L);
        assertThat(result.getKofuGaku()).isEqualTo(1000L); // 10000 * 10 / 100
    }
}

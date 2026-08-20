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
import jp.lg.asp.accommodation.dto.ShoreikinBulkDto;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.Shoreikin;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.KofuRitsuRepository;
import jp.lg.asp.accommodation.repository.ShoreikinRepository;
import jp.lg.asp.accommodation.repository.ShunoRirekiRepository;
import jp.lg.asp.accommodation.service.impl.ShoreikinBulkServiceImpl;

@ExtendWith(MockitoExtension.class)
class ShoreikinBulkServiceImplTest {

    @Mock ShoreikinRepository shoreikinRepository;
    @Mock FukaRepository fukaRepository;
    @Mock ShunoRirekiRepository shunoRirekiRepository;
    @Mock KofuRitsuRepository kofuRitsuRepository;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks ShoreikinBulkServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String NENDO = "2024";

    @BeforeEach
    void setUp() {
        lenient().when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    @Test
    void executeBulkSanshutsu_noNendo_returnsErrorMessage() {
        ShoreikinBulkDto dto = new ShoreikinBulkDto();
        dto.setNendo("");

        ShoreikinBulkDto result = service.executeBulkSanshutsu(dto);

        assertThat(result.getResultMessage()).contains("年度が指定されていません");
        assertThat(result.isExecuted()).isFalse();
    }

    @Test
    void executeBulkSanshutsu_noFuka_zeroTargetCount() {
        ShoreikinBulkDto dto = new ShoreikinBulkDto();
        dto.setNendo(NENDO);
        dto.setKofuRitsu(BigDecimal.valueOf(10));
        when(fukaRepository.findByJichitaiCdAndNendoAndNewFlgAndDelFlg(JICHITAI_CD, NENDO, "1", "0"))
                .thenReturn(List.of());
        when(kofuRitsuRepository.findKofuRitsuEntityByJichitaiCd(eq(JICHITAI_CD), any(Integer.class)))
                .thenReturn(List.of());
        when(shunoRirekiRepository.sumNonyugakuByShiteiNoIn(eq(JICHITAI_CD), anyList()))
                .thenReturn(List.of());

        ShoreikinBulkDto result = service.executeBulkSanshutsu(dto);

        assertThat(result.getTargetCount()).isEqualTo(0);
        assertThat(result.isExecuted()).isTrue();
    }

    @Test
    void executeBulkSanshutsu_newRecord_savesAndCountsSuccess() {
        Fuka fuka = new Fuka();
        fuka.setShiteiNo("00000001");
        fuka.setKibetsu(1);
        fuka.setRno(1);
        fuka.setTotalZeigaku(10000L);
        fuka.setShinkokuYmd(java.time.LocalDate.of(2024, 1, 1));

        when(fukaRepository.findByJichitaiCdAndNendoAndNewFlgAndDelFlg(JICHITAI_CD, NENDO, "1", "0"))
                .thenReturn(List.of(fuka));
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoAndDelFlgAndNewFlg(
                JICHITAI_CD, "00000001", NENDO, "0", "1"))
                .thenReturn(List.of(fuka));
        when(kofuRitsuRepository.findKofuRitsuEntityByJichitaiCd(eq(JICHITAI_CD), any(Integer.class)))
                .thenReturn(List.of());
        // 納付済み: 納入額合計 >= totalZeigaku
        when(shunoRirekiRepository.sumNonyugakuByShiteiNoIn(eq(JICHITAI_CD), anyList()))
                .thenReturn(List.<Object[]>of(new Object[]{"00000001", NENDO, 1, 10000L}));
        when(shoreikinRepository.findById(any())).thenReturn(Optional.empty());
        when(shoreikinRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShoreikinBulkDto dto = new ShoreikinBulkDto();
        dto.setNendo(NENDO);
        dto.setKofuRitsu(BigDecimal.valueOf(10));
        dto.setIncludeCalculated(false);

        ShoreikinBulkDto result = service.executeBulkSanshutsu(dto);

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailureCount()).isEqualTo(0);
    }

    @Test
    void executeBulkSanshutsu_existingRecordSkipped_whenNotIncludeCalculated() {
        Fuka fuka = new Fuka();
        fuka.setShiteiNo("00000001");
        fuka.setKibetsu(1);
        fuka.setRno(1);
        fuka.setTotalZeigaku(10000L);

        when(fukaRepository.findByJichitaiCdAndNendoAndNewFlgAndDelFlg(JICHITAI_CD, NENDO, "1", "0"))
                .thenReturn(List.of(fuka));
        when(kofuRitsuRepository.findKofuRitsuEntityByJichitaiCd(eq(JICHITAI_CD), any(Integer.class)))
                .thenReturn(List.of());
        when(shunoRirekiRepository.sumNonyugakuByShiteiNoIn(eq(JICHITAI_CD), anyList()))
                .thenReturn(List.of());

        Shoreikin existing = new Shoreikin();
        existing.setShiteiNo("00000001");
        when(shoreikinRepository.findById(any())).thenReturn(Optional.of(existing));

        ShoreikinBulkDto dto = new ShoreikinBulkDto();
        dto.setNendo(NENDO);
        dto.setKofuRitsu(BigDecimal.valueOf(10));
        dto.setIncludeCalculated(false);

        ShoreikinBulkDto result = service.executeBulkSanshutsu(dto);

        assertThat(result.getSkipCount()).isEqualTo(1);
        assertThat(result.getSuccessCount()).isEqualTo(0);
    }
}

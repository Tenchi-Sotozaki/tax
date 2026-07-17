package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.FurikomiKozaDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.FurikomiKoza;
import jp.lg.asp.accommodation.entity.FurikomiKozaId;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.FurikomiKozaRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.FurikomiKozaServiceImpl;

@ExtendWith(MockitoExtension.class)
class FurikomiKozaServiceImplTest {

    @Mock FurikomiKozaRepository furikomiKozaRepository;
    @Mock TokugimuRepository tokugimuRepository;
    @Mock AtenaRepository atenaRepository;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks FurikomiKozaServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "00000001";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    @Test
    void getFurikomiKoza_existingRecord_returnsViewMode() {
        Tokugimu tokugimu = new Tokugimu();
        tokugimu.setShisetsuName("テスト施設");
        tokugimu.setAtenaNo(BigDecimal.ONE);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.of(tokugimu));

        Atena atena = new Atena();
        atena.setName("テスト事業者");
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.of(atena));

        FurikomiKoza koza = new FurikomiKoza();
        koza.setBankCd("0001");
        koza.setVersion(1);
        when(furikomiKozaRepository.findById(new FurikomiKozaId(JICHITAI_CD, SHITEI_NO)))
                .thenReturn(Optional.of(koza));

        FurikomiKozaDto result = service.getFurikomiKoza(SHITEI_NO);

        assertThat(result.getMode()).isEqualTo("view");
        assertThat(result.isExists()).isTrue();
        assertThat(result.getBankCd()).isEqualTo("0001");
        assertThat(result.getName()).isEqualTo("テスト事業者");
    }

    @Test
    void getFurikomiKoza_noRecord_returnsCreateMode() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(furikomiKozaRepository.findById(any())).thenReturn(Optional.empty());

        FurikomiKozaDto result = service.getFurikomiKoza(SHITEI_NO);

        assertThat(result.getMode()).isEqualTo("create");
        assertThat(result.isExists()).isFalse();
    }

    @Test
    void createFurikomiKoza_savesAndReturnsViewMode() {
        FurikomiKozaDto dto = new FurikomiKozaDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setBankCd("0001");
        dto.setBankName("テスト銀行");
        when(furikomiKozaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FurikomiKozaDto result = service.createFurikomiKoza(dto);

        assertThat(result.getMode()).isEqualTo("view");
        assertThat(result.isExists()).isTrue();
        assertThat(result.getVersion()).isEqualTo(1);
        verify(furikomiKozaRepository).save(any());
    }

    @Test
    void updateFurikomiKoza_versionMismatch_throwsException() {
        FurikomiKozaDto dto = new FurikomiKozaDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setVersion(1);

        FurikomiKoza existing = new FurikomiKoza();
        existing.setVersion(2);
        when(furikomiKozaRepository.findById(any())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.updateFurikomiKoza(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("他のユーザー");
    }

    @Test
    void updateFurikomiKoza_notFound_throwsException() {
        FurikomiKozaDto dto = new FurikomiKozaDto();
        dto.setShiteiNo(SHITEI_NO);
        when(furikomiKozaRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateFurikomiKoza(dto))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void updateFurikomiKoza_success_updatesFields() {
        FurikomiKozaDto dto = new FurikomiKozaDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setVersion(1);
        dto.setBankCd("0002");
        dto.setBankName("新銀行");

        FurikomiKoza existing = new FurikomiKoza();
        existing.setVersion(1);
        when(furikomiKozaRepository.findById(any())).thenReturn(Optional.of(existing));
        when(furikomiKozaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FurikomiKozaDto result = service.updateFurikomiKoza(dto);

        assertThat(result.getMode()).isEqualTo("view");
        assertThat(existing.getBankCd()).isEqualTo("0002");
    }
}

package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import jp.lg.asp.accommodation.dto.ShunoDto;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.ShunoRenkeiServiceImpl;

@ExtendWith(MockitoExtension.class)
class ShunoRenkeiServiceImplTest {

    @Mock EntityManager em;
    @Mock FukaRepository fukaRepository;
    @Mock TokugimuRepository tokugimuRepository;
    @InjectMocks ShunoRenkeiServiceImpl service;

    private static final String JICHITAI_CD = "011002";

    @Test
    void findByKeys_emptyKeys_returnsEmptyList() {
        List<ShunoDto> result = service.findByKeys(JICHITAI_CD, List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void findByKeys_fukaNotFound_skipsEntry() {
        ShunoDto.Key key = new ShunoDto.Key();
        key.setShiteiNo("00000001");
        key.setNendo("2024");
        key.setKibetsu(1);

        when(fukaRepository.findLatestByNendoAndKibetsu(JICHITAI_CD, "00000001", "2024", 1))
                .thenReturn(List.of());

        List<ShunoDto> result = service.findByKeys(JICHITAI_CD, List.of(key));

        assertThat(result).isEmpty();
    }

    @Test
    void findByKeys_fukaFound_tokugimuNotFound_skipsEntry() {
        ShunoDto.Key key = new ShunoDto.Key();
        key.setShiteiNo("00000001");
        key.setNendo("2024");
        key.setKibetsu(1);

        Fuka fuka = new Fuka();
        fuka.setShiteiNo("00000001");
        fuka.setJichitaiCd(JICHITAI_CD);
        fuka.setTaishoYm("202403");
        when(fukaRepository.findLatestByNendoAndKibetsu(JICHITAI_CD, "00000001", "2024", 1))
                .thenReturn(List.of(fuka));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "00000001"))
                .thenReturn(List.of());

        List<ShunoDto> result = service.findByKeys(JICHITAI_CD, List.of(key));

        assertThat(result).isEmpty();
    }

    @Test
    void findByKeys_fukaAndTokugimuFound_returnsMappedDto() {
        ShunoDto.Key key = new ShunoDto.Key();
        key.setShiteiNo("00000001");
        key.setNendo("2024");
        key.setKibetsu(1);

        Fuka fuka = new Fuka();
        fuka.setShiteiNo("00000001");
        fuka.setJichitaiCd(JICHITAI_CD);
        fuka.setTaishoYm("202403");
        fuka.setTotalZeigaku(10000L);
        when(fukaRepository.findLatestByNendoAndKibetsu(JICHITAI_CD, "00000001", "2024", 1))
                .thenReturn(List.of(fuka));

        Tokugimu tokugimu = new Tokugimu();
        tokugimu.setShiteiNo("00000001");
        tokugimu.setJichitaiCd(JICHITAI_CD);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "00000001"))
                .thenReturn(List.of(tokugimu));

        List<ShunoDto> result = service.findByKeys(JICHITAI_CD, List.of(key));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getShiteiNo()).isEqualTo("00000001");
        assertThat(result.get(0).getTotalZeigaku()).isEqualTo(10000L);
        assertThat(result.get(0).getTaishoYm()).isEqualTo("2024-03");
    }
}

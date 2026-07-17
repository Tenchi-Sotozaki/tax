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

import jakarta.persistence.EntityManager;
import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.GassanForm;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Gassan;
import jp.lg.asp.accommodation.entity.GassanUchi;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.GassanRepository;
import jp.lg.asp.accommodation.repository.GassanUchiRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.GassanServiceImpl;

@ExtendWith(MockitoExtension.class)
class GassanServiceImplTest {

    @Mock GassanRepository gassanRepository;
    @Mock GassanUchiRepository gassanUchiRepository;
    @Mock AtenaRepository atenaRepository;
    @Mock TokugimuRepository tokugimuRepository;
    @Mock JichitaiRepository jichitaiRepository;
    @Mock EntityManager entityManager;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks GassanServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String GASSAN_SHITEI_NO = "90000001";
    private static final String SHITEI_NO = "00000001";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    private Gassan buildGassan() {
        Gassan g = new Gassan();
        g.setGassanShiteiNo(GASSAN_SHITEI_NO);
        g.setAtenaNo(BigDecimal.ONE);
        g.setRno(BigDecimal.ONE);
        g.setTorokuYmd(LocalDate.now());
        g.setShinkokuYmd(LocalDate.now());
        return g;
    }

    private Tokugimu buildTokugimu(String shiteiNo) {
        Tokugimu t = new Tokugimu();
        t.setShiteiNo(shiteiNo);
        t.setAtenaNo(BigDecimal.ONE);
        t.setShisetsuName("テスト施設");
        t.setKyokaName("テスト事業者");
        return t;
    }

    @Test
    void getByGassanShiteiNo_found() {
        Gassan gassan = buildGassan();
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(gassan));
        when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of());
        when(tokugimuRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(List.of(buildTokugimu(SHITEI_NO)));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.of(new Atena()));
        when(gassanRepository.findMaxRnoByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(BigDecimal.ONE);
        when(gassanRepository.findMinRnoByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(BigDecimal.ONE);

        GassanForm form = service.getByGassanShiteiNo(GASSAN_SHITEI_NO);

        assertThat(form.getGassanShiteiNo()).isEqualTo(GASSAN_SHITEI_NO);
    }

    @Test
    void getByGassanShiteiNo_notFound_throwsException() {
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.getByGassanShiteiNo(GASSAN_SHITEI_NO))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void register_noDaihyoShiteiNo_throwsException() {
        GassanForm form = new GassanForm();
        form.setShiteiNoList(List.of());

        assertThatThrownBy(() -> service.register(form))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("代表施設");
    }

    @Test
    void register_alreadyAssigned_throwsException() {
        GassanForm form = new GassanForm();
        form.setShiteiNoList(List.of(SHITEI_NO));
        form.setDaihyoShiteiNo(SHITEI_NO);
        form.setAtenaNo(BigDecimal.ONE);

        GassanUchi existing = new GassanUchi();
        existing.setShiteiNo(SHITEI_NO);
        when(gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(JICHITAI_CD, List.of(SHITEI_NO)))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.register(form))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("既に合算申告に登録");
    }

    @Test
    void deleteByGassanShiteiNo_callsLogicalDelete() {
        service.deleteByGassanShiteiNo(GASSAN_SHITEI_NO);

        verify(gassanRepository).deleteLogicallyByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO);
    }

    @Test
    void validateNotAlreadyAssigned_emptyList_noException() {
        assertThatCode(() -> service.validateNotAlreadyAssigned(List.of()))
                .doesNotThrowAnyException();
    }

    @Test
    void validateNotAlreadyAssigned_duplicate_throwsException() {
        GassanUchi uchi = new GassanUchi();
        uchi.setShiteiNo(SHITEI_NO);
        when(gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(JICHITAI_CD, List.of(SHITEI_NO)))
                .thenReturn(List.of(uchi));

        assertThatThrownBy(() -> service.validateNotAlreadyAssigned(List.of(SHITEI_NO)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getFacilitiesByAtenaNo_returnsFacilityItems() {
        when(tokugimuRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(List.of(buildTokugimu(SHITEI_NO)));

        List<GassanForm.FacilityItem> items = service.getFacilitiesByAtenaNo(BigDecimal.ONE);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getShiteiNo()).isEqualTo(SHITEI_NO);
    }
}

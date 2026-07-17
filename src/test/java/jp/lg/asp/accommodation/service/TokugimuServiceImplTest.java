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
import org.springframework.data.domain.Page;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.TokugimuForm;
import jp.lg.asp.accommodation.dto.TokugimuSearchForm;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.GassanUchi;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.GassanRepository;
import jp.lg.asp.accommodation.repository.GassanUchiRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.KyodoJigyoshaRepository;
import jp.lg.asp.accommodation.repository.ShoyushaRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.TokugimuServiceImpl;

@ExtendWith(MockitoExtension.class)
class TokugimuServiceImplTest {

    @Mock TokugimuRepository tokugimuRepository;
    @Mock AtenaRepository atenaRepository;
    @Mock GassanRepository gassanRepository;
    @Mock GassanUchiRepository gassanUchiRepository;
    @Mock ShoyushaRepository shoyushaRepository;
    @Mock KyodoJigyoshaRepository kyodoJigyoshaRepository;
    @Mock JichitaiRepository jichitaiRepository;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks TokugimuServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "00000001";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    private Tokugimu buildTokugimu(String shiteiNo) {
        Tokugimu t = new Tokugimu();
        t.setShiteiNo(shiteiNo);
        t.setAtenaNo(BigDecimal.ONE);
        t.setShisetsuName("テスト施設");
        t.setKyokaName("テスト事業者");
        t.setRno(BigDecimal.ONE);
        return t;
    }

    private Atena buildAtena() {
        Atena a = new Atena();
        a.setAtenaNo(BigDecimal.ONE);
        a.setName("テスト事業者");
        return a;
    }

    @Test
    void search_emptyForm_returnsAllItems() {
        TokugimuSearchForm form = new TokugimuSearchForm();
        form.setPage(0);
        form.setPageSize(10);

        Tokugimu t = buildTokugimu(SHITEI_NO);
        when(tokugimuRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of(t));
        when(atenaRepository.findByJichitaiCdAndAtenaNoIn(eq(JICHITAI_CD), any())).thenReturn(List.of(buildAtena()));
        when(gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(eq(JICHITAI_CD), any())).thenReturn(List.of());

        Page<?> result = service.search(form);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void search_emptyResult_returnsEmptyPage() {
        TokugimuSearchForm form = new TokugimuSearchForm();
        form.setPage(0);
        form.setPageSize(10);
        when(tokugimuRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of());

        Page<?> result = service.search(form);

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void getTokugimuByShiteiNo_found() {
        Tokugimu t = buildTokugimu(SHITEI_NO);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of(t));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE)).thenReturn(Optional.of(buildAtena()));
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(Optional.of(1));
        when(tokugimuRepository.findMinRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(Optional.of(1));
        when(shoyushaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of());
        when(kyodoJigyoshaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of());

        TokugimuForm form = service.getTokugimuByShiteiNo(SHITEI_NO);

        assertThat(form.getShiteiNo()).isEqualTo(SHITEI_NO);
    }

    @Test
    void getTokugimuByShiteiNo_notFound_throwsException() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of());

        assertThatThrownBy(() -> service.getTokugimuByShiteiNo(SHITEI_NO))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void register_noAtenaNo_throwsException() {
        TokugimuForm form = new TokugimuForm();
        form.setAtenaNo(null);

        assertThatThrownBy(() -> service.register(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("宛名番号");
    }

    @Test
    void deleteByShiteiNo_setsDelFlg1() {
        Tokugimu t = buildTokugimu(SHITEI_NO);
        t.setDelFlg("0");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of(t));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deleteByShiteiNo(SHITEI_NO);

        assertThat(t.getDelFlg()).isEqualTo("1");
    }

    @Test
    void deleteByShiteiNo_notFound_throwsException() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of());

        assertThatThrownBy(() -> service.deleteByShiteiNo(SHITEI_NO))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getShiteiNoById_found() {
        Tokugimu t = buildTokugimu(SHITEI_NO);
        when(tokugimuRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE)).thenReturn(List.of(t));

        assertThat(service.getShiteiNoById(1L)).isEqualTo(SHITEI_NO);
    }

    @Test
    void getShiteiNoById_notFound_throwsException() {
        when(tokugimuRepository.findByJichitaiCdAndAtenaNo(any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.getShiteiNoById(99L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getTokugimuName_validId_returnsName() {
        Atena atena = buildAtena();
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE)).thenReturn(Optional.of(atena));

        assertThat(service.getTokugimuName("1")).isEqualTo("テスト事業者");
    }

    @Test
    void getTokugimuName_invalidId_returns不明() {
        assertThat(service.getTokugimuName("not-a-number")).isEqualTo("不明");
    }
}

package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.GassanDaichoItem;
import jp.lg.asp.accommodation.dto.GassanDaichoSearchForm;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Gassan;
import jp.lg.asp.accommodation.entity.GassanUchi;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.GassanRepository;
import jp.lg.asp.accommodation.repository.GassanUchiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GassanDaichoServiceImplTest {

    @Mock private GassanRepository gassanRepository;
    @Mock private TokugimuRepository tokugimuRepository;
    @Mock private AtenaRepository atenaRepository;
    @Mock private GassanUchiRepository gassanUchiRepository;
    @Mock private JichitaiContext jichitaiContext;

    @InjectMocks
    private GassanDaichoServiceImpl service;

    private static final String JICHITAI_CD = "011002";

    private Gassan createGassan(String gassanShiteiNo, BigDecimal rno) {
        Gassan g = new Gassan();
        g.setJichitaiCd(JICHITAI_CD);
        g.setGassanShiteiNo(gassanShiteiNo);
        g.setRno(rno);
        g.setAtenaNo(BigDecimal.valueOf(1001));
        g.setShiteiNo("S001");
        return g;
    }

    private GassanUchi createGassanUchi(String gassanShiteiNo, String shiteiNo, BigDecimal rno) {
        GassanUchi u = new GassanUchi();
        u.setJichitaiCd(JICHITAI_CD);
        u.setGassanShiteiNo(gassanShiteiNo);
        u.setShiteiNo(shiteiNo);
        u.setRno(rno);
        return u;
    }

    private Tokugimu createTokugimu(String shiteiNo) {
        Tokugimu t = new Tokugimu();
        t.setShiteiNo(shiteiNo);
        t.setShisetsuName("施設名");
        t.setAtenaNo(BigDecimal.valueOf(1001));
        return t;
    }

    private Atena createAtena(String name) {
        Atena a = new Atena();
        a.setAtenaNo(BigDecimal.valueOf(1001));
        a.setName(name);
        return a;
    }

    private GassanDaichoSearchForm emptyForm() {
        return new GassanDaichoSearchForm();
    }

    @Test
    void search_条件なしで全件返す() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        Gassan g = createGassan("G001", BigDecimal.ONE);
        when(gassanRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of(g));
        when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, "G001"))
                .thenReturn(List.of(createGassanUchi("G001", "S001", BigDecimal.ONE)));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "S001"))
                .thenReturn(List.of(createTokugimu("S001")));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.valueOf(1001)))
                .thenReturn(Optional.of(createAtena("テスト太郎")));

        Page<GassanDaichoItem> result = service.search(emptyForm());

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getGassanShiteiNo()).isEqualTo("G001");
    }

    @Test
    void search_合算指定番号フィルタで絞り込まれる() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        Gassan g1 = createGassan("G001", BigDecimal.ONE);
        Gassan g2 = createGassan("G002", BigDecimal.ONE);
        when(gassanRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of(g1, g2));
        when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, "G001"))
                .thenReturn(List.of(createGassanUchi("G001", "S001", BigDecimal.ONE)));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "S001"))
                .thenReturn(List.of(createTokugimu("S001")));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(any(), any())).thenReturn(Optional.empty());

        GassanDaichoSearchForm form = emptyForm();
        form.setGassanShiteiNo("G001");

        Page<GassanDaichoItem> result = service.search(form);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getGassanShiteiNo()).isEqualTo("G001");
    }

    @Test
    void search_指定番号フィルタで絞り込まれる() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        Gassan g1 = createGassan("G001", BigDecimal.ONE);
        Gassan g2 = createGassan("G002", BigDecimal.ONE);
        when(gassanRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of(g1, g2));
        GassanUchi uchi = createGassanUchi("G001", "S001", BigDecimal.ONE);
        when(gassanUchiRepository.findByJichitaiCdAndShiteiNoOrGassanShiteiNo(JICHITAI_CD, "S001", null))
        .thenReturn(List.of(uchi));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "S001"))
                .thenReturn(List.of(createTokugimu("S001")));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(any(), any())).thenReturn(Optional.empty());

        GassanDaichoSearchForm form = emptyForm();
        form.setShiteiNo("S001");

        Page<GassanDaichoItem> result = service.search(form);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void search_名前フィルタprefix一致で絞り込まれる() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        Gassan g = createGassan("G001", BigDecimal.ONE);
        when(gassanRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of(g));
        when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, "G001"))
                .thenReturn(List.of(createGassanUchi("G001", "S001", BigDecimal.ONE)));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "S001"))
                .thenReturn(List.of(createTokugimu("S001")));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.valueOf(1001)))
                .thenReturn(Optional.of(createAtena("テスト太郎")));

        GassanDaichoSearchForm form = emptyForm();
        form.setName("テスト");
        form.setNameMatchType("prefix");

        Page<GassanDaichoItem> result = service.search(form);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void search_名前フィルタpartial一致で絞り込まれる() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        Gassan g = createGassan("G001", BigDecimal.ONE);
        when(gassanRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of(g));
        when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, "G001"))
                .thenReturn(List.of(createGassanUchi("G001", "S001", BigDecimal.ONE)));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "S001"))
                .thenReturn(List.of(createTokugimu("S001")));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.valueOf(1001)))
                .thenReturn(Optional.of(createAtena("テスト太郎")));

        GassanDaichoSearchForm form = emptyForm();
        form.setName("太郎");
        form.setNameMatchType("partial");

        Page<GassanDaichoItem> result = service.search(form);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void search_名前フィルタ一致なしは空を返す() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        Gassan g = createGassan("G001", BigDecimal.ONE);
        when(gassanRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of(g));
        when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, "G001"))
                .thenReturn(List.of(createGassanUchi("G001", "S001", BigDecimal.ONE)));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "S001"))
                .thenReturn(List.of(createTokugimu("S001")));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.valueOf(1001)))
                .thenReturn(Optional.of(createAtena("テスト太郎")));

        GassanDaichoSearchForm form = emptyForm();
        form.setName("存在しない名前");
        form.setNameMatchType("partial");

        Page<GassanDaichoItem> result = service.search(form);

        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void search_ページングが正しく動作する() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        List<Gassan> gassanList = List.of(
                createGassan("G001", BigDecimal.ONE),
                createGassan("G002", BigDecimal.ONE),
                createGassan("G003", BigDecimal.ONE));
        when(gassanRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(gassanList);
        for (String no : List.of("G001", "G002", "G003")) {
            when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, no))
                    .thenReturn(Collections.emptyList());
        }

        GassanDaichoSearchForm form = emptyForm();
        form.setPage(0);
        form.setPageSize(2);

        Page<GassanDaichoItem> result = service.search(form);

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void search_GassanUchiが空の場合はGassanから直接取得() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        Gassan g = createGassan("G001", BigDecimal.ONE);
        when(gassanRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of(g));
        when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, "G001"))
                .thenReturn(Collections.emptyList());
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "S001"))
                .thenReturn(List.of(createTokugimu("S001")));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.valueOf(1001)))
                .thenReturn(Optional.of(createAtena("テスト太郎")));

        Page<GassanDaichoItem> result = service.search(emptyForm());

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getGassanShiteiNo()).isEqualTo("G001");
    }

    @Test
    void search_データなしは空ページを返す() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(gassanRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(Collections.emptyList());

        Page<GassanDaichoItem> result = service.search(emptyForm());

        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void getByGassanShiteiNo_存在する場合はアイテムを返す() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        Gassan g = createGassan("G001", BigDecimal.ONE);
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, "G001")).thenReturn(List.of(g));
        when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, "G001"))
                .thenReturn(List.of(createGassanUchi("G001", "S001", BigDecimal.ONE)));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "S001"))
                .thenReturn(List.of(createTokugimu("S001")));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.valueOf(1001)))
                .thenReturn(Optional.of(createAtena("テスト太郎")));

        GassanDaichoItem result = service.getByGassanShiteiNo("G001");

        assertThat(result).isNotNull();
        assertThat(result.getGassanShiteiNo()).isEqualTo("G001");
        assertThat(result.getName()).isEqualTo("テスト太郎");
    }

    @Test
    void getByGassanShiteiNo_存在しない場合はnullを返す() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, "G999"))
                .thenReturn(Collections.emptyList());

        GassanDaichoItem result = service.getByGassanShiteiNo("G999");

        assertThat(result).isNull();
    }

    @Test
    void getByGassanShiteiNo_rno1がない場合は先頭レコードを代表とする() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        Gassan g = createGassan("G001", BigDecimal.valueOf(2));
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, "G001")).thenReturn(List.of(g));
        when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, "G001"))
                .thenReturn(Collections.emptyList());
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "S001"))
                .thenReturn(List.of(createTokugimu("S001")));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(any(), any())).thenReturn(Optional.empty());

        GassanDaichoItem result = service.getByGassanShiteiNo("G001");

        assertThat(result).isNotNull();
        assertThat(result.getGassanShiteiNo()).isEqualTo("G001");
    }
}

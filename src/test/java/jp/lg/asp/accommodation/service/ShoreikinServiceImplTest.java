package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.ShoreikinDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Gassan;
import jp.lg.asp.accommodation.entity.GassanUchi;
import jp.lg.asp.accommodation.entity.Shoreikin;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.GassanRepository;
import jp.lg.asp.accommodation.repository.GassanUchiRepository;
import jp.lg.asp.accommodation.repository.ShoreikinRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.ShoreikinServiceImpl;

@ExtendWith(MockitoExtension.class)
class ShoreikinServiceImplTest {

    @Mock ShoreikinRepository shoreikinRepository;
    @Mock TokugimuRepository tokugimuRepository;
    @Mock AtenaRepository atenaRepository;
    @Mock GassanRepository gassanRepository;
    @Mock GassanUchiRepository gassanUchiRepository;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks ShoreikinServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "00100001";
    private static final String NENDO = "2024";

    private Tokugimu buildTokugimu(String shiteiNo) {
        Tokugimu t = new Tokugimu();
        t.setShiteiNo(shiteiNo);
        t.setAtenaNo(BigDecimal.valueOf(1001));
        t.setShisetsuName("テスト施設");
        return t;
    }

    private Atena buildAtena() {
        Atena a = new Atena();
        a.setAtenaNo(BigDecimal.valueOf(1001));
        a.setName("テスト太郎");
        return a;
    }

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    @Test
    void search_奨励金あり_正常取得() {
        ShoreikinDto form = new ShoreikinDto();
        form.setNendo(NENDO);

        when(tokugimuRepository.findBySearchConditions(eq(JICHITAI_CD), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(buildTokugimu(SHITEI_NO)));
        when(atenaRepository.findByJichitaiCdAndAtenaNoIn(eq(JICHITAI_CD), any()))
                .thenReturn(List.of(buildAtena()));
        when(gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(eq(JICHITAI_CD), any()))
                .thenReturn(List.of());

        Shoreikin shoreikin = new Shoreikin();
        shoreikin.setShiteiNo(SHITEI_NO);
        shoreikin.setNendo(NENDO);
        shoreikin.setKofuGaku(100000L);
        shoreikin.setKofuYmd(LocalDate.of(2024, 6, 1));
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoInAndNendo(eq(JICHITAI_CD), any(), eq(NENDO)))
                .thenReturn(List.of(shoreikin));

        Page<ShoreikinDto> result = service.search(form);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getShimei()).isEqualTo("テスト太郎");
        assertThat(result.getContent().get(0).getKofuGaku()).isEqualTo(100000L);
    }

    @Test
    void search_特別徴収義務者なしは空ページ() {
        ShoreikinDto form = new ShoreikinDto();
        form.setNendo(NENDO);

        when(tokugimuRepository.findBySearchConditions(eq(JICHITAI_CD), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        Page<ShoreikinDto> result = service.search(form);

        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void search_奨励金なし_算出無フィルタ() {
        ShoreikinDto form = new ShoreikinDto();
        form.setNendo(NENDO);
        form.setKofuSanshutsuUmu("2"); // 算出無のみ

        when(tokugimuRepository.findBySearchConditions(eq(JICHITAI_CD), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(buildTokugimu(SHITEI_NO)));
        when(atenaRepository.findByJichitaiCdAndAtenaNoIn(eq(JICHITAI_CD), any()))
                .thenReturn(List.of(buildAtena()));
        when(gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(eq(JICHITAI_CD), any()))
                .thenReturn(List.of());
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoInAndNendo(eq(JICHITAI_CD), any(), eq(NENDO)))
                .thenReturn(List.of()); // 奨励金なし

        Page<ShoreikinDto> result = service.search(form);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getKofuGaku()).isNull();
    }

    @Test
    void search_合算指定番号で検索() {
        ShoreikinDto form = new ShoreikinDto();
        form.setNendo(NENDO);
        form.setGassanShiteiNo("90000001");

        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(eq(JICHITAI_CD), eq("90000001")))
                .thenReturn(List.of(new Gassan()));
        GassanUchi gassanUchi = new GassanUchi();
        gassanUchi.setShiteiNo(SHITEI_NO);
        when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(eq(JICHITAI_CD), eq("90000001")))
                .thenReturn(List.of(gassanUchi));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(eq(JICHITAI_CD), eq(SHITEI_NO)))
                .thenReturn(List.of(buildTokugimu(SHITEI_NO)));
        when(atenaRepository.findByJichitaiCdAndAtenaNoIn(eq(JICHITAI_CD), any()))
                .thenReturn(List.of(buildAtena()));
        when(gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(eq(JICHITAI_CD), any()))
                .thenReturn(List.of());
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoInAndNendo(eq(JICHITAI_CD), any(), any()))
                .thenReturn(List.of());

        Page<ShoreikinDto> result = service.search(form);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void search_ページング() {
        ShoreikinDto form = new ShoreikinDto();
        form.setNendo(NENDO);
        form.setPage(0);
        form.setPageSize(2);

        List<Tokugimu> tokugimuList = List.of(
                buildTokugimu("00100001"),
                buildTokugimu("00100002"),
                buildTokugimu("00100003"));
        when(tokugimuRepository.findBySearchConditions(eq(JICHITAI_CD), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(tokugimuList);
        when(atenaRepository.findByJichitaiCdAndAtenaNoIn(eq(JICHITAI_CD), any()))
                .thenReturn(List.of(buildAtena()));
        when(gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(eq(JICHITAI_CD), any()))
                .thenReturn(List.of());
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoInAndNendo(eq(JICHITAI_CD), any(), eq(NENDO)))
                .thenReturn(List.of());

        Page<ShoreikinDto> result = service.search(form);

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(2);
    }
}

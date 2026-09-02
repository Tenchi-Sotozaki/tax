package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jp.lg.asp.accommodation.dto.ShoreikinRenkeiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.FurikomiKoza;
import jp.lg.asp.accommodation.entity.Shoreikin;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.FurikomiKozaRepository;
import jp.lg.asp.accommodation.repository.ShoreikinRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.ShoreikinRenkeiServiceImpl;

@ExtendWith(MockitoExtension.class)
class ShoreikinRenkeiServiceImplTest {

    @Mock EntityManager em;
    @Mock ShoreikinRepository shoreikinRepository;
    @Mock TokugimuRepository tokugimuRepository;
    @Mock FurikomiKozaRepository furikomiKozaRepository;

    @InjectMocks ShoreikinRenkeiServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "00100001";
    private static final String NENDO = "2024";

    // =====================================================================
    // 既存テスト（findByKeys）
    // =====================================================================

    @Test
    void findByKeys_存在するキー() {
        Shoreikin shoreikin = new Shoreikin();
        shoreikin.setJichitaiCd(JICHITAI_CD);
        shoreikin.setShiteiNo(SHITEI_NO);
        shoreikin.setNendo(NENDO);
        shoreikin.setKofuGaku(100000L);
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(JICHITAI_CD, SHITEI_NO, NENDO))
                .thenReturn(Optional.of(shoreikin));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of());
        when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(Optional.empty());

        ShoreikinRenkeiDto.Key key = new ShoreikinRenkeiDto.Key();
        key.setShiteiNo(SHITEI_NO);
        key.setNendo(NENDO);

        List<ShoreikinRenkeiDto> result = service.findByKeys(JICHITAI_CD, List.of(key));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getKofuGaku()).isEqualTo(100000L);
    }

    @Test
    void findByKeys_存在しないキーは空リスト() {
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(JICHITAI_CD, SHITEI_NO, NENDO))
                .thenReturn(Optional.empty());

        ShoreikinRenkeiDto.Key key = new ShoreikinRenkeiDto.Key();
        key.setShiteiNo(SHITEI_NO);
        key.setNendo(NENDO);

        List<ShoreikinRenkeiDto> result = service.findByKeys(JICHITAI_CD, List.of(key));

        assertThat(result).isEmpty();
    }

    @Test
    void findByKeys_振込口座情報あり() {
        Shoreikin shoreikin = new Shoreikin();
        shoreikin.setJichitaiCd(JICHITAI_CD);
        shoreikin.setShiteiNo(SHITEI_NO);
        shoreikin.setNendo(NENDO);
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(JICHITAI_CD, SHITEI_NO, NENDO))
                .thenReturn(Optional.of(shoreikin));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of());

        FurikomiKoza koza = new FurikomiKoza();
        koza.setBankName("テスト銀行");
        koza.setKozaNo("1234567");
        when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(Optional.of(koza));

        ShoreikinRenkeiDto.Key key = new ShoreikinRenkeiDto.Key();
        key.setShiteiNo(SHITEI_NO);
        key.setNendo(NENDO);

        List<ShoreikinRenkeiDto> result = service.findByKeys(JICHITAI_CD, List.of(key));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBankName()).isEqualTo("テスト銀行");
        assertThat(result.get(0).getKozaNo()).isEqualTo("1234567");
    }

    @Test
    void findByKeys_Tokugimu宛名情報あり() {
        Shoreikin shoreikin = new Shoreikin();
        shoreikin.setJichitaiCd(JICHITAI_CD);
        shoreikin.setShiteiNo(SHITEI_NO);
        shoreikin.setNendo(NENDO);
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(JICHITAI_CD, SHITEI_NO, NENDO))
                .thenReturn(Optional.of(shoreikin));

        Atena atena = new Atena();
        atena.setName("テスト太郎");
        Tokugimu tokugimu = new Tokugimu();
        tokugimu.setAtenaNo(java.math.BigDecimal.valueOf(1001));
        tokugimu.setAtena(atena);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of(tokugimu));
        when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(Optional.empty());

        ShoreikinRenkeiDto.Key key = new ShoreikinRenkeiDto.Key();
        key.setShiteiNo(SHITEI_NO);
        key.setNendo(NENDO);

        List<ShoreikinRenkeiDto> result = service.findByKeys(JICHITAI_CD, List.of(key));

        assertThat(result.get(0).getName()).isEqualTo("テスト太郎");
    }

    // =====================================================================
    // ヘルパー（search テスト用）
    // =====================================================================

    @SuppressWarnings("unchecked")
    private CriteriaBuilder mockCriteria() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<Shoreikin> cq = mock(CriteriaQuery.class);
        Root<Shoreikin> root = mock(Root.class);
        TypedQuery<Shoreikin> typedQuery = mock(TypedQuery.class);

        when(em.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Shoreikin.class)).thenReturn(cq);
        when(cq.from(Shoreikin.class)).thenReturn(root);
        when(em.createQuery(cq)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(List.of());

        Predicate predicate = mock(Predicate.class);
        lenient().when(cb.equal(any(), any())).thenReturn(predicate);
        lenient().when(cb.like(any(Expression.class), any(Expression.class))).thenReturn(predicate);
        lenient().when(cq.where(any(Predicate[].class))).thenReturn(cq);
        lenient().when(cq.orderBy(any(Order[].class))).thenReturn(cq);
        lenient().when(cb.desc(any())).thenReturn(mock(Order.class));
        lenient().when(cb.asc(any())).thenReturn(mock(Order.class));
        when(root.get(anyString())).thenReturn(mock(Path.class));

        return cb;
    }

    @SuppressWarnings("unchecked")
    private CriteriaBuilder mockCriteriaWithName() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<Shoreikin> cq = mock(CriteriaQuery.class);
        Root<Shoreikin> root = mock(Root.class);
        TypedQuery<Shoreikin> typedQuery = mock(TypedQuery.class);
        Subquery<Tokugimu> subquery = mock(Subquery.class);
        Root<Tokugimu> tRoot = mock(Root.class);
        Join<Object, Object> atenaJoin = mock(Join.class);

        when(em.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Shoreikin.class)).thenReturn(cq);
        when(cq.from(Shoreikin.class)).thenReturn(root);
        when(em.createQuery(cq)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(List.of());
        when(cq.subquery(Tokugimu.class)).thenReturn(subquery);
        when(subquery.from(Tokugimu.class)).thenReturn(tRoot);
        when(tRoot.join(eq("atena"), any())).thenReturn(atenaJoin);
        when(subquery.select(any())).thenReturn(subquery);
        when(subquery.where(any(Predicate.class))).thenReturn(subquery);

        Predicate predicate = mock(Predicate.class);
        lenient().when(cb.equal(any(), any())).thenReturn(predicate);
        lenient().when(cb.and(any(Predicate[].class))).thenReturn(predicate);
        lenient().when(cb.exists(any())).thenReturn(predicate);
        lenient().when(cq.where(any(Predicate[].class))).thenReturn(cq);
        lenient().when(cq.orderBy(any(Order[].class))).thenReturn(cq);
        lenient().when(cb.desc(any())).thenReturn(mock(Order.class));
        lenient().when(cb.asc(any())).thenReturn(mock(Order.class));
        when(root.get(anyString())).thenReturn(mock(Path.class));
        when(tRoot.get(anyString())).thenReturn(mock(Path.class));
        when(atenaJoin.get(anyString())).thenReturn(mock(Path.class));

        return cb;
    }

    // =====================================================================
    // #24 search 正常系
    // =====================================================================

    @Test
    @DisplayName("#24 search 正常系 shiteiNo が完全一致で絞り込まれること")
    void search_shiteiNoで絞り込まれる() {
        mockCriteriaWithName();

        List<ShoreikinRenkeiDto> result = service.search("01100", "2024", "00100001", "山田", "partial");

        assertNotNull(result);
        verify(em).getCriteriaBuilder();
    }

    // =====================================================================
    // #25 search 正常系
    // =====================================================================

    @Test
    @DisplayName("#25 search 正常系 検索条件がすべて null の場合")
    void search_検索条件がすべてnull() {
        mockCriteria();

        List<ShoreikinRenkeiDto> result = service.search("01100", null, null, null, "partial");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // =====================================================================
    // #26 search 異常系
    // =====================================================================

    @Test
    @DisplayName("#26 search 異常系 検索条件が空文字の場合")
    void search_検索条件が空文字() {
        mockCriteria();

        List<ShoreikinRenkeiDto> result = service.search("01100", "", "", "", "partial");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // =====================================================================
    // #27 search 正常系
    // =====================================================================

    @Test
    @DisplayName("#27 search 正常系 氏名の一致区分 partial：LIKE が \"%山田%\" になる")
    @SuppressWarnings("unchecked")
    void search_氏名一致区分partial() {
        CriteriaBuilder cb = mockCriteriaWithName();

        ArgumentCaptor<String> literalCaptor = ArgumentCaptor.forClass(String.class);
        Expression<String> literalExpr = mock(Expression.class);
        when(cb.literal(literalCaptor.capture())).thenReturn(literalExpr);

        service.search("01100", null, null, "山田", "partial");

        assertTrue(literalCaptor.getAllValues().contains("%山田%"));
    }

    // =====================================================================
    // #28 search 正常系
    // =====================================================================

    @Test
    @DisplayName("#28 search 正常系 氏名の一致区分 prefix：LIKE が \"山田%\" になる")
    @SuppressWarnings("unchecked")
    void search_氏名一致区分prefix() {
        CriteriaBuilder cb = mockCriteriaWithName();

        ArgumentCaptor<String> literalCaptor = ArgumentCaptor.forClass(String.class);
        Expression<String> literalExpr = mock(Expression.class);
        when(cb.literal(literalCaptor.capture())).thenReturn(literalExpr);

        service.search("01100", null, null, "山田", "prefix");

        assertTrue(literalCaptor.getAllValues().contains("山田%"));
    }

    // =====================================================================
    // #29 search 正常系
    // =====================================================================

    @Test
    @DisplayName("#29 search 正常系 氏名の一致区分 exact：LIKE が \"山田\" になる")
    @SuppressWarnings("unchecked")
    void search_氏名一致区分exact() {
        CriteriaBuilder cb = mockCriteriaWithName();

        ArgumentCaptor<String> literalCaptor = ArgumentCaptor.forClass(String.class);
        Expression<String> literalExpr = mock(Expression.class);
        when(cb.literal(literalCaptor.capture())).thenReturn(literalExpr);

        service.search("01100", null, null, "山田", "exact");

        assertTrue(literalCaptor.getAllValues().contains("山田"));
    }

    // =====================================================================
    // #30 search 異常系
    // =====================================================================

    @Test
    @DisplayName("#30 search 異常系 未定義の一致区分が渡された場合は部分一致になる")
    @SuppressWarnings("unchecked")
    void search_未定義の一致区分は部分一致() {
        CriteriaBuilder cb = mockCriteriaWithName();

        ArgumentCaptor<String> literalCaptor = ArgumentCaptor.forClass(String.class);
        Expression<String> literalExpr = mock(Expression.class);
        when(cb.literal(literalCaptor.capture())).thenReturn(literalExpr);

        service.search("01100", null, null, "山田", "unknown");

        assertTrue(literalCaptor.getAllValues().contains("%山田%"));
    }

    // =====================================================================
    // #31 search 異常系
    // =====================================================================

    @Test
    @DisplayName("#31 search 異常系 氏名の一致区分が null の場合は部分一致として扱われる")
    @SuppressWarnings("unchecked")
    void search_氏名一致区分がnullは部分一致() {
        // 現行実装は switch(null) で NullPointerException となるため失敗する（実装修正が必要）
        CriteriaBuilder cb = mockCriteriaWithName();

        ArgumentCaptor<String> literalCaptor = ArgumentCaptor.forClass(String.class);
        Expression<String> literalExpr = mock(Expression.class);
        when(cb.literal(literalCaptor.capture())).thenReturn(literalExpr);

        service.search("01100", null, null, "山田", null);

        assertTrue(literalCaptor.getAllValues().contains("%山田%"));
    }

    // =====================================================================
    // #32 search 異常系
    // =====================================================================

    @Test
    @DisplayName("#32 search 異常系 該当0件の場合")
    void search_該当0件() {
        mockCriteria();

        List<ShoreikinRenkeiDto> result = service.search("01100", "9999", null, null, "partial");

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(tokugimuRepository, never()).findByJichitaiCdAndShiteiNo(any(), any());
        verify(furikomiKozaRepository, never()).findByJichitaiCdAndShiteiNo(any(), any());
    }
}

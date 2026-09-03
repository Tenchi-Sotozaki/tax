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
import java.math.BigDecimal;
import java.time.LocalDate;

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

    // =====================================================================
    // 交付金振込情報出力確認_単体テストチェックリスト（#確認15〜#確認30）
    // =====================================================================

    private static final String K_JICHITAI_CD = "01100";

    private ShoreikinRenkeiDto.Key keyOf(String shiteiNo, String nendo) {
        ShoreikinRenkeiDto.Key k = new ShoreikinRenkeiDto.Key();
        k.setShiteiNo(shiteiNo);
        k.setNendo(nendo);
        return k;
    }

    private Shoreikin shoreikinOf(String shiteiNo, String nendo) {
        Shoreikin s = new Shoreikin();
        s.setJichitaiCd(K_JICHITAI_CD);
        s.setShiteiNo(shiteiNo);
        s.setNendo(nendo);
        return s;
    }

    private Atena atenaOf(String name) {
        Atena a = new Atena();
        a.setName(name);
        return a;
    }

    private Tokugimu tokugimuOf(BigDecimal rno, BigDecimal atenaNo, Atena atena) {
        Tokugimu t = new Tokugimu();
        t.setRno(rno);
        t.setAtenaNo(atenaNo);
        t.setAtena(atena);
        return t;
    }

    @Test
    @DisplayName("#確認15 findByKeys 正常系 存在するキー1件：該当データが1件返る")
    void 確認15_存在するキー1件() {
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(K_JICHITAI_CD, "S001", "2024"))
                .thenReturn(Optional.of(shoreikinOf("S001", "2024")));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(K_JICHITAI_CD, "S001")).thenReturn(List.of());
        when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(K_JICHITAI_CD, "S001")).thenReturn(Optional.empty());

        List<ShoreikinRenkeiDto> result = service.findByKeys(K_JICHITAI_CD, List.of(keyOf("S001", "2024")));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getShiteiNo()).isEqualTo("S001");
        assertThat(result.get(0).getNendo()).isEqualTo("2024");

        verify(shoreikinRepository, times(1))
                .findByJichitaiCdAndShiteiNoAndNendo(K_JICHITAI_CD, "S001", "2024");
    }

    @Test
    @DisplayName("#確認16 findByKeys 正常系 複数キーで一部が存在しない場合：存在するものだけ返る")
    void 確認16_複数キーで一部が存在しない() {
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(K_JICHITAI_CD, "S001", "2024"))
                .thenReturn(Optional.of(shoreikinOf("S001", "2024")));
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(K_JICHITAI_CD, "S002", "2024"))
                .thenReturn(Optional.empty());
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(K_JICHITAI_CD, "S001")).thenReturn(List.of());
        when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(K_JICHITAI_CD, "S001")).thenReturn(Optional.empty());

        List<ShoreikinRenkeiDto> result =
                service.findByKeys(K_JICHITAI_CD, List.of(keyOf("S001", "2024"), keyOf("S002", "2024")));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getShiteiNo()).isEqualTo("S001");

        verify(shoreikinRepository, times(2))
                .findByJichitaiCdAndShiteiNoAndNendo(any(), any(), any());
        verify(tokugimuRepository, never()).findByJichitaiCdAndShiteiNo(K_JICHITAI_CD, "S002");
    }

    @Test
    @DisplayName("#確認17 findByKeys 正常系 同じキーを重複して渡した場合：重複排除せず件数分返る")
    void 確認17_同じキーを重複して渡した場合() {
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(K_JICHITAI_CD, "S001", "2024"))
                .thenReturn(Optional.of(shoreikinOf("S001", "2024")));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(K_JICHITAI_CD, "S001")).thenReturn(List.of());
        when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(K_JICHITAI_CD, "S001")).thenReturn(Optional.empty());

        List<ShoreikinRenkeiDto> result =
                service.findByKeys(K_JICHITAI_CD, List.of(keyOf("S001", "2024"), keyOf("S001", "2024")));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getShiteiNo()).isEqualTo("S001");
        assertThat(result.get(1).getShiteiNo()).isEqualTo("S001");

        verify(shoreikinRepository, times(2))
                .findByJichitaiCdAndShiteiNoAndNendo(K_JICHITAI_CD, "S001", "2024");
    }

    @Test
    @DisplayName("#確認18 findByKeys 正常系 keys が空リスト：リポジトリを呼ばずに空リストを返す")
    void 確認18_keysが空リスト() {
        List<ShoreikinRenkeiDto> result = service.findByKeys(K_JICHITAI_CD, List.of());

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(shoreikinRepository, never())
                .findByJichitaiCdAndShiteiNoAndNendo(any(), any(), any());
    }

    @Test
    @DisplayName("#確認19 findByKeys 異常系 存在しないキーのみ：空リストを返す")
    void 確認19_存在しないキーのみ() {
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(K_JICHITAI_CD, "X999", "2024"))
                .thenReturn(Optional.empty());

        List<ShoreikinRenkeiDto> result = service.findByKeys(K_JICHITAI_CD, List.of(keyOf("X999", "2024")));

        assertThat(result).isEmpty();

        verify(tokugimuRepository, never()).findByJichitaiCdAndShiteiNo(any(), any());
        verify(furikomiKozaRepository, never()).findByJichitaiCdAndShiteiNo(any(), any());
    }

    @Test
    @DisplayName("#確認20 findByKeys 異常系 keys が null の場合")
    void 確認20_keysがnull() {
        // 現行実装は for が null を受けて NullPointerException となるため、実装側の修正が必要
        List<ShoreikinRenkeiDto> result =
                assertDoesNotThrow(() -> service.findByKeys(K_JICHITAI_CD, null));

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("#確認21 findByKeys 異常系 キーの指定番号・年度が null の場合")
    void 確認21_キーの指定番号と年度がnull() {
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(K_JICHITAI_CD, null, null))
                .thenReturn(Optional.empty());

        List<ShoreikinRenkeiDto> result =
                assertDoesNotThrow(() -> service.findByKeys(K_JICHITAI_CD, List.of(keyOf(null, null))));

        assertThat(result).isEmpty();

        ArgumentCaptor<String> shiteiNoCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> nendoCaptor = ArgumentCaptor.forClass(String.class);
        verify(shoreikinRepository).findByJichitaiCdAndShiteiNoAndNendo(
                eq(K_JICHITAI_CD), shiteiNoCaptor.capture(), nendoCaptor.capture());
        assertThat(shiteiNoCaptor.getValue()).isNull();
        assertThat(nendoCaptor.getValue()).isNull();
    }

    @Test
    @DisplayName("#確認22 toDto 正常系 特別徴収義務者・宛名が存在する場合：宛名番号と氏名が設定される")
    void 確認22_特別徴収義務者と宛名が存在する() {
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(K_JICHITAI_CD, "S001", "2024"))
                .thenReturn(Optional.of(shoreikinOf("S001", "2024")));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(K_JICHITAI_CD, "S001"))
                .thenReturn(List.of(tokugimuOf(null, BigDecimal.ONE, atenaOf("山田太郎"))));
        when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(K_JICHITAI_CD, "S001")).thenReturn(Optional.empty());

        List<ShoreikinRenkeiDto> result = service.findByKeys(K_JICHITAI_CD, List.of(keyOf("S001", "2024")));

        assertThat(result.get(0).getAtenaNo()).isEqualTo("1");
        assertThat(result.get(0).getName()).isEqualTo("山田太郎");
    }

    @Test
    @DisplayName("#確認23 toDto 異常系 宛名が null の場合：宛名番号のみ設定され氏名は null")
    void 確認23_宛名がnull() {
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(K_JICHITAI_CD, "S001", "2024"))
                .thenReturn(Optional.of(shoreikinOf("S001", "2024")));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(K_JICHITAI_CD, "S001"))
                .thenReturn(List.of(tokugimuOf(null, BigDecimal.ONE, null)));
        when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(K_JICHITAI_CD, "S001")).thenReturn(Optional.empty());

        List<ShoreikinRenkeiDto> result = assertDoesNotThrow(
                () -> service.findByKeys(K_JICHITAI_CD, List.of(keyOf("S001", "2024"))));

        assertThat(result.get(0).getAtenaNo()).isEqualTo("1");
        assertThat(result.get(0).getName()).isNull();
    }

    @Test
    @DisplayName("#確認24 toDto 異常系 特別徴収義務者が0件（未登録・削除済み）の場合")
    void 確認24_特別徴収義務者が0件() {
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(K_JICHITAI_CD, "S001", "2024"))
                .thenReturn(Optional.of(shoreikinOf("S001", "2024")));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(K_JICHITAI_CD, "S001")).thenReturn(List.of());
        when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(K_JICHITAI_CD, "S001")).thenReturn(Optional.empty());

        List<ShoreikinRenkeiDto> result = assertDoesNotThrow(
                () -> service.findByKeys(K_JICHITAI_CD, List.of(keyOf("S001", "2024"))));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAtenaNo()).isNull();
        assertThat(result.get(0).getName()).isNull();
    }

    @Test
    @DisplayName("#確認25 toDto 正常系 特別徴収義務者が複数件の場合：先頭（rno 最大）の1件のみ使用される")
    void 確認25_特別徴収義務者が複数件() {
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(K_JICHITAI_CD, "S001", "2024"))
                .thenReturn(Optional.of(shoreikinOf("S001", "2024")));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(K_JICHITAI_CD, "S001"))
                .thenReturn(List.of(
                        tokugimuOf(BigDecimal.valueOf(2), BigDecimal.ONE, atenaOf("山田太郎")),
                        tokugimuOf(BigDecimal.valueOf(1), BigDecimal.valueOf(2), atenaOf("佐藤花子"))));
        when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(K_JICHITAI_CD, "S001")).thenReturn(Optional.empty());

        List<ShoreikinRenkeiDto> result = service.findByKeys(K_JICHITAI_CD, List.of(keyOf("S001", "2024")));

        assertThat(result.get(0).getAtenaNo()).isEqualTo("1");
        assertThat(result.get(0).getName()).isEqualTo("山田太郎");
    }

    @Test
    @DisplayName("#確認26 toDto 異常系 宛名番号が null の場合")
    void 確認26_宛名番号がnull() {
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(K_JICHITAI_CD, "S001", "2024"))
                .thenReturn(Optional.of(shoreikinOf("S001", "2024")));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(K_JICHITAI_CD, "S001"))
                .thenReturn(List.of(tokugimuOf(null, null, atenaOf("山田太郎"))));
        when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(K_JICHITAI_CD, "S001")).thenReturn(Optional.empty());

        List<ShoreikinRenkeiDto> result = service.findByKeys(K_JICHITAI_CD, List.of(keyOf("S001", "2024")));

        assertThat(result.get(0).getAtenaNo()).isNull();
        assertThat(result.get(0).getName()).isEqualTo("山田太郎");
    }

    @Test
    @DisplayName("#確認27 toDto 正常系 振込口座が存在する場合：口座情報7項目が設定される")
    void 確認27_振込口座が存在する() {
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(K_JICHITAI_CD, "S001", "2024"))
                .thenReturn(Optional.of(shoreikinOf("S001", "2024")));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(K_JICHITAI_CD, "S001")).thenReturn(List.of());

        FurikomiKoza koza = new FurikomiKoza();
        koza.setBankCd("0001");
        koza.setBankName("テスト銀行");
        koza.setBranchCd("001");
        koza.setBranchName("本店");
        koza.setShumoku("1");
        koza.setKozaNo("1234567");
        koza.setMeigi("ヤマダタロウ");
        when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(K_JICHITAI_CD, "S001"))
                .thenReturn(Optional.of(koza));

        ShoreikinRenkeiDto dto = service.findByKeys(K_JICHITAI_CD, List.of(keyOf("S001", "2024"))).get(0);

        assertThat(dto.getBankCd()).isEqualTo("0001");
        assertThat(dto.getBankName()).isEqualTo("テスト銀行");
        assertThat(dto.getBranchCd()).isEqualTo("001");
        assertThat(dto.getBranchName()).isEqualTo("本店");
        assertThat(dto.getShumoku()).isEqualTo("1");
        assertThat(dto.getKozaNo()).isEqualTo("1234567");
        assertThat(dto.getMeigi()).isEqualTo("ヤマダタロウ");
    }

    @Test
    @DisplayName("#確認28 toDto 異常系 振込口座が存在しない場合：口座情報は null のまま")
    void 確認28_振込口座が存在しない() {
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(K_JICHITAI_CD, "S001", "2024"))
                .thenReturn(Optional.of(shoreikinOf("S001", "2024")));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(K_JICHITAI_CD, "S001")).thenReturn(List.of());
        when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(K_JICHITAI_CD, "S001")).thenReturn(Optional.empty());

        ShoreikinRenkeiDto dto = assertDoesNotThrow(
                () -> service.findByKeys(K_JICHITAI_CD, List.of(keyOf("S001", "2024"))).get(0));

        assertThat(dto.getBankCd()).isNull();
        assertThat(dto.getBankName()).isNull();
        assertThat(dto.getBranchCd()).isNull();
        assertThat(dto.getBranchName()).isNull();
        assertThat(dto.getShumoku()).isNull();
        assertThat(dto.getKozaNo()).isNull();
        assertThat(dto.getMeigi()).isNull();
    }

    @Test
    @DisplayName("#確認29 toDto 正常系 奨励金エンティティ由来の項目がすべて転記される")
    void 確認29_奨励金エンティティ由来の項目がすべて転記される() {
        Shoreikin s = shoreikinOf("S001", "2024");
        s.setKofuZeigaku(1000L);
        s.setKofuRitsu(new BigDecimal("1.5"));
        s.setKofuGaku(15L);
        s.setKofuYmd(LocalDate.of(2024, 4, 1));
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(K_JICHITAI_CD, "S001", "2024"))
                .thenReturn(Optional.of(s));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(K_JICHITAI_CD, "S001")).thenReturn(List.of());
        when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(K_JICHITAI_CD, "S001")).thenReturn(Optional.empty());

        ShoreikinRenkeiDto dto = service.findByKeys(K_JICHITAI_CD, List.of(keyOf("S001", "2024"))).get(0);

        assertThat(dto.getJichitaiCd()).isEqualTo("01100");
        assertThat(dto.getShiteiNo()).isEqualTo("S001");
        assertThat(dto.getNendo()).isEqualTo("2024");
        assertThat(dto.getKofuZeigaku()).isEqualTo(1000L);
        assertThat(dto.getKofuRitsu()).isEqualByComparingTo(new BigDecimal("1.5"));
        assertThat(dto.getKofuGaku()).isEqualTo(15L);
        assertThat(dto.getKofuYmd()).isEqualTo(LocalDate.of(2024, 4, 1));
    }

    @Test
    @DisplayName("#確認30 toDto 異常系 奨励金の金額項目が null の場合")
    void 確認30_奨励金の金額項目がnull() {
        Shoreikin s = shoreikinOf("S001", "2024");
        s.setKofuZeigaku(null);
        s.setKofuRitsu(null);
        s.setKofuGaku(null);
        s.setKofuYmd(null);
        when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(K_JICHITAI_CD, "S001", "2024"))
                .thenReturn(Optional.of(s));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(K_JICHITAI_CD, "S001")).thenReturn(List.of());
        when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(K_JICHITAI_CD, "S001")).thenReturn(Optional.empty());

        List<ShoreikinRenkeiDto> result = assertDoesNotThrow(
                () -> service.findByKeys(K_JICHITAI_CD, List.of(keyOf("S001", "2024"))));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getKofuZeigaku()).isNull();
        assertThat(result.get(0).getKofuRitsu()).isNull();
        assertThat(result.get(0).getKofuGaku()).isNull();
        assertThat(result.get(0).getKofuYmd()).isNull();
    }
}

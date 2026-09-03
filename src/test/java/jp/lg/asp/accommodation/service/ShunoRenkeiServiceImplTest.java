package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataAccessException;

import jp.lg.asp.accommodation.dto.ShunoDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.ShunoRenkeiServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShunoRenkeiServiceImplTest {

	@Mock
	private EntityManager em;

	@Mock
	private FukaRepository fukaRepository;

	@Mock
	private TokugimuRepository tokugimuRepository;

	@Mock
	private CriteriaBuilder cb;

	@Mock
	private CriteriaQuery<Fuka> cqFuka;

	@Mock
	private CriteriaQuery<Tokugimu> cqTokugimu;

	@Mock
	private Root<Fuka> rootFuka;

	@Mock
	private Root<Tokugimu> rootTokugimu;

	@Mock
	private TypedQuery<Fuka> typedQueryFuka;

	@Mock
	private TypedQuery<Tokugimu> typedQueryTokugimu;

	@Mock
	private Predicate predicate;

	@Mock
	private Subquery<Tokugimu> subquery;

	@Mock
	private Join<?, ?> joinMock;

	@Mock
	private Path<Object> pathMock;

	@InjectMocks
	private ShunoRenkeiServiceImpl shunoRenkeiService;

	private static final String JICHITAI_CD = "011002";

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUpCriteria() {
		when(em.getCriteriaBuilder()).thenReturn(cb);
		when(cb.createQuery(Fuka.class)).thenReturn(cqFuka);
		when(cb.createQuery(Tokugimu.class)).thenReturn(cqTokugimu);

		when(cqFuka.from(Fuka.class)).thenReturn(rootFuka);
		when(cqTokugimu.from(Tokugimu.class)).thenReturn(rootTokugimu);

		when(rootFuka.join(anyString())).thenReturn((Join<Object, Object>) joinMock);
		when(rootFuka.join(anyString(), any())).thenReturn((Join<Object, Object>) joinMock);
		when(rootTokugimu.join(anyString())).thenReturn((Join<Object, Object>) joinMock);
		when(rootTokugimu.join(anyString(), any())).thenReturn((Join<Object, Object>) joinMock);
		when(joinMock.join(anyString())).thenReturn((Join<Object, Object>) joinMock);
		when(joinMock.join(anyString(), any())).thenReturn((Join<Object, Object>) joinMock);

		when(rootFuka.get(anyString())).thenReturn((Path<Object>) pathMock);
		when(rootTokugimu.get(anyString())).thenReturn((Path<Object>) pathMock);
		when(joinMock.get(anyString())).thenReturn((Path<Object>) pathMock);
		when(pathMock.get(anyString())).thenReturn((Path<Object>) pathMock);

		when(cb.equal(any(), any())).thenReturn(predicate);
		when(cb.greaterThanOrEqualTo(any(), any(LocalDate.class))).thenReturn(predicate);
		when(cb.lessThanOrEqualTo(any(), any(LocalDate.class))).thenReturn(predicate);
		when(cb.like(any(), (String) any())).thenReturn(predicate);
		when(cb.exists(any())).thenReturn(predicate);
		when(cb.and(any(Predicate[].class))).thenReturn(predicate);

		when(cqFuka.where(any(Predicate[].class))).thenReturn(cqFuka);
		when(cqTokugimu.where(any(Predicate[].class))).thenReturn(cqTokugimu);
		
		doReturn(typedQueryFuka).when(em).createQuery(cqFuka);
		doReturn(typedQueryTokugimu).when(em).createQuery(cqTokugimu);
	}

	@Nested
	@DisplayName("search メソッドの分岐テスト")
	class SearchTest {

		@Test
		@DisplayName("正常系：shinkokuFromが指定されている場合、searchFromFukaが呼び出されること")
		void search_withShinkokuFrom() {
			when(typedQueryFuka.getResultList()).thenReturn(List.of());
			List<ShunoDto> result = shunoRenkeiService.search(JICHITAI_CD, LocalDate.now(), null, null, null, null, null);
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("正常系：shinkokuToが指定されている場合、searchFromFukaが呼び出されること")
		void search_withShinkokuTo() {
			when(typedQueryFuka.getResultList()).thenReturn(List.of());
			List<ShunoDto> result = shunoRenkeiService.search(JICHITAI_CD, null, LocalDate.now(), null, null, null, null);
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("正常系：taishoMonthが指定されている場合、searchFromFukaが呼び出されること")
		void search_withTaishoMonth() {
			when(typedQueryFuka.getResultList()).thenReturn(List.of());
			List<ShunoDto> result = shunoRenkeiService.search(JICHITAI_CD, null, null, "2026-04", null, null, null);
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("正常系：条件がすべてnullの場合、searchFromTokugimuが呼び出されること")
		void search_allNull() {
			when(typedQueryTokugimu.getResultList()).thenReturn(List.of());
			List<ShunoDto> result = shunoRenkeiService.search(JICHITAI_CD, null, null, null, null, null, null);
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("異常系：EntityManager実行時に例外が発生した場合にスローされること")
		@SuppressWarnings("unchecked")
		void search_exception() {
			when(em.createQuery(any(CriteriaQuery.class)))
					.thenThrow(new org.springframework.dao.DataAccessResourceFailureException("DB Error"));

			assertThatThrownBy(
					() -> shunoRenkeiService.search(JICHITAI_CD, LocalDate.now(), null, null, null, null, null))
							.isInstanceOf(DataAccessException.class);
		}
	}

	@Nested
	@DisplayName("searchFromFuka 詳細・マッチタイプ・フィルタのテスト")
	class SearchFromFukaDetailTest {

		@BeforeEach
		void setupSubquery() {
			when(cqFuka.subquery(Tokugimu.class)).thenReturn(subquery);
			when(subquery.from(Tokugimu.class)).thenReturn(rootTokugimu);
			when(subquery.select(any())).thenReturn(subquery);
			when(subquery.where(any(Predicate[].class))).thenReturn(subquery);
		}

		@Test
		@DisplayName("境界値：nameMatchType が prefix の場合")
		void matchTypePrefix() {
			Fuka fuka = new Fuka();
			fuka.setJichitaiCd(JICHITAI_CD);
			fuka.setShiteiNo("S001");
			when(typedQueryFuka.getResultList()).thenReturn(List.of(fuka));
			when(tokugimuRepository.findByJichitaiCdAndShiteiNo(any(), any())).thenReturn(List.of());

			List<ShunoDto> result = shunoRenkeiService.search(JICHITAI_CD, LocalDate.now(), null, null, "S001", "山田", "prefix");
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("境界値：nameMatchType が exact の場合")
		void matchTypeExact() {
			Fuka fuka = new Fuka();
			fuka.setJichitaiCd(JICHITAI_CD);
			fuka.setShiteiNo("S001");
			when(typedQueryFuka.getResultList()).thenReturn(List.of(fuka));
			when(tokugimuRepository.findByJichitaiCdAndShiteiNo(any(), any())).thenReturn(List.of());

			List<ShunoDto> result = shunoRenkeiService.search(JICHITAI_CD, LocalDate.now(), null, null, "S001", "山田", "exact");
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("境界値：nameMatchType がその他（default）の場合、およびTokugimuが存在する場合のDTO返却")
		void matchTypeDefaultAndSuccess() {
			Fuka fuka = new Fuka();
			fuka.setJichitaiCd(JICHITAI_CD);
			fuka.setShiteiNo("S001");
			fuka.setTaishoYm("202604");

			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setJichitaiCd(JICHITAI_CD);
			tokugimu.setShiteiNo("S001");

			when(typedQueryFuka.getResultList()).thenReturn(List.of(fuka));
			when(tokugimuRepository.findByJichitaiCdAndShiteiNo(any(), any())).thenReturn(List.of(tokugimu));

			List<ShunoDto> result = shunoRenkeiService.search(JICHITAI_CD, LocalDate.now(), null, null, "S001", "山田", "other");
			assertThat(result).hasSize(1);
		}
	}

	@Nested
	@DisplayName("searchFromTokugimu 詳細・重複排除・フィルタのテスト")
	class SearchFromTokugimuDetailTest {

		@Test
		@DisplayName("境界値：Tokugimuの重複排除(seen)およびFukaが存在しない場合のフィルタ除外")
		void duplicateAndEmptyFuka() {
			Tokugimu t1 = new Tokugimu();
			t1.setShiteiNo("S001");
			Tokugimu t2 = new Tokugimu();
			t2.setShiteiNo("S001"); // 重複
			Tokugimu t3 = new Tokugimu();
			t3.setShiteiNo("S002"); // Fukaなしにする

			when(typedQueryTokugimu.getResultList()).thenReturn(List.of(t1, t2, t3));
			when(fukaRepository.findByJichitaiCdAndShiteiNo(eq(JICHITAI_CD), eq("S001"))).thenReturn(List.of());
			when(fukaRepository.findByJichitaiCdAndShiteiNo(eq(JICHITAI_CD), eq("S002"))).thenReturn(List.of());

			List<ShunoDto> result = shunoRenkeiService.search(JICHITAI_CD, null, null, null, "S001", "山田", "exact");
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("正常系：Tokugimuから検索してFukaがヒットする場合")
		void successTokugimuPath() {
			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setJichitaiCd(JICHITAI_CD);
			tokugimu.setShiteiNo("S001");

			Fuka fuka = new Fuka();
			fuka.setTaishoYm("202604");

			when(typedQueryTokugimu.getResultList()).thenReturn(List.of(tokugimu));
			when(fukaRepository.findByJichitaiCdAndShiteiNo(any(), any())).thenReturn(List.of(fuka));

			List<ShunoDto> result = shunoRenkeiService.search(JICHITAI_CD, null, null, null, null, null, null);
			assertThat(result).hasSize(1);
		}
	}

	@Nested
	@DisplayName("toDtoFromTokugimuAndFuka (DTO変換) 境界値テスト")
	class ToDtoTest {

		@Test
		@DisplayName("境界値：Tokugimuのatenaがnullの場合、あるいはatenaNoがnullの場合に正しくnull処理されること")
		void toDto_atenaNull_handlesNull() {
			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setJichitaiCd(JICHITAI_CD);
			tokugimu.setShiteiNo("S001");
			tokugimu.setAtena(null);
			tokugimu.setAtenaNo(null);

			Fuka fuka = new Fuka();
			fuka.setTaishoYm("202604");

			when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any()))
					.thenReturn(List.of(fuka));
			when(tokugimuRepository.findByJichitaiCdAndShiteiNo(any(), any()))
					.thenReturn(List.of(tokugimu));

			ShunoDto.Key key = new ShunoDto.Key();
			key.setShiteiNo("S001");
			key.setNendo("2026");
			key.setKibetsu(1);

			List<ShunoDto> result = shunoRenkeiService.findByKeys(JICHITAI_CD, List.of(key));

			assertThat(result).hasSize(1);
			assertThat(result.get(0).getName()).isNull();
			assertThat(result.get(0).getAtenaNo()).isNull();
		}

		@Test
		@DisplayName("境界値：FukaのtaishoYmが6桁以外の場合、フォーマットされずにそのまま設定されること")
		void toDto_taishoYmNotSixDigits_keepsOriginal() {
			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setJichitaiCd(JICHITAI_CD);
			tokugimu.setShiteiNo("S001");

			Fuka fuka = new Fuka();
			fuka.setTaishoYm("2026-04");

			when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any()))
					.thenReturn(List.of(fuka));
			when(tokugimuRepository.findByJichitaiCdAndShiteiNo(any(), any()))
					.thenReturn(List.of(tokugimu));

			ShunoDto.Key key = new ShunoDto.Key();
			key.setShiteiNo("S001");
			key.setNendo("2026");
			key.setKibetsu(1);

			List<ShunoDto> result = shunoRenkeiService.findByKeys(JICHITAI_CD, List.of(key));

			assertThat(result).hasSize(1);
			assertThat(result.get(0).getTaishoYm()).isEqualTo("2026-04");
		}
	}

	@Nested
	@DisplayName("findByKeys メソッドのテスト")
	class FindByKeysTest {

		@Test
		@DisplayName("正常系：指定されたキーに紐づくFukaおよびTokugimuが両方存在する場合、正しくDTOに変換されること")
		void findByKeys_success() {
			ShunoDto.Key key = new ShunoDto.Key();
			key.setShiteiNo("S001");
			key.setNendo("2026");
			key.setKibetsu(1);

			Fuka fuka = new Fuka();
			fuka.setJichitaiCd(JICHITAI_CD);
			fuka.setShiteiNo("S001");
			fuka.setTaishoYm("202604");

			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setJichitaiCd(JICHITAI_CD);
			tokugimu.setShiteiNo("S001");
			Atena atena = new Atena();
			atena.setName("山田太郎");
			tokugimu.setAtena(atena);
			tokugimu.setAtenaNo(BigDecimal.valueOf(100));

			when(fukaRepository.findLatestByNendoAndKibetsu(eq(JICHITAI_CD), eq("S001"), eq("2026"), eq(1)))
					.thenReturn(List.of(fuka));
			when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "S001"))
					.thenReturn(List.of(tokugimu));

			List<ShunoDto> result = shunoRenkeiService.findByKeys(JICHITAI_CD, List.of(key));

			assertThat(result).hasSize(1);
			assertThat(result.get(0).getName()).isEqualTo("山田太郎");
			assertThat(result.get(0).getTaishoYm()).isEqualTo("2026-04");
		}

		@Test
		@DisplayName("境界値：入力のkeysリストが空の場合、空のリストが返却されること")
		void findByKeys_emptyKeys_returnsEmpty() {
			List<ShunoDto> result = shunoRenkeiService.findByKeys(JICHITAI_CD, List.of());
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("境界値：fukaRepositoryの検索結果が空の場合、スキップされること")
		void findByKeys_fukaNotFound_skips() {
			ShunoDto.Key key = new ShunoDto.Key();
			key.setShiteiNo("S001");
			key.setNendo("2026");
			key.setKibetsu(1);

			when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any()))
					.thenReturn(List.of());

			List<ShunoDto> result = shunoRenkeiService.findByKeys(JICHITAI_CD, List.of(key));
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("境界値：tokugimuRepositoryの検索結果が空の場合、スキップされること")
		void findByKeys_tokugimuNotFound_skips() {
			ShunoDto.Key key = new ShunoDto.Key();
			key.setShiteiNo("S001");
			key.setNendo("2026");
			key.setKibetsu(1);

			Fuka fuka = new Fuka();
			fuka.setJichitaiCd(JICHITAI_CD);
			fuka.setShiteiNo("S001");

			when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any()))
					.thenReturn(List.of(fuka));
			when(tokugimuRepository.findByJichitaiCdAndShiteiNo(any(), any()))
					.thenReturn(List.of());

			List<ShunoDto> result = shunoRenkeiService.findByKeys(JICHITAI_CD, List.of(key));
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("異常系：データベース例外が発生した場合にスローされること")
		void findByKeys_exception_throwsException() {
			ShunoDto.Key key = new ShunoDto.Key();
			key.setShiteiNo("S001");

			when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any()))
					.thenThrow(new DataAccessException("DB Error") {});

			assertThatThrownBy(() -> shunoRenkeiService.findByKeys(JICHITAI_CD, List.of(key)))
					.isInstanceOf(DataAccessException.class);
		}
	}
}
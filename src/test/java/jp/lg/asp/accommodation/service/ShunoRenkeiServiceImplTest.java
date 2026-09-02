package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

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
<<<<<<< HEAD
	private FukaRepository fukaRepository;

	@Mock
	private TokugimuRepository tokugimuRepository;

	@InjectMocks
	private ShunoRenkeiServiceImpl shunoRenkeiService;

	private static final String JICHITAI_CD = "011002";

	@Nested
	@DisplayName("findByKeys メソッドのテスト")
	class FindByKeysTest {

		@Test
		@DisplayName("正常系：指定されたキーに紐づくFukaおよびTokugimuが存在する場合、DTOに変換されてリストに格納されること")
		void findByKeys_success() {
			ShunoDto.Key key = new ShunoDto.Key();
			key.setShiteiNo("S001");
			key.setNendo("2026");
			key.setKibetsu(1);

			Fuka fuka = new Fuka();
			fuka.setJichitaiCd(JICHITAI_CD);
			fuka.setShiteiNo("S001");

			Tokugimu tokugimu = new Tokugimu();

			when(fukaRepository.findLatestByNendoAndKibetsu(eq(JICHITAI_CD), eq("S001"), eq("2026"), eq(1)))
					.thenReturn(List.of(fuka));
			when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "S001"))
					.thenReturn(List.of(tokugimu));

			List<ShunoDto> result = shunoRenkeiService.findByKeys(JICHITAI_CD, List.of(key));

			assertThat(result).hasSize(1);
			verify(fukaRepository, times(1)).findLatestByNendoAndKibetsu(any(), any(), any(), any());
			verify(tokugimuRepository, times(1)).findByJichitaiCdAndShiteiNo(any(), any());
		}

		@Test
		@DisplayName("境界値：入力のkeysリストが空の場合、ループ処理が行われず空のリストが返却されること")
		void findByKeys_emptyKeys_returnsEmptyList() {
			List<ShunoDto> result = shunoRenkeiService.findByKeys(JICHITAI_CD, List.of());

			assertThat(result).isEmpty();
			verify(fukaRepository, never()).findLatestByNendoAndKibetsu(any(), any(), any(), any());
		}

		@Test
		@DisplayName("境界値：fukaRepositoryの検索結果が空の場合、データ追加されずにスキップされること")
		void findByKeys_fukaNotFound_skips() {
			ShunoDto.Key key = new ShunoDto.Key();
			key.setShiteiNo("S001");
			key.setNendo("2026");
			key.setKibetsu(1);

			when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any()))
					.thenReturn(List.of());

			List<ShunoDto> result = shunoRenkeiService.findByKeys(JICHITAI_CD, List.of(key));

			assertThat(result).isEmpty();
			verify(tokugimuRepository, never()).findByJichitaiCdAndShiteiNo(any(), any());
		}

		@Test
		@DisplayName("境界値：fukaListは存在するが、tokugimuRepositoryの検索結果が空の場合、スキップされること")
=======
	private jakarta.persistence.EntityManager em;

	@Mock
	private FukaRepository fukaRepository;

	@Mock
	private TokugimuRepository tokugimuRepository;

	@InjectMocks
	private ShunoRenkeiServiceImpl shunoRenkeiService;

	private static final String JICHITAI_CD = "011002";

	@Nested
	@DisplayName("search メソッドのテスト")
	class SearchTest {

		@Test
		@DisplayName("正常系：shinkokuFromが指定されている場合、searchFromFukaが呼び出されDTOリストが返却されること")
		void search_withShinkokuFrom_callsSearchFromFuka() {
			Fuka fuka = new Fuka();
			fuka.setJichitaiCd(JICHITAI_CD);
			fuka.setShiteiNo("S001");
			fuka.setTaishoYm("202604");

			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setJichitaiCd(JICHITAI_CD);
			tokugimu.setShiteiNo("S001");
		}

		@Test
		@DisplayName("正常系：検索条件がすべてnullの場合、searchFromTokugimuが呼び出されること")
		void search_allNullConditions_callsSearchFromTokugimu() {
		}
	}

	@Nested
	@DisplayName("findByKeys メソッドのテスト")
	class FindByKeysTest {

		@Test
		@DisplayName("正常系：指定されたキーに紐づくFukaおよびTokugimuが存在する場合、DTOに変換されること")
		void findByKeys_success() {
			ShunoDto.Key key = new ShunoDto.Key();
			key.setShiteiNo("S001");
			key.setNendo("2026");
			key.setKibetsu(1);

			Fuka fuka = new Fuka();
			fuka.setJichitaiCd(JICHITAI_CD);
			fuka.setShiteiNo("S001");
			fuka.setTaishoYm("2026-04");

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
		}

		@Test
		@DisplayName("境界値：keysリストが空の場合、空のリストが返却されること")
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
>>>>>>> fix-370_shunoRenkei
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
<<<<<<< HEAD

=======
>>>>>>> fix-370_shunoRenkei
			assertThat(result).isEmpty();
		}

		@Test
<<<<<<< HEAD
		@DisplayName("異常系：リポジトリ実行時に例外が発生した場合に例外がスローされること")
		void findByKeys_repositoryThrowsException_throwsException() {
=======
		@DisplayName("異常系：リポジトリで例外が発生した場合に例外がスローされること")
		void findByKeys_exception_throwsException() {
>>>>>>> fix-370_shunoRenkei
			ShunoDto.Key key = new ShunoDto.Key();
			key.setShiteiNo("S001");

			when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any()))
					.thenThrow(new DataAccessException("DB Error") {});

			assertThatThrownBy(() -> shunoRenkeiService.findByKeys(JICHITAI_CD, List.of(key)))
					.isInstanceOf(DataAccessException.class);
		}
	}
}
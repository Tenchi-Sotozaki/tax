package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.ShoreikinBulkDto;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.KofuRitsu;
import jp.lg.asp.accommodation.entity.Shoreikin;
import jp.lg.asp.accommodation.entity.ShoreikinId;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.KofuRitsuRepository;
import jp.lg.asp.accommodation.repository.ShoreikinRepository;
import jp.lg.asp.accommodation.repository.ShunoRirekiRepository;
import jp.lg.asp.accommodation.service.impl.ShoreikinBulkServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShoreikinBulkServiceImplTest {

	@Mock
	private ShoreikinRepository shoreikinRepository;

	@Mock
	private FukaRepository fukaRepository;

	@Mock
	private ShunoRirekiRepository shunoRirekiRepository;

	@Mock
	private KofuRitsuRepository kofuRitsuRepository;

	@Mock
	private JichitaiContext jichitaiContext;

	@InjectMocks
	private ShoreikinBulkServiceImpl service;

	private static final String JICHITAI_CD = "011002";

	@Nested
	@DisplayName("findKofuRitsuList メソッドのテスト")
	class FindKofuRitsuListTest {

		@Test
		@DisplayName("正常系：指定した自治体コードと年度に該当する交付率リストが取得できること")
		void findKofuRitsuList_success_returnsList() {
			int year = 2026;
			List<BigDecimal> expectedList = List.of(BigDecimal.valueOf(50.0));

			when(kofuRitsuRepository.findKofuRitsuByJichitaiCd(JICHITAI_CD, year)).thenReturn(expectedList);

			List<BigDecimal> result = service.findKofuRitsuList(JICHITAI_CD, year);

			assertThat(result).isEqualTo(expectedList);
		}

		@Test
		@DisplayName("境界値：該当するデータが存在しない場合、空のリストが返却されること")
		void findKofuRitsuList_notFound_returnsEmptyList() {
			int year = 9999;

			when(kofuRitsuRepository.findKofuRitsuByJichitaiCd(JICHITAI_CD, year)).thenReturn(Collections.emptyList());

			List<BigDecimal> result = service.findKofuRitsuList(JICHITAI_CD, year);

			assertThat(result).isEmpty();
		}
	}

	@Nested
	@DisplayName("executeBulkSanshutsu メソッドのテスト")
	class ExecuteBulkSanshutsuTest {

		@Test
		@DisplayName("正常系：年度が正しく指定され、一括算出・保存処理が正常に完了すること")
		void executeBulkSanshutsu_success_executesAndSaves() {
			ShoreikinBulkDto dto = new ShoreikinBulkDto();
			dto.setNendo("2026");
			dto.setKofuRitsu(BigDecimal.valueOf(50.0));
			dto.setIncludeCalculated(true);

			Fuka fuka = new Fuka();
			fuka.setShiteiNo("S001");
			fuka.setKibetsu(1);
			fuka.setNewFlg("1");
			fuka.setDelFlg("0");
			fuka.setTotalZeigaku(10000L);
			fuka.setShinkokuYmd(LocalDate.of(2026, 6, 1));
			fuka.setRno(1);

			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
			when(kofuRitsuRepository.findKofuRitsuEntityByJichitaiCd(JICHITAI_CD, 2026))
					.thenReturn(List.of(new KofuRitsu()));
			when(fukaRepository.findByJichitaiCdAndNendoAndNewFlgAndDelFlg(JICHITAI_CD, "2026", "1", "0"))
					.thenReturn(List.of(fuka));
			when(shunoRirekiRepository.sumNonyugakuByShiteiNoIn(eq(JICHITAI_CD), anyList()))
	        .thenReturn(Collections.singletonList(new Object[] { "S001", "2026", "1", 10000L }));
			when(shoreikinRepository.findById(any(ShoreikinId.class))).thenReturn(Optional.empty());
			when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoAndDelFlgAndNewFlg(
					eq(JICHITAI_CD), eq("S001"), eq("2026"), eq("0"), eq("1")))
					.thenReturn(List.of(fuka));

			ShoreikinBulkDto result = service.executeBulkSanshutsu(dto);

			assertThat(result.isExecuted()).isTrue();
			assertThat(result.getSuccessCount()).isEqualTo(1);
			assertThat(result.getResultMessage()).contains("成功: 1件");
			verify(shoreikinRepository, times(1)).save(any(Shoreikin.class));
		}

		@Test
		@DisplayName("境界値：年度（nendo）が未指定の場合、計算を行わずにメッセージを設定して処理が終了すること")
		void executeBulkSanshutsu_nendoNull_returnsEarlyWithMessage() {
			ShoreikinBulkDto dto = new ShoreikinBulkDto();
			dto.setNendo(null);

			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);

			ShoreikinBulkDto result = service.executeBulkSanshutsu(dto);

			assertThat(result.getResultMessage()).isEqualTo("交付金年度が指定されていません");
			verify(fukaRepository, never()).findByJichitaiCdAndNendoAndNewFlgAndDelFlg(any(), any(), any(), any());
		}

		@Test
		@DisplayName("境界値：既に算出済みであり上書きフラグがfalseの場合、スキップされること")
		void executeBulkSanshutsu_alreadyCalculatedAndNoInclude_skipsProcessing() {
			ShoreikinBulkDto dto = new ShoreikinBulkDto();
			dto.setNendo("2026");
			dto.setKofuRitsu(BigDecimal.valueOf(50.0));
			dto.setIncludeCalculated(false);

			Fuka fuka = new Fuka();
			fuka.setShiteiNo("S001");

			Shoreikin existing = new Shoreikin();
			existing.setShiteiNo("S001");

			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
			when(kofuRitsuRepository.findKofuRitsuEntityByJichitaiCd(JICHITAI_CD, 2026)).thenReturn(Collections.emptyList());
			when(fukaRepository.findByJichitaiCdAndNendoAndNewFlgAndDelFlg(JICHITAI_CD, "2026", "1", "0"))
					.thenReturn(List.of(fuka));
			when(shunoRirekiRepository.sumNonyugakuByShiteiNoIn(eq(JICHITAI_CD), anyList())).thenReturn(Collections.emptyList());
			when(shoreikinRepository.findById(any(ShoreikinId.class))).thenReturn(Optional.of(existing));

			ShoreikinBulkDto result = service.executeBulkSanshutsu(dto);

			assertThat(result.getSkipCount()).isEqualTo(1);
			assertThat(result.getSuccessCount()).isEqualTo(0);
			verify(shoreikinRepository, never()).save(any());
		}

		@Test
		@DisplayName("異常系：個別データの処理中に例外が発生した場合、エラーとしてカウントされて処理が継続されること")
		void executeBulkSanshutsu_exceptionDuringSave_countsAsFailure() {
			ShoreikinBulkDto dto = new ShoreikinBulkDto();
			dto.setNendo("2026");
			dto.setKofuRitsu(BigDecimal.valueOf(50.0));
			dto.setIncludeCalculated(true);

			Fuka fuka = new Fuka();
			fuka.setShiteiNo("S001");
			fuka.setKibetsu(1);
			fuka.setTotalZeigaku(10000L);
			fuka.setShinkokuYmd(LocalDate.of(2026, 6, 1));
			fuka.setRno(1);

			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
			when(kofuRitsuRepository.findKofuRitsuEntityByJichitaiCd(JICHITAI_CD, 2026)).thenReturn(Collections.emptyList());
			when(fukaRepository.findByJichitaiCdAndNendoAndNewFlgAndDelFlg(JICHITAI_CD, "2026", "1", "0"))
					.thenReturn(List.of(fuka));
			when(shunoRirekiRepository.sumNonyugakuByShiteiNoIn(eq(JICHITAI_CD), anyList())).thenReturn(Collections.emptyList());
			when(shoreikinRepository.findById(any(ShoreikinId.class))).thenReturn(Optional.empty());
			when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoAndDelFlgAndNewFlg(any(), any(), any(), any(), any()))
					.thenReturn(List.of(fuka));
			when(shoreikinRepository.save(any())).thenThrow(new DataAccessException("Save Error") {});

			ShoreikinBulkDto result = service.executeBulkSanshutsu(dto);

			assertThat(result.getFailureCount()).isEqualTo(1);
			assertThat(result.getSuccessCount()).isEqualTo(0);
		}
	}
}
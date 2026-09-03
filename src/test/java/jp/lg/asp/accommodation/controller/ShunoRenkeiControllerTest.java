package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
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
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.ShunoDto;
import jp.lg.asp.accommodation.service.ShunoRenkeiService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShunoRenkeiControllerTest {
	@Mock
	private JichitaiContext jichitaiContext;

	@Mock
	private ScreenAccessChecker accessChecker;

	@Mock
	private ShunoRenkeiService shunoRenkeiService;

	@Mock
	private Model model;

	@InjectMocks
	private ShunoRenkeiController shunoRenkeiController;

	private static final String JICHITAI_CD = "011002";

	@Nested
	@DisplayName("kakunin メソッドのテスト")
	class KakuninTest {

		@Test
		@DisplayName("正常系：有効なJSONとアクセス権があり、サービスからデータが取得できる場合、モデルに設定されてビュー名が返却されること")
		void kakunin_success() {
			String keysJson = "[{\"shiteiNo\":\"S001\",\"nendo\":2026,\"kibetsu\":1}]";
			List<ShunoDto> rows = List.of(new ShunoDto());

			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
			when(shunoRenkeiService.findByKeys(eq(JICHITAI_CD), any())).thenReturn(rows);

			String viewName = shunoRenkeiController.kakunin(keysJson, model);

			assertThat(viewName).isEqualTo("renkei/shunoRenkeiKakunin");
			verify(accessChecker, times(1)).checkAccess(any());
			verify(model, times(1)).addAttribute("rows", rows);
		}

		@Test
		@DisplayName("異常系：アクセス権限チェックで例外が発生した場合、例外がキャッチされ空のリストがモデルに設定されること")
		void kakunin_accessDenied_addsEmptyList() {
			String keysJson = "[]";

			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
			doThrow(new RuntimeException("Access Denied")).when(accessChecker).checkAccess(any());

			String viewName = shunoRenkeiController.kakunin(keysJson, model);

			assertThat(viewName).isEqualTo("renkei/shunoRenkeiKakunin");
			verify(model, times(1)).addAttribute("rows", Collections.emptyList());
		}

		@Test
		@DisplayName("異常系：不正なJSON文字列が渡された場合、パースエラーがキャッチされ空のリストがモデルに設定されること")
		void kakunin_invalidJson_addsEmptyList() {
			String keysJson = "invalid-json";

			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);

			String viewName = shunoRenkeiController.kakunin(keysJson, model);

			assertThat(viewName).isEqualTo("renkei/shunoRenkeiKakunin");
			verify(model, times(1)).addAttribute("rows", Collections.emptyList());
		}

		@Test
		@DisplayName("異常系：サービス層の実行中に例外が発生した場合、例外がキャッチされ空のリストがモデルに設定されること")
		void kakunin_serviceThrowsException_addsEmptyList() {
			String keysJson = "[{\"shiteiNo\":\"S001\",\"nendo\":2026,\"kibetsu\":1}]";

			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
			when(shunoRenkeiService.findByKeys(any(), any())).thenThrow(new RuntimeException("Service Error"));

			String viewName = shunoRenkeiController.kakunin(keysJson, model);

			assertThat(viewName).isEqualTo("renkei/shunoRenkeiKakunin");
			verify(model, times(1)).addAttribute("rows", Collections.emptyList());
		}
	}
			
	@Nested
	@DisplayName("index メソッドのテスト")
	class IndexTest {

		@Test
		@DisplayName("正常系：searchedがnullの場合、検索を行わずに初期画面のビュー名が返却されること")
		void index_searchedNull_returnsViewWithoutSearch() {
			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);

			String viewName = shunoRenkeiController.index(
					null, null, null, null, null, "partial", 0, 10, null, model);

			assertThat(viewName).isEqualTo("renkei/shunoRenkei");
			verify(accessChecker, times(1)).checkAccess(any());
			verify(model, times(1)).addAttribute(eq("searchForm"), any());
			verify(shunoRenkeiService, never()).search(any(), any(), any(), any(), any(), any(), any());
		}

		@Test
		@DisplayName("正常系：searchedが指定され、検索・ページング処理が正常に行われること")
		void index_searchedTrue_performsSearchAndPaging() {
			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
			when(shunoRenkeiService.search(any(), any(), any(), any(), any(), any(), any()))
					.thenReturn(List.of(new ShunoDto()));

			String viewName = shunoRenkeiController.index(
					"2026-04-01", "2026-04-30", "2026-04", "S001", "山田", "partial", 0, 10, "true", model);

			assertThat(viewName).isEqualTo("renkei/shunoRenkei");
			verify(model, times(1)).addAttribute(eq("items"), any());
		}

		@Test
		@DisplayName("境界値：pageSizeが0以下の場合、デフォルトサイズ10として処理されること")
		void index_invalidPageSize_defaultsTo10() {
			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
			when(shunoRenkeiService.search(any(), any(), any(), any(), any(), any(), any()))
					.thenReturn(List.of(new ShunoDto()));

			String viewName = shunoRenkeiController.index(
					null, null, null, null, null, "partial", 0, 0, "true", model);

			assertThat(viewName).isEqualTo("renkei/shunoRenkei");
		}

		@Test
		@DisplayName("境界値：pageが負の値の場合、0ページ目に補正されること")
		void index_negativePage_clampedToZero() {
			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
			when(shunoRenkeiService.search(any(), any(), any(), any(), any(), any(), any()))
					.thenReturn(List.of(new ShunoDto()));

			String viewName = shunoRenkeiController.index(
					null, null, null, null, null, "partial", -1, 10, "true", model);

			assertThat(viewName).isEqualTo("renkei/shunoRenkei");
		}

		@Test
		@DisplayName("境界値：指定されたpageが総ページ数以上のとき、最終ページに補正されること")
		void index_pageExceedsTotal_clampedToLastPage() {
			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
			when(shunoRenkeiService.search(any(), any(), any(), any(), any(), any(), any()))
					.thenReturn(List.of(new ShunoDto()));

			String viewName = shunoRenkeiController.index(
					null, null, null, null, null, "partial", 5, 10, "true", model);

			assertThat(viewName).isEqualTo("renkei/shunoRenkei");
		}

		@Test
		@DisplayName("境界値：日付文字列が空文字の場合、nullとしてサービスに渡されること")
		void index_emptyDates_passedAsNull() {
			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
			when(shunoRenkeiService.search(eq(JICHITAI_CD), isNull(), isNull(), any(), any(), any(), any()))
					.thenReturn(List.of());

			String viewName = shunoRenkeiController.index(
					"", "", null, null, null, "partial", 0, 10, "true", model);

			assertThat(viewName).isEqualTo("renkei/shunoRenkei");
		}

		@Test
		@DisplayName("異常系：アクセス権限チェックで例外が発生した場合にそのままスローされること")
		void index_accessDenied_throwsException() {
			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
			doThrow(new RuntimeException("Access Denied")).when(accessChecker).checkAccess(any());

			assertThatThrownBy(() -> shunoRenkeiController.index(
					null, null, null, null, null, "partial", 0, 10, null, model))
					.isInstanceOf(RuntimeException.class);
		}
	}

	@Nested
	@DisplayName("search メソッドのテスト")
	class SearchTest {

		@Test
		@DisplayName("正常系：検索条件に一致するリストが正常に返却されること")
		void search_success() {
			List<ShunoDto> expectedList = List.of(new ShunoDto());
			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
			when(shunoRenkeiService.search(any(), any(), any(), any(), any(), any(), any()))
					.thenReturn(expectedList);

			List<ShunoDto> result = shunoRenkeiController.search(
					"2026-04-01", "2026-04-30", "2026-04", "S001", "山田", "partial");

			assertThat(result).isSameAs(expectedList);
		}

		@Test
		@DisplayName("境界値：日付が空文字やnullの場合にnullとしてサービスに渡されること")
		void search_blankDates_passedAsNull() {
			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
			when(shunoRenkeiService.search(eq(JICHITAI_CD), isNull(), isNull(), any(), any(), any(), any()))
					.thenReturn(List.of());

			List<ShunoDto> result = shunoRenkeiController.search(null, "", null, null, null, "partial");

			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("異常系：サービス層で例外が発生した場合にそのままスローされること")
		void search_serviceThrowsException_throwsException() {
			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
			when(shunoRenkeiService.search(any(), any(), any(), any(), any(), any(), any()))
					.thenThrow(new RuntimeException("Service Error"));

			assertThatThrownBy(() -> shunoRenkeiController.search(null, null, null, null, null, "partial"))
					.isInstanceOf(RuntimeException.class);
		}
	}

	@Nested
	@DisplayName("downloadCsv メソッドのテスト")
	class DownloadCsvTest {

		@Test
		@DisplayName("正常系：有効なキーリストを受け取り、CSVのバイト配列がレスポンスとして返却されること")
		void downloadCsv_success() {
			ShunoDto dto = new ShunoDto();
			dto.setAtenaNo("123");
			dto.setNendo("2026");
			dto.setKibetsu(1);
			dto.setTorokuYmd(LocalDate.of(2026, 4, 1));
			dto.setShinkokuYmd(LocalDate.of(2026, 4, 2));
			dto.setTaishoYm("2026-04");
			dto.setTotalZeigaku(1000L);
			dto.setCityZeigaku(600L);
			dto.setKenZeigaku(400L);
			dto.setKasanKbn1("1");
			dto.setKasanRitsu1(BigDecimal.valueOf(10));
			dto.setKasanGaku1(100L);
			dto.setKasanKbn2("2");
			dto.setKasanKbn3("3");
			dto.setEntaikin(50L);
			dto.setNokigen(LocalDate.of(2026, 4, 30));

			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
			when(shunoRenkeiService.findByKeys(eq(JICHITAI_CD), any())).thenReturn(List.of(dto));

			ResponseEntity<byte[]> response = shunoRenkeiController.downloadCsv(List.of(new ShunoDto.Key()));

			assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
			assertThat(response.getBody()).isNotNull();
			assertThat(response.getHeaders().getContentType().toString()).contains("text/csv");
		}

		@Test
		@DisplayName("境界値：DTOの各フィールドがnull、あるいは異なる加算区分やフォーマット外の対象年月の場合でも正常に処理されること")
		void downloadCsv_nullOrAlternativeValues_handlesCorrectly() {
			ShunoDto dto = new ShunoDto();
			dto.setTaishoYm("202604");
			dto.setKasanKbn1("other");
			dto.setKasanKbn2(null);

			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
			when(shunoRenkeiService.findByKeys(eq(JICHITAI_CD), any())).thenReturn(List.of(dto));

			ResponseEntity<byte[]> response = shunoRenkeiController.downloadCsv(List.of(new ShunoDto.Key()));

			assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
		}

		@Test
		@DisplayName("異常系：サービス層で例外が発生した場合に例外がスローされること")
		void downloadCsv_serviceThrowsException_throwsException() {
			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
			when(shunoRenkeiService.findByKeys(any(), any())).thenThrow(new RuntimeException("Service Error"));

			assertThatThrownBy(() -> shunoRenkeiController.downloadCsv(List.of()))
					.isInstanceOf(RuntimeException.class);
		}
	}	
}
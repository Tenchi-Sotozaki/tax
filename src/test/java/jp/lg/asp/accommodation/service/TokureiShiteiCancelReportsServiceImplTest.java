package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jp.lg.asp.accommodation.dto.TokureiShiteiCancelDto;
import jp.lg.asp.accommodation.service.impl.TokureiShiteiCancelReportsServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TokureiShiteiCancelReportsServiceImplTest {

	@InjectMocks
	private TokureiShiteiCancelReportsServiceImpl reportsService;

	@Nested
	@DisplayName("generateTsuchiPdf メソッドのテスト")
	class GenerateTsuchiPdfTest {

		@Test
		@DisplayName("正常系：すべての項目が正常に設定されている場合にPDFバイト配列が返却されること")
		void successWithAllFields() {
			TokureiShiteiCancelDto dto = new TokureiShiteiCancelDto();
			dto.setHakkoYmd(LocalDate.of(2026, 4, 1));
			dto.setTekiyoYmd("2026-05");
			dto.setJorei("条例テキスト");
			dto.setCity("テスト市");
			dto.setRiyu("取消理由");
			dto.setTokuYubin("123-4567");
			dto.setTokuJusho("テスト住所1");
			dto.setTokuName("テスト特例者");
			dto.setShisetsuYubin("765-4321");
			dto.setShisetsuJusho("テスト住所2");
			dto.setShisetsuName("テスト施設");
			dto.setShiteiNo("12345");
			dto.setBiko("備考テキスト");
			dto.setKoin(new byte[]{1, 2, 3});

			byte[] resultPdf = reportsService.generateTsuchiPdf(dto);

			assertThat(resultPdf).isNotNull();
			assertThat(resultPdf).isNotEmpty();
		}

		@Test
		@DisplayName("境界値：発行年月日（hakkoYmd）がnullの場合に空文字として処理されPDF生成処理が実行されること")
		void boundaryHakkoYmdIsNull() {
			TokureiShiteiCancelDto dto = new TokureiShiteiCancelDto();
			dto.setHakkoYmd(null);
			dto.setTekiyoYmd("2026-05");

			byte[] resultPdf = reportsService.generateTsuchiPdf(dto);

			assertThat(resultPdf).isNotNull();
			assertThat(resultPdf).isNotEmpty();
		}

		@Test
		@DisplayName("境界値：適用年月日（tekiyoYmd）がnullの場合に空文字として処理されPDF生成処理が実行されること")
		void boundaryTekiyoYmdIsNull() {
			TokureiShiteiCancelDto dto = new TokureiShiteiCancelDto();
			dto.setHakkoYmd(LocalDate.of(2026, 4, 1));
			dto.setTekiyoYmd(null);

			byte[] resultPdf = reportsService.generateTsuchiPdf(dto);

			assertThat(resultPdf).isNotNull();
			assertThat(resultPdf).isNotEmpty();
		}

		@Test
		@DisplayName("境界値：適用年月日（tekiyoYmd）が空文字の場合に空文字として処理されPDF生成処理が実行されること")
		void boundaryTekiyoYmdIsEmpty() {
			TokureiShiteiCancelDto dto = new TokureiShiteiCancelDto();
			dto.setHakkoYmd(LocalDate.of(2026, 4, 1));
			dto.setTekiyoYmd("");

			byte[] resultPdf = reportsService.generateTsuchiPdf(dto);

			assertThat(resultPdf).isNotNull();
			assertThat(resultPdf).isNotEmpty();
		}

		@Test
		@DisplayName("境界値：条例・市区町村・理由などのテキスト項目がnullの場合に空文字として処理されること")
		void boundaryTextFieldsAreNull() {
			TokureiShiteiCancelDto dto = new TokureiShiteiCancelDto();
			dto.setHakkoYmd(LocalDate.of(2026, 4, 1));
			dto.setJorei(null);
			dto.setCity(null);
			dto.setRiyu(null);

			byte[] resultPdf = reportsService.generateTsuchiPdf(dto);

			assertThat(resultPdf).isNotNull();
			assertThat(resultPdf).isNotEmpty();
		}

		@Test
		@DisplayName("境界値：データソース用の各種DTO項目（郵便番号・住所・名称・指定番号・備考）がnullの場合に空文字として設定されること")
		void boundaryDataSourceFieldsAreNull() {
			TokureiShiteiCancelDto dto = new TokureiShiteiCancelDto();
			dto.setHakkoYmd(LocalDate.of(2026, 4, 1));
			dto.setTokuYubin(null);
			dto.setTokuJusho(null);
			dto.setTokuName(null);
			dto.setShisetsuYubin(null);
			dto.setShisetsuJusho(null);
			dto.setShisetsuName(null);
			dto.setShiteiNo(null);
			dto.setBiko(null);

			byte[] resultPdf = reportsService.generateTsuchiPdf(dto);

			assertThat(resultPdf).isNotNull();
			assertThat(resultPdf).isNotEmpty();
		}

		@Test
		@DisplayName("境界値：記章（koin）がnullまたは空配列の場合にnullとして設定されること")
		void boundaryKoinIsNullOrEmpty() {
			TokureiShiteiCancelDto dtoNull = new TokureiShiteiCancelDto();
			dtoNull.setHakkoYmd(LocalDate.of(2026, 4, 1));
			dtoNull.setKoin(null);

			byte[] resultPdfNull = reportsService.generateTsuchiPdf(dtoNull);
			assertThat(resultPdfNull).isNotNull();

			TokureiShiteiCancelDto dtoEmpty = new TokureiShiteiCancelDto();
			dtoEmpty.setHakkoYmd(LocalDate.of(2026, 4, 1));
			dtoEmpty.setKoin(new byte[0]);

			byte[] resultPdfEmpty = reportsService.generateTsuchiPdf(dtoEmpty);
			assertThat(resultPdfEmpty).isNotNull();
		}

		@Test
		@DisplayName("異常系：JasperReportsのコンパイルや処理中にエラーが発生した場合にRuntimeExceptionがスローされること")
		void exceptionHandlingThrowsRuntimeException() {
			TokureiShiteiCancelDto dto = new TokureiShiteiCancelDto();
			// 不正なフォーマットを渡してYearMonthのパースエラー等で例外を誘発する
			dto.setHakkoYmd(LocalDate.of(2026, 4, 1));
			dto.setTekiyoYmd("INVALID_DATE_FORMAT");

			assertThatThrownBy(() -> reportsService.generateTsuchiPdf(dto))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("PDF生成に失敗しました");
		}
	}
}
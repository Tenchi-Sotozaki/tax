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
		@DisplayName("境界値：DTO自体がnullの場合にエラーがスローされること")
		void boundaryDtoIsNull() {
			assertThatThrownBy(() -> reportsService.generateTsuchiPdf(null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("値が取得できませんでした。");
		}

		@Test
		@DisplayName("境界値：発行年月日（hakkoYmd）がnullの場合にエラーがスローされること")
		void boundaryHakkoYmdIsNull() {
			TokureiShiteiCancelDto dto = new TokureiShiteiCancelDto();
			dto.setHakkoYmd(null);
			dto.setTekiyoYmd("2026-05");

			assertThatThrownBy(() -> reportsService.generateTsuchiPdf(dto))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("発行年月日は必須です。");
		}

		@Test
		@DisplayName("境界値：適用年月日（tekiyoYmd）がnullの場合にエラーがスローされること")
		void boundaryTekiyoYmdIsNull() {
			TokureiShiteiCancelDto dto = new TokureiShiteiCancelDto();
			dto.setHakkoYmd(LocalDate.of(2026, 4, 1));
			dto.setTekiyoYmd(null);

			assertThatThrownBy(() -> reportsService.generateTsuchiPdf(dto))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("適用年月は必須です。");
		}

		@Test
		@DisplayName("境界値：適用年月日（tekiyoYmd）が空文字の場合にエラーがスローされること")
		void boundaryTekiyoYmdIsEmpty() {
			TokureiShiteiCancelDto dto = new TokureiShiteiCancelDto();
			dto.setHakkoYmd(LocalDate.of(2026, 4, 1));
			dto.setTekiyoYmd("");

			assertThatThrownBy(() -> reportsService.generateTsuchiPdf(dto))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("適用年月は必須です。");
		}

		@Test
		@DisplayName("境界値：テキスト項目（jorei, city, riyu）のいずれかがnullの場合にエラーがスローされること")
		void boundaryTextFieldsAreNull() {
			// joreiがnull
			TokureiShiteiCancelDto dto1 = createValidDto();
			dto1.setJorei(null);
			assertThatThrownBy(() -> reportsService.generateTsuchiPdf(dto1))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("帳票出力項目が設定されていません。管理者にお問い合わせください。");

			// cityがnull
			TokureiShiteiCancelDto dto2 = createValidDto();
			dto2.setCity(null);
			assertThatThrownBy(() -> reportsService.generateTsuchiPdf(dto2))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("帳票出力項目が設定されていません。管理者にお問い合わせください。");

			// riyuがnull
			TokureiShiteiCancelDto dto3 = createValidDto();
			dto3.setRiyu(null);
			assertThatThrownBy(() -> reportsService.generateTsuchiPdf(dto3))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("帳票出力項目が設定されていません。管理者にお問い合わせください。");
		}

		@Test
		@DisplayName("境界値：データソース用の各種項目がnullの場合にエラーがスローされること")
		void boundaryDataSourceFieldsAreNull() {
			String[] fields = {"tokuYubin", "tokuJusho", "tokuName", "shisetsuYubin", "shisetsuJusho", "shisetsuName", "shiteiNo", "biko"};
			
			for (String field : fields) {
				TokureiShiteiCancelDto dto = createValidDto();
				if ("tokuYubin".equals(field)) dto.setTokuYubin(null);
				if ("tokuJusho".equals(field)) dto.setTokuJusho(null);
				if ("tokuName".equals(field)) dto.setTokuName(null);
				if ("shisetsuYubin".equals(field)) dto.setShisetsuYubin(null);
				if ("shisetsuJusho".equals(field)) dto.setShisetsuJusho(null);
				if ("shisetsuName".equals(field)) dto.setShisetsuName(null);
				if ("shiteiNo".equals(field)) dto.setShiteiNo(null);
				if ("biko".equals(field)) dto.setBiko(null);

				assertThatThrownBy(() -> reportsService.generateTsuchiPdf(dto))
						.isInstanceOf(IllegalArgumentException.class)
						.hasMessage("該当するデータが見つかりませんでした。");
			}
		}

		@Test
		@DisplayName("境界値：記章（koin）がnullまたは空配列の場合にエラーがスローされること")
		void boundaryKoinIsNullOrEmpty() {
			TokureiShiteiCancelDto dtoNull = createValidDto();
			dtoNull.setKoin(null);
			assertThatThrownBy(() -> reportsService.generateTsuchiPdf(dtoNull))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("公印が設定されていません。管理者にお問い合わせください。");

			TokureiShiteiCancelDto dtoEmpty = createValidDto();
			dtoEmpty.setKoin(new byte[0]);
			assertThatThrownBy(() -> reportsService.generateTsuchiPdf(dtoEmpty))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("公印が設定されていません。管理者にお問い合わせください。");
		}

		@Test
		@DisplayName("異常系：JasperReportsのコンパイルや処理中にエラーが発生した場合にRuntimeExceptionがスローされること")
		void exceptionHandlingThrowsRuntimeException() {
			TokureiShiteiCancelDto dto = createValidDto();
			dto.setTekiyoYmd("INVALID_DATE_FORMAT");

			assertThatThrownBy(() -> reportsService.generateTsuchiPdf(dto))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("PDF生成に失敗しました");
		}

		/**
		 * テスト用の有効なDTOを生成するヘルパーメソッド
		 */
		private TokureiShiteiCancelDto createValidDto() {
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
			return dto;
		}
	}
}
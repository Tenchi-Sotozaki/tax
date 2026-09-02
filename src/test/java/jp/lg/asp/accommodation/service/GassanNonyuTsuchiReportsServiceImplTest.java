package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.InputStream;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jp.lg.asp.accommodation.dto.GassanNonyuTsuchiDto;
import jp.lg.asp.accommodation.service.impl.GassanNonyuTsuchiReportsServiceImpl;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GassanNonyuTsuchiReportsServiceImplTest {

	@InjectMocks
	private GassanNonyuTsuchiReportsServiceImpl reportsService;

	private static final String SHITEI_NO = "S001";

	@Nested
	@DisplayName("generateTsuchiPdf メソッドのテスト")
	class GenerateTsuchiPdfTest {

		@Test
		@DisplayName("正常系：すべてのDTO項目が設定されている場合にPDFが正常に生成されること")
		void success_fullData() {
			GassanNonyuTsuchiDto dto = new GassanNonyuTsuchiDto();
			dto.setShiteiNo(SHITEI_NO);
			dto.setHakkoYmd(LocalDate.of(2026, 4, 1));
			dto.setJorei("テスト条例");
			dto.setCity("テスト市");
			dto.setBiko("テスト備考");
			dto.setNonyuKigen("6月30日");
			dto.setTokuJusho("〒1234567\r\nテスト住所");
			dto.setTokuName("テスト宛名");
			dto.setGassanShiteiNo("GS001");
			dto.setKoin(new byte[] { 1, 2, 3 });
			dto.setTekiyoStYmd(LocalDate.of(2026, 5, 1));

			byte[] expectedPdf = new byte[] { 10, 20, 30 };

			JasperReport mockReport = mock(JasperReport.class);
			JasperPrint mockPrint = mock(JasperPrint.class);

			try (MockedStatic<JasperCompileManager> compileMock = Mockito.mockStatic(JasperCompileManager.class);
					MockedStatic<JasperFillManager> fillMock = Mockito.mockStatic(JasperFillManager.class);
					MockedStatic<JasperExportManager> exportMock = Mockito.mockStatic(JasperExportManager.class)) {

				compileMock.when(() -> JasperCompileManager.compileReport(any(InputStream.class)))
						.thenReturn(mockReport);
				fillMock.when(() -> JasperFillManager.fillReport(eq(mockReport), anyMap(), any(JRDataSource.class)))
						.thenReturn(mockPrint);
				exportMock.when(() -> JasperExportManager.exportReportToPdf(mockPrint))
						.thenReturn(expectedPdf);

				byte[] result = reportsService.generateTsuchiPdf(dto);

				assertThat(result).isEqualTo(expectedPdf);
			}
		}

		@Test
		@DisplayName("異常系：DTOの各項目がnullまたは空の場合にエラーメッセージを返して例外がスローされること")
		void error_nullOrEmptyFields() {
			GassanNonyuTsuchiDto dto = new GassanNonyuTsuchiDto();
			dto.setShiteiNo(SHITEI_NO);
			dto.setHakkoYmd(null);
			dto.setJorei(null);
			dto.setCity(null);
			dto.setBiko(null);
			dto.setNonyuKigen(null);
			dto.setTokuJusho(null);
			dto.setTokuName(null);
			dto.setGassanShiteiNo(null);
			dto.setKoin(null);
			dto.setTekiyoStYmd(null);

			assertThatThrownBy(() -> reportsService.generateTsuchiPdf(dto))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("必須項目が設定されていません");
		}

		@Test
		@DisplayName("異常系：公印（koin）のバイト配列が長さ0の場合に公印未設定エラーとして例外がスローされること")
		void error_emptyKoin() {
			GassanNonyuTsuchiDto dto = new GassanNonyuTsuchiDto();
			dto.setShiteiNo(SHITEI_NO);
			dto.setHakkoYmd(LocalDate.of(2026, 4, 1));
			dto.setJorei("テスト条例");
			dto.setCity("テスト市");
			dto.setBiko("テスト備考");
			dto.setNonyuKigen("6月30日");
			dto.setTokuJusho("〒1234567\r\nテスト住所");
			dto.setTokuName("テスト宛名");
			dto.setGassanShiteiNo("GS001");
			dto.setKoin(new byte[0]); // 長さ0の配列
			dto.setTekiyoStYmd(LocalDate.of(2026, 5, 1));

			assertThatThrownBy(() -> reportsService.generateTsuchiPdf(dto))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("公印が設定されていません");
		}

		@Test
		@DisplayName("異常系：JasperReports処理中に例外が発生した場合にRuntimeExceptionにラップされてスローされること")
		void error_jasperReportsException() {
			GassanNonyuTsuchiDto dto = new GassanNonyuTsuchiDto();
			dto.setShiteiNo(SHITEI_NO);
			dto.setHakkoYmd(LocalDate.of(2026, 4, 1));
			dto.setJorei("テスト条例");
			dto.setCity("テスト市");
			dto.setBiko("テスト備考");
			dto.setNonyuKigen("6月30日");
			dto.setTokuJusho("〒1234567\r\nテスト住所");
			dto.setTokuName("テスト宛名");
			dto.setGassanShiteiNo("GS001");
			dto.setKoin(new byte[] { 1, 2, 3 });
			dto.setTekiyoStYmd(LocalDate.of(2026, 5, 1));

			try (MockedStatic<JasperCompileManager> compileMock = Mockito.mockStatic(JasperCompileManager.class)) {
				compileMock.when(() -> JasperCompileManager.compileReport(any(InputStream.class)))
						.thenThrow(new RuntimeException("Compile error"));

				assertThatThrownBy(() -> reportsService.generateTsuchiPdf(dto))
						.isInstanceOf(RuntimeException.class)
						.hasMessageContaining("PDF生成に失敗しました");
			}
		}
	}
}
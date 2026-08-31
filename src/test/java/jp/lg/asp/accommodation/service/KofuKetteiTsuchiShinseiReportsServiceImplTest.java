package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.KofuKetteiTsuchiShinseiDto;
import jp.lg.asp.accommodation.repository.ReportsLogRepository;
import jp.lg.asp.accommodation.repository.RptStatusRepository;
import jp.lg.asp.accommodation.service.impl.KofuKetteiTsuchiShinseiReportsServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KofuKetteiTsuchiShinseiReportsServiceImplTest {

	@InjectMocks
	private KofuKetteiTsuchiShinseiReportsServiceImpl service;

	@Mock
	private ReportsLogRepository reportsLogRepository;

	@Mock
	private RptStatusRepository rptStatusRepository;

	@Mock
	private JichitaiContext jichitaiContext;

	private static final String JICHITAI_CD = "123456";
	private static final String SHITEI_NO = "S001";

	@BeforeEach
	void setUp() {
		when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
		when(reportsLogRepository.findNextSeq(JICHITAI_CD)).thenReturn(1L);
		when(rptStatusRepository.findByJichitaiCdAndShiteiNoAndRptId(any(), any(), any()))
				.thenReturn(Optional.empty());

		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("testUser", "password", Collections.emptyList())
		);
	}

	@Nested
	@DisplayName("generatekofuKetteiTsuchiShinseiPdf メソッドのテスト")
	class GenerateSinglePdfTest {

		@Test
		@DisplayName("正常系：JRXMLファイルが存在し有効なデータの場合にPDFのバイト配列が返却されること")
		void success_singlePdf() {
			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setShiteiNo(SHITEI_NO);
			dto.setOperation("PRINT");
			dto.setKetteiTsuchi(true);
			dto.setShinsei(false);
			dto.setBankCd("123");

			byte[] result = service.generatekofuKetteiTsuchiShinseiPdf(dto);

			assertThat(result).isNotNull();
		}

		@Test
		@DisplayName("境界値：印刷対象フラグが両方ともfalseの場合に例外がスローされること")
		void error_bothFlagsFalse() {
			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setKetteiTsuchi(false);
			dto.setShinsei(false);

			assertThatThrownBy(() -> service.generatekofuKetteiTsuchiShinseiPdf(dto))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("印刷対象が選択されていません。");
		}

		@Test
		@DisplayName("正常系：ログ保存時に例外が発生した場合でも、内部でキャッチされ正常にPDF生成処理が継続・完了すること")
		void success_saveLogExceptionHandling() {
			when(reportsLogRepository.findNextSeq(any())).thenThrow(new RuntimeException("DB Error"));

			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setShiteiNo(SHITEI_NO);
			dto.setOperation("PRINT");
			dto.setKetteiTsuchi(true);
			dto.setShinsei(false);
			dto.setBankCd("123");

			// saveLog内の例外はcatchされるため、PDF生成が成功することを検証
			byte[] result = service.generatekofuKetteiTsuchiShinseiPdf(dto);
			assertThat(result).isNotNull();
		}

		@Test
		@DisplayName("正常系（匿名ユーザー）：認証情報がnullの場合でも匿名ユーザーとして処理されPDFが生成されること")
		void success_anonymousUser() {
			SecurityContextHolder.clearContext();

			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setShiteiNo(SHITEI_NO);
			dto.setOperation("PRINT");
			dto.setKetteiTsuchi(true);
			dto.setShinsei(false);
			dto.setBankCd("123");

			byte[] result = service.generatekofuKetteiTsuchiShinseiPdf(dto);
			assertThat(result).isNotNull();
		}
	}

	@Nested
	@DisplayName("generateBulkPdf メソッドのテスト")
	class GenerateBulkPdfTest {

		@Test
		@DisplayName("境界値：引数のDTOリストがnullまたは空の場合に例外がスローされること")
		void error_dtoListNullOrEmpty() {
			assertThatThrownBy(() -> service.generateBulkPdf(null))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("帳票データがありません。");

			assertThatThrownBy(() -> service.generateBulkPdf(Collections.emptyList()))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("帳票データがありません。");
		}

		@Test
		@DisplayName("異常系：リスト内の全要素のフラグがfalseの場合に例外がスローされること")
		void error_allFlagsFalseInList() {
			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setKetteiTsuchi(false);
			dto.setShinsei(false);

			assertThatThrownBy(() -> service.generateBulkPdf(List.of(dto)))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("印刷対象がありません。");
		}

		@Test
		@DisplayName("正常系：一括生成時に有効なリストが渡された場合にPDFのバイト配列が返却されること")
		void success_generateBulkPdf() {
			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setKetteiTsuchi(true);
			dto.setShinsei(true);
			dto.setBankCd("123");
			dto.setShiteiNo(SHITEI_NO);
			dto.setOperation("PRINT");

			byte[] result = service.generateBulkPdf(List.of(dto));

			assertThat(result).isNotNull();
		}
	}
}
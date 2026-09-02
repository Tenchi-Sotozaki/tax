package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
		when(reportsLogRepository.findNextSeq(anyString())).thenReturn(1L);
		when(rptStatusRepository.findByJichitaiCdAndShiteiNoAndRptId(anyString(), anyString(), anyString()))
				.thenReturn(Optional.empty());
		
		setValidAuthentication();
	}

	private void setValidAuthentication() {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("testUser", "password", Collections.emptyList()));
	}

	@Nested
	@DisplayName("generatekofuKetteiTsuchiShinseiPdf メソッドのテスト")
	class GeneratePdfTest {

		@Test
		@DisplayName("異常系：認証情報がnullまたは未認証の場合にAccessDeniedExceptionがスローされること")
		void error_unauthenticatedUser() {
			SecurityContextHolder.clearContext();

			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setShiteiNo(SHITEI_NO);
			dto.setKetteiTsuchi(true);
			dto.setShinsei(false);

			assertThatThrownBy(() -> service.generatekofuKetteiTsuchiShinseiPdf(dto))
					.isInstanceOf(AccessDeniedException.class);
		}

		@Test
		@DisplayName("異常系：印刷対象が選択されていない場合にRuntimeExceptionがスローされること")
		void error_targetNotSelected() {
			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setShiteiNo(SHITEI_NO);
			dto.setKetteiTsuchi(false);
			dto.setShinsei(false);

			assertThatThrownBy(() -> service.generatekofuKetteiTsuchiShinseiPdf(dto))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("印刷対象が選択されていません。");
		}

		@Test
		@DisplayName("正常系：決定通知書のみ選択された場合にPDFが生成されること")
		void success_ketteiOnly() {
			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setShiteiNo(SHITEI_NO);
			dto.setKetteiTsuchi(true);
			dto.setShinsei(false);
			dto.setOperation("PRINT");

			byte[] result = service.generatekofuKetteiTsuchiShinseiPdf(dto);
			assertThat(result).isNotNull();
		}

		@Test
		@DisplayName("正常系：交付申請書のみ選択された場合にPDFが生成されること")
		void success_shinseiOnly() {
			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setShiteiNo(SHITEI_NO);
			dto.setKetteiTsuchi(false);
			dto.setShinsei(true);
			dto.setBankCd("123"); // JasperReportsの評価用エラー回避
			dto.setOperation("PRINT");

			byte[] result = service.generatekofuKetteiTsuchiShinseiPdf(dto);
			assertThat(result).isNotNull();
		}

		@Test
		@DisplayName("正常系：両方選択された場合にPDFが生成されること")
		void success_bothSelected() {
			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setShiteiNo(SHITEI_NO);
			dto.setKetteiTsuchi(true);
			dto.setShinsei(true);
			dto.setBankCd("123"); // JasperReportsの評価用エラー回避
			dto.setOperation("PRINT");

			byte[] result = service.generatekofuKetteiTsuchiShinseiPdf(dto);
			assertThat(result).isNotNull();
		}

		@Test
		@DisplayName("正常系：ログ保存時に例外が発生した場合でもキャッチされて正常終了すること（getCurrentUserIdの認証nullも含めてカバー）")
		void success_whenLogSaveFailsAndAuthNull() {
			Authentication auth = mock(Authentication.class);
			when(auth.isAuthenticated()).thenReturn(true);
			when(auth.getPrincipal()).thenReturn("testUser");
			when(auth.getName()).thenReturn(null);
			SecurityContextHolder.getContext().setAuthentication(auth);

			doThrow(new RuntimeException("DB Error")).when(reportsLogRepository).save(any());

			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setShiteiNo(SHITEI_NO);
			dto.setKetteiTsuchi(true);
			dto.setShinsei(false);
			dto.setOperation("PRINT");

			byte[] result = service.generatekofuKetteiTsuchiShinseiPdf(dto);
			assertThat(result).isNotNull();
		}
	}

	@Nested
	@DisplayName("generateBulkPdf メソッドのテスト")
	class GenerateBulkPdfTest {

		@Test
		@DisplayName("異常系：認証情報が未認証の場合にAccessDeniedExceptionがスローされること")
		void error_unauthenticatedUser() {
			SecurityContextHolder.clearContext();

			List<KofuKetteiTsuchiShinseiDto> dtoList = Arrays.asList(new KofuKetteiTsuchiShinseiDto());

			assertThatThrownBy(() -> service.generateBulkPdf(dtoList))
					.isInstanceOf(AccessDeniedException.class);
		}

		@Test
		@DisplayName("異常系：一括帳票データがnullまたは空の場合に例外がスローされること")
		void error_dtoListIsNull() {
			assertThatThrownBy(() -> service.generateBulkPdf(null))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("帳票データがありません。");
		}

		@Test
		@DisplayName("異常系：一括帳票データのリストが空の場合に例外がスローされること")
		void error_dtoListIsEmpty() {
			assertThatThrownBy(() -> service.generateBulkPdf(Collections.emptyList()))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("帳票データがありません。");
		}

		@Test
		@DisplayName("異常系：一括リスト内の全要素のフラグがfalseで印刷対象がない場合に例外がスローされること")
		void error_allFlagsFalseInList() {
			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setKetteiTsuchi(false);
			dto.setShinsei(false);

			assertThatThrownBy(() -> service.generateBulkPdf(Arrays.asList(dto)))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("印刷対象がありません。");
		}

		@Test
		@DisplayName("正常系：一括生成時に交付申請書フラグが有効なリストが渡された場合にPDFが返却されること")
		void success_bulkPdfShinseiGeneration() {
			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setShiteiNo(SHITEI_NO);
			dto.setKetteiTsuchi(false);
			dto.setShinsei(true);
			dto.setBankCd("123"); // JasperReportsの評価用エラー回避
			dto.setOperation("PRINT");

			byte[] result = service.generateBulkPdf(Arrays.asList(dto));
			assertThat(result).isNotNull();
		}
	}
}
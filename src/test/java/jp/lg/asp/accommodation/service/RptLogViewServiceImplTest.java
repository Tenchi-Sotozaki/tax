package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.RptLogViewDto;
import jp.lg.asp.accommodation.entity.Reports;
import jp.lg.asp.accommodation.entity.ReportsLog;
import jp.lg.asp.accommodation.repository.ReportsLogRepository;
import jp.lg.asp.accommodation.repository.ReportsRepository;
import jp.lg.asp.accommodation.service.impl.RptLogViewServiceImpl;

@ExtendWith(MockitoExtension.class)
class RptLogViewServiceImplTest {

	@InjectMocks
	private RptLogViewServiceImpl rptLogViewService;

	@Mock
	private ReportsRepository reportsRepository;

	@Mock
	private ReportsLogRepository reportsLogRepository;

	@Mock
	private JichitaiContext jichitaiContext;

	private static final String JICHITAI_CD = "123456";



	@Nested
	@DisplayName("search メソッドのテスト")
	class SearchTest {

		@Test
		@DisplayName("正常系：条件に一致するログが存在し、帳票名・操作名が正常に解決されてDTOリストが返却されること")
		void success() {
			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
			RptLogViewDto form = new RptLogViewDto();

			ReportsLog log = new ReportsLog();
			log.setSeq(1L);
			log.setRptId("RPT001");
			log.setSousa("1");
			log.setOpeUser("user1");
			log.setOpeDt(LocalDateTime.now());
			log.setShiteiNo("S01");

			Reports report = new Reports();
			report.setRptId("RPT001");
			report.setRptName("テスト帳票");

			when(reportsLogRepository.findByConditions(eq(JICHITAI_CD), any(), any(), any(), any(), any(), any()))
					.thenReturn(List.of(log));
			when(reportsRepository.findAll()).thenReturn(List.of(report));

			List<RptLogViewDto> results = rptLogViewService.search(form);

			assertThat(results).hasSize(1);
			assertThat(results.get(0).getRptName()).isEqualTo("テスト帳票");
			assertThat(results.get(0).getSousaName()).isNotNull();
		}

		@Test
		@DisplayName("境界値：検索条件に一致するログが0件の場合、空のリストが返却されること")
		void emptyLogs() {
			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
			RptLogViewDto form = new RptLogViewDto();

			when(reportsLogRepository.findByConditions(eq(JICHITAI_CD), any(), any(), any(), any(), any(), any()))
					.thenReturn(List.of());
			when(reportsRepository.findAll()).thenReturn(List.of());

			List<RptLogViewDto> results = rptLogViewService.search(form);

			assertThat(results).isEmpty();
		}

		@Test
		@DisplayName("境界値：ログの rptId が null の場合、帳票名が空文字に解決されること")
		void rptIdNull() {
			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
			RptLogViewDto form = new RptLogViewDto();

			ReportsLog log = new ReportsLog();
			log.setRptId(null);
			log.setSousa("1");

			when(reportsLogRepository.findByConditions(eq(JICHITAI_CD), any(), any(), any(), any(), any(), any()))
					.thenReturn(List.of(log));
			when(reportsRepository.findAll()).thenReturn(List.of());

			List<RptLogViewDto> results = rptLogViewService.search(form);

			assertThat(results).hasSize(1);
			assertThat(results.get(0).getRptName()).isEqualTo("");
		}

		@Test
		@DisplayName("境界値：ログの rptId に一致する帳票定義が存在しない場合、IDがそのままフォールバックされること")
		void rptIdNotFound() {
			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
			RptLogViewDto form = new RptLogViewDto();

			ReportsLog log = new ReportsLog();
			log.setRptId("UNKNOWN");
			log.setSousa("1");

			when(reportsLogRepository.findByConditions(eq(JICHITAI_CD), any(), any(), any(), any(), any(), any()))
					.thenReturn(List.of(log));
			when(reportsRepository.findAll()).thenReturn(List.of());

			List<RptLogViewDto> results = rptLogViewService.search(form);

			assertThat(results).hasSize(1);
			assertThat(results.get(0).getRptName()).isEqualTo("UNKNOWN");
		}
	}

	@Nested
	@DisplayName("findAllReports メソッドのテスト")
	class FindAllReportsTest {

		@Test
		@DisplayName("正常系：全帳票定義が返却されること")
		void success() {
			Reports r1 = new Reports();
			r1.setRptId("R1");

			Reports r2 = new Reports();
			r2.setRptId("R2");

			when(reportsRepository.findAll()).thenReturn(List.of(r1, r2));

			List<Reports> results = rptLogViewService.findAllReports();

			assertThat(results).hasSize(2);
			assertThat(results.get(0).getRptId()).isEqualTo("R1");
			assertThat(results.get(1).getRptId()).isEqualTo("R2");
		}

		@Test
		@DisplayName("境界値：帳票定義が存在しない場合、空のリストが返却されること")
		void empty() {
			when(reportsRepository.findAll()).thenReturn(List.of());

			List<Reports> results = rptLogViewService.findAllReports();

			assertThat(results).isEmpty();
		}
	}
}
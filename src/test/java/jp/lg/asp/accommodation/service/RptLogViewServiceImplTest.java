package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
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

    @Mock ReportsRepository reportsRepository;
    @Mock ReportsLogRepository reportsLogRepository;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks RptLogViewServiceImpl service;

    private static final String JICHITAI_CD = "011002";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    @Test
    void search_mapsLogToDto() {
        ReportsLog log = new ReportsLog();
        log.setSeq(1L);
        log.setRptId("RPT001");
        log.setSousa("1");
        log.setOpeUser("user01");
        log.setShiteiNo("00000001");

        Reports reports = new Reports();
        reports.setRptId("RPT001");
        reports.setRptName("特別徴収義務者指定通知書");
        reports.setJichitaiCd(JICHITAI_CD);

        when(reportsLogRepository.findByConditions(eq(JICHITAI_CD), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(log));
        when(reportsRepository.findAll()).thenReturn(List.of(reports));

        RptLogViewDto form = new RptLogViewDto();
        List<RptLogViewDto> result = service.search(form);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRptId()).isEqualTo("RPT001");
        assertThat(result.get(0).getRptName()).isEqualTo("特別徴収義務者指定通知書");
    }

    @Test
    void search_unknownRptId_fallsBackToId() {
        ReportsLog log = new ReportsLog();
        log.setRptId("UNKNOWN");

        when(reportsLogRepository.findByConditions(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(log));
        when(reportsRepository.findAll()).thenReturn(List.of());

        List<RptLogViewDto> result = service.search(new RptLogViewDto());

        assertThat(result.get(0).getRptName()).isEqualTo("UNKNOWN");
    }

    @Test
    void findAllReports_filtersbyJichitaiCd() {
        Reports r1 = new Reports();
        r1.setRptId("RPT001");
        r1.setJichitaiCd(JICHITAI_CD);

        Reports r2 = new Reports();
        r2.setRptId("RPT002");
        r2.setJichitaiCd("999999");

        when(reportsRepository.findAll()).thenReturn(List.of(r1, r2));

        List<Reports> result = service.findAllReports();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRptId()).isEqualTo("RPT001");
    }
}

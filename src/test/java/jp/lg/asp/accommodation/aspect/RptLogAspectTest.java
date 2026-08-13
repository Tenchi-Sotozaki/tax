package jp.lg.asp.accommodation.aspect;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import jp.lg.asp.accommodation.annotation.RptLog;
import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.entity.ReportsLog;
import jp.lg.asp.accommodation.entity.RptStatus;
import jp.lg.asp.accommodation.repository.ReportsLogRepository;
import jp.lg.asp.accommodation.repository.RptStatusRepository;

@ExtendWith(MockitoExtension.class)
class RptLogAspectTest {

    @Mock ReportsLogRepository reportsLogRepository;
    @Mock RptStatusRepository rptStatusRepository;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks RptLogAspect aspect;

    @Mock ProceedingJoinPoint joinPoint;
    @Mock MethodSignature methodSignature;
    @Mock RptLog rptLog;

    private static final String JICHITAI_CD = "01100";

    @BeforeEach
    void setUp() {
        lenient().when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        lenient().when(reportsLogRepository.findNextSeq(JICHITAI_CD)).thenReturn(1L);
        lenient().when(joinPoint.getSignature()).thenReturn(methodSignature);
        lenient().when(methodSignature.getParameterNames()).thenReturn(new String[]{});
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{});
        lenient().when(rptLog.rptId()).thenReturn("RPT001");
        lenient().when(rptLog.operation()).thenReturn("1");
        lenient().when(rptLog.shiteiNo()).thenReturn("");
        lenient().when(rptStatusRepository.findByJichitaiCdAndShiteiNoAndRptId(any(), any(), any()))
                .thenReturn(Optional.empty());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testUser", "password"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logOperation_正常時_ログ保存してresultを返す() throws Throwable {
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.logOperation(joinPoint, rptLog);

        assertThat(result).isEqualTo("result");
        verify(reportsLogRepository).save(any(ReportsLog.class));
        verify(rptStatusRepository).save(any(RptStatus.class));
    }

    @Test
    void logOperation_例外時_ログ保存して例外を再スロー() throws Throwable {
        RuntimeException ex = new RuntimeException("error");
        when(joinPoint.proceed()).thenThrow(ex);

        assertThatThrownBy(() -> aspect.logOperation(joinPoint, rptLog))
                .isSameAs(ex);
        verify(reportsLogRepository).save(any(ReportsLog.class));
        verify(rptStatusRepository).save(any(RptStatus.class));
    }

    @Test
    void saveLog_ReportsLogに正しい値がセットされる() throws Throwable {
        when(joinPoint.proceed()).thenReturn(null);

        aspect.logOperation(joinPoint, rptLog);

        ArgumentCaptor<ReportsLog> captor = ArgumentCaptor.forClass(ReportsLog.class);
        verify(reportsLogRepository).save(captor.capture());
        ReportsLog saved = captor.getValue();
        assertThat(saved.getJichitaiCd()).isEqualTo(JICHITAI_CD);
        assertThat(saved.getSeq()).isEqualTo(1L);
        assertThat(saved.getRptId()).isEqualTo("RPT001");
        assertThat(saved.getSousa()).isEqualTo("1");
        assertThat(saved.getOpeUser()).isEqualTo("testUser");
        assertThat(saved.getOpeDt()).isNotNull();
    }

    @Test
    void saveLog_RptStatusが存在しない場合_新規作成して保存() throws Throwable {
        when(rptLog.shiteiNo()).thenReturn("#shiteiNo");
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"shiteiNo"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"12345678"});
        when(rptStatusRepository.findByJichitaiCdAndShiteiNoAndRptId(JICHITAI_CD, "12345678", "RPT001"))
                .thenReturn(Optional.empty());
        when(joinPoint.proceed()).thenReturn(null);

        aspect.logOperation(joinPoint, rptLog);

        ArgumentCaptor<RptStatus> captor = ArgumentCaptor.forClass(RptStatus.class);
        verify(rptStatusRepository).save(captor.capture());
        RptStatus saved = captor.getValue();
        assertThat(saved.getJichitaiCd()).isEqualTo(JICHITAI_CD);
        assertThat(saved.getShiteiNo()).isEqualTo("12345678");
        assertThat(saved.getRptId()).isEqualTo("RPT001");
        assertThat(saved.getCreateDt()).isNotNull();
    }

    @Test
    void saveLog_RptStatusが既存の場合_既存エンティティを更新して保存() throws Throwable {
        when(rptLog.shiteiNo()).thenReturn("#shiteiNo");
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"shiteiNo"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"12345678"});
        RptStatus existing = new RptStatus();
        existing.setJichitaiCd(JICHITAI_CD);
        existing.setShiteiNo("12345678");
        existing.setRptId("RPT001");
        when(rptStatusRepository.findByJichitaiCdAndShiteiNoAndRptId(JICHITAI_CD, "12345678", "RPT001"))
                .thenReturn(Optional.of(existing));
        when(joinPoint.proceed()).thenReturn(null);

        aspect.logOperation(joinPoint, rptLog);

        verify(rptStatusRepository).save(same(existing));
    }

    @Test
    void resolveShiteiNo_shiteiNoが空文字の場合_nullを返す() throws Throwable {
        when(rptLog.shiteiNo()).thenReturn("");
        when(joinPoint.proceed()).thenReturn(null);

        aspect.logOperation(joinPoint, rptLog);

        ArgumentCaptor<ReportsLog> captor = ArgumentCaptor.forClass(ReportsLog.class);
        verify(reportsLogRepository).save(captor.capture());
        assertThat(captor.getValue().getShiteiNo()).isNull();
    }

    @Test
    void resolveShiteiNo_SpEL評価失敗時_nullを返す() throws Throwable {
        when(rptLog.shiteiNo()).thenReturn("#nonExistentVar.shiteiNo");
        when(methodSignature.getParameterNames()).thenReturn(new String[]{});
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenReturn(null);

        aspect.logOperation(joinPoint, rptLog);

        ArgumentCaptor<ReportsLog> captor = ArgumentCaptor.forClass(ReportsLog.class);
        verify(reportsLogRepository).save(captor.capture());
        assertThat(captor.getValue().getShiteiNo()).isNull();
    }

    @Test
    void getCurrentUserId_認証なしの場合_anonymousを返す() throws Throwable {
        SecurityContextHolder.clearContext();
        when(joinPoint.proceed()).thenReturn(null);

        aspect.logOperation(joinPoint, rptLog);

        ArgumentCaptor<ReportsLog> captor = ArgumentCaptor.forClass(ReportsLog.class);
        verify(reportsLogRepository).save(captor.capture());
        assertThat(captor.getValue().getOpeUser()).isEqualTo("anonymous");
    }

    @Test
    void saveLog_リポジトリ例外時_例外を握りつぶしてproceedのresultを返す() throws Throwable {
        when(reportsLogRepository.findNextSeq(any())).thenThrow(new RuntimeException("DB error"));
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.logOperation(joinPoint, rptLog);

        assertThat(result).isEqualTo("ok");
    }
}

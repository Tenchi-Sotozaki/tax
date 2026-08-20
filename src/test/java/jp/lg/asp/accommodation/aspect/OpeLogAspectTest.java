package jp.lg.asp.accommodation.aspect;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.entity.OperationLog;
import jp.lg.asp.accommodation.repository.OperationLogRepository;

@ExtendWith(MockitoExtension.class)
class OpeLogAspectTest {

    @Mock OperationLogRepository operationLogRepository;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks OpeLogAspect aspect;

    @Mock ProceedingJoinPoint joinPoint;
    @Mock OpeLog opeLog;

    private static final String JICHITAI_CD = "01100";

    // ObjectMapperは実インスタンスを使用
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        try {
            var field = OpeLogAspect.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            field.set(aspect, objectMapper);
        } catch (Exception e) {
            fail("初期化処理に失敗しました", e);
        }

        lenient().when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        lenient().when(operationLogRepository.findNextSeq(JICHITAI_CD)).thenReturn(1L);
        lenient().when(opeLog.screenId()).thenReturn("SCR001");
        lenient().when(opeLog.operation()).thenReturn("登録");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testUser", "password"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void logOperation_正常時_ログ保存してresultを返す() throws Throwable {
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.logOperation(joinPoint, opeLog);

        assertThat(result).isEqualTo("result");
        verify(operationLogRepository).save(any(OperationLog.class));
    }

    @Test
    void logOperation_例外時_ログ保存して例外を再スロー() throws Throwable {
        RuntimeException ex = new RuntimeException("error");
        when(joinPoint.proceed()).thenThrow(ex);

        assertThatThrownBy(() -> aspect.logOperation(joinPoint, opeLog))
                .isSameAs(ex);
        verify(operationLogRepository).save(any(OperationLog.class));
    }

    @Test
    void saveLog_OperationLogに正しい値がセットされる() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test/path");
        request.addParameter("key1", "value1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(joinPoint.proceed()).thenReturn(null);

        aspect.logOperation(joinPoint, opeLog);

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogRepository).save(captor.capture());
        OperationLog saved = captor.getValue();
        assertThat(saved.getJichitaiCd()).isEqualTo(JICHITAI_CD);
        assertThat(saved.getSeq()).isEqualTo(1L);
        assertThat(saved.getScreenId()).isEqualTo("SCR001");
        assertThat(saved.getSousa()).isEqualTo("登録");
        assertThat(saved.getMethod()).isEqualTo("POST");
        assertThat(saved.getPath()).isEqualTo("/test/path");
        assertThat(saved.getStatus()).isEqualTo("200");
        assertThat(saved.getOpeUser()).isEqualTo("testUser");
        assertThat(saved.getOpeDt()).isNotNull();
    }

    @Test
    void saveLog_requestがnullの場合_method_path_paramがnull() throws Throwable {
        // RequestContextHolderをリセット（requestなし）
        RequestContextHolder.resetRequestAttributes();
        when(joinPoint.proceed()).thenReturn(null);

        aspect.logOperation(joinPoint, opeLog);

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogRepository).save(captor.capture());
        OperationLog saved = captor.getValue();
        assertThat(saved.getMethod()).isNull();
        assertThat(saved.getPath()).isNull();
        assertThat(saved.getParam()).isNull();
    }

    @Test
    void saveLog_csrfパラメータは除外される() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/path");
        request.addParameter("_csrf", "token123");
        request.addParameter("name", "value");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(joinPoint.proceed()).thenReturn(null);

        aspect.logOperation(joinPoint, opeLog);

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getParam()).doesNotContain("_csrf").contains("name");
    }

    @Test
    void saveLog_paramが2000文字超の場合_2000文字に切り詰める() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/path");
        request.addParameter("key", "a".repeat(3000));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(joinPoint.proceed()).thenReturn(null);

        aspect.logOperation(joinPoint, opeLog);

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getParam()).hasSizeLessThanOrEqualTo(2000);
    }

    @Test
    void getCurrentUserId_認証なしの場合_anonymousを返す() throws Throwable {
        SecurityContextHolder.clearContext();
        when(joinPoint.proceed()).thenReturn(null);

        aspect.logOperation(joinPoint, opeLog);

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getOpeUser()).isEqualTo("anonymous");
    }

    @Test
    void saveLog_リポジトリ例外時_例外を握りつぶしてproceedのresultを返す() throws Throwable {
        when(operationLogRepository.findNextSeq(any())).thenThrow(new RuntimeException("DB error"));
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.logOperation(joinPoint, opeLog);

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void saveLog_ObjectMapper例外時_paramがnullになる() throws Throwable {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any(Map.class))).thenThrow(new RuntimeException("json error"));
        try {
            var field = OpeLogAspect.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            field.set(aspect, failingMapper);
        } catch (Exception e) {
            fail("フィールド設定失敗", e);
        }

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/path");
        request.addParameter("key", "value");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(joinPoint.proceed()).thenReturn(null);

        aspect.logOperation(joinPoint, opeLog);

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getParam()).isNull();
    }
}

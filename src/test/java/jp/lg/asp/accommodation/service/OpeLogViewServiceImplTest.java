package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.OpeLogViewDto;
import jp.lg.asp.accommodation.entity.OperationLog;
import jp.lg.asp.accommodation.entity.Screen;
import jp.lg.asp.accommodation.repository.OperationLogRepository;
import jp.lg.asp.accommodation.repository.ScreenRepository;
import jp.lg.asp.accommodation.service.impl.OpeLogViewServiceImpl;

@ExtendWith(MockitoExtension.class)
class OpeLogViewServiceImplTest {

    @Mock ScreenRepository screenRepository;
    @Mock OperationLogRepository operationLogRepository;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks OpeLogViewServiceImpl service;

    private static final String JICHITAI_CD = "011002";

    private Screen screen(String screenId, String screenName) {
        Screen s = new Screen();
        s.setScreenId(screenId);
        s.setScreenName(screenName);
        return s;
    }

    private OperationLog log(Long seq, String screenId, String sousa, String opeUser, LocalDateTime opeDt) {
        OperationLog log = new OperationLog();
        log.setSeq(seq);
        log.setScreenId(screenId);
        log.setSousa(sousa);
        log.setOpeUser(opeUser);
        log.setOpeDt(opeDt);
        return log;
    }

    // ===== No.8: search 正常系 - 全条件指定・結果あり → 全フィールドがDTOにマッピングされる =====
    @Test
    void search_全条件指定_全フィールドがDTOにマッピングされる() {
        LocalDateTime opeDt = LocalDateTime.of(2024, 6, 1, 10, 0);
        OperationLog log = log(1L, "S001", "検索", "user01", opeDt);
        Screen screen = screen("S001", "操作ログ照会");

        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(operationLogRepository.findByConditions(eq(JICHITAI_CD), eq("S001"), eq("検索"), eq("user01"),
                eq("2024-01-01T00:00"), eq("2024-12-31T23:59"))).thenReturn(List.of(log));
        when(screenRepository.findAllByOrderByScreenIdAsc()).thenReturn(List.of(screen));

        OpeLogViewDto form = new OpeLogViewDto();
        form.setScreenId("S001");
        form.setSousa("検索");
        form.setOpeUser("user01");
        form.setOpeDtFrom("2024-01-01T00:00");
        form.setOpeDtTo("2024-12-31T23:59");

        List<OpeLogViewDto> results = service.search(form);

        assertThat(results).hasSize(1);
        OpeLogViewDto dto = results.get(0);
        assertThat(dto.getSeq()).isEqualTo(1L);
        assertThat(dto.getScreenId()).isEqualTo("S001");
        assertThat(dto.getScreenName()).isEqualTo("操作ログ照会");
        assertThat(dto.getSousa()).isEqualTo("検索");
        assertThat(dto.getOpeUser()).isEqualTo("user01");
        assertThat(dto.getOpeDt()).isEqualTo(opeDt);
    }

    // ===== No.9: search 正常系 - 全条件null → findByConditionsにnullが渡される =====
    @Test
    void search_全条件null_findByConditionsにnullが渡される() {
        OperationLog log = log(1L, "S001", "検索", "user01", null);
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(operationLogRepository.findByConditions(JICHITAI_CD, null, null, null, null, null))
                .thenReturn(List.of(log));
        when(screenRepository.findAllByOrderByScreenIdAsc()).thenReturn(List.of());

        List<OpeLogViewDto> results = service.search(new OpeLogViewDto());

        assertThat(results).hasSize(1);
        verify(operationLogRepository).findByConditions(JICHITAI_CD, null, null, null, null, null);
    }

    // ===== No.10: search 正常系 - 検索結果が0件 → 空リストを返す =====
    @Test
    void search_検索結果が0件_空リストを返す() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(operationLogRepository.findByConditions(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(screenRepository.findAllByOrderByScreenIdAsc()).thenReturn(List.of());

        assertThat(service.search(new OpeLogViewDto())).isEmpty();
    }

    // ===== No.11: search 正常系 - screenIdがマスタに一致 → screenNameが解決される =====
    @Test
    void search_screenIdがマスタに一致_screenNameが解決される() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(operationLogRepository.findByConditions(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(log(1L, "S001", "", "", null)));
        when(screenRepository.findAllByOrderByScreenIdAsc())
                .thenReturn(List.of(screen("S001", "操作ログ照会")));

        List<OpeLogViewDto> results = service.search(new OpeLogViewDto());

        assertThat(results.get(0).getScreenName()).isEqualTo("操作ログ照会");
    }

    // ===== No.12: search 正常系 - screenIdがマスタに不一致 → screenIdをそのままscreenNameに使用 =====
    @Test
    void search_screenIdがマスタに不一致_screenIdをscreenNameに使用() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(operationLogRepository.findByConditions(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(log(1L, "S999", "", "", null)));
        when(screenRepository.findAllByOrderByScreenIdAsc())
                .thenReturn(List.of(screen("S001", "操作ログ照会")));

        List<OpeLogViewDto> results = service.search(new OpeLogViewDto());

        assertThat(results.get(0).getScreenName()).isEqualTo("S999");
    }

    // ===== No.13: search 正常系 - log.screenIdがnull → screenNameが空文字 =====
    @Test
    void search_screenIdがnull_screenNameが空文字() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(operationLogRepository.findByConditions(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(log(1L, null, "", "", null)));
        when(screenRepository.findAllByOrderByScreenIdAsc()).thenReturn(List.of());

        List<OpeLogViewDto> results = service.search(new OpeLogViewDto());

        assertThat(results.get(0).getScreenName()).isEqualTo("");
    }

    // ===== No.14: search 正常系 - screenIdの前後に空白あり → strip()で一致してscreenNameが解決される =====
    @Test
    void search_screenIdの前後に空白あり_strip後に一致してscreenNameが解決される() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(operationLogRepository.findByConditions(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(log(1L, " S001 ", "", "", null)));
        when(screenRepository.findAllByOrderByScreenIdAsc())
                .thenReturn(List.of(screen("S001", "操作ログ照会")));

        List<OpeLogViewDto> results = service.search(new OpeLogViewDto());

        assertThat(results.get(0).getScreenName()).isEqualTo("操作ログ照会");
    }

    // ===== No.15: search 正常系 - 複数件ログあり → 全件DTOに変換される =====
    @Test
    void search_複数件ログあり_全件DTOに変換される() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(operationLogRepository.findByConditions(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(
                        log(1L, "S001", "", "", null),
                        log(2L, "S001", "", "", null),
                        log(3L, "S001", "", "", null)));
        when(screenRepository.findAllByOrderByScreenIdAsc()).thenReturn(List.of());

        assertThat(service.search(new OpeLogViewDto())).hasSize(3);
    }

    // ===== No.16: findAllScreens 正常系 - 画面マスタあり → リストを返す =====
    @Test
    void findAllScreens_画面マスタあり_リストを返す() {
        Screen screen1 = screen("S001", "操作ログ照会");
        Screen screen2 = screen("S002", "帳票ログ照会");
        when(screenRepository.findAllByOrderByScreenIdAsc()).thenReturn(List.of(screen1, screen2));

        List<Screen> result = service.findAllScreens();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(screen1, screen2);
    }

    // ===== No.17: findAllScreens 正常系 - 画面マスタが0件 → 空リストを返す =====
    @Test
    void findAllScreens_画面マスタが0件_空リストを返す() {
        when(screenRepository.findAllByOrderByScreenIdAsc()).thenReturn(List.of());

        assertThat(service.findAllScreens()).isEmpty();
    }
}

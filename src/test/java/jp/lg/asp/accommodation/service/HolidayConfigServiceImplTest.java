package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.entity.Kyugyobi;
import jp.lg.asp.accommodation.repository.HolidayRepository;
import jp.lg.asp.accommodation.service.impl.HolidayConfigServiceImpl;

@ExtendWith(MockitoExtension.class)
class HolidayConfigServiceImplTest {

    @Mock HolidayRepository holidayRepository;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks HolidayConfigServiceImpl service;

    private static final String JICHITAI_CD = "01100";

    private Kyugyobi kyugyobi(LocalDate date) {
        Kyugyobi k = new Kyugyobi();
        k.setKyugyobi(date);
        return k;
    }

    // ── findByNendo ──────────────────────────────────────────────

    @Test
    void findByNendo_登録済みなら自治体自身の休業日を返す() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(holidayRepository.existsByJichitaiCdAndNen(JICHITAI_CD, "2026")).thenReturn(true);
        when(holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi(JICHITAI_CD, "2026"))
                .thenReturn(List.of(kyugyobi(LocalDate.of(2026, 1, 1)), kyugyobi(LocalDate.of(2026, 5, 3))));

        var form = service.findByNendo("2026");

        assertThat(form.getNendo()).isEqualTo("2026");
        assertThat(form.getHolidayDts()).containsExactly("20260101", "20260503");
        verify(holidayRepository, never()).findByJichitaiCdAndNenOrderByKyugyobi("99999", "2026");
    }

    @Test
    void findByNendo_未登録なら共通テンプレートの休業日を返す() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(holidayRepository.existsByJichitaiCdAndNen(JICHITAI_CD, "2026")).thenReturn(false);
        when(holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi("99999", "2026"))
                .thenReturn(List.of(kyugyobi(LocalDate.of(2026, 1, 1))));

        var form = service.findByNendo("2026");

        assertThat(form.getHolidayDts()).containsExactly("20260101");
        verify(holidayRepository, never()).findByJichitaiCdAndNenOrderByKyugyobi(JICHITAI_CD, "2026");
    }

    @Test
    void findByNendo_番兵レコードは除外され空リストを返す() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(holidayRepository.existsByJichitaiCdAndNen(JICHITAI_CD, "2026")).thenReturn(true);
        when(holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi(JICHITAI_CD, "2026"))
                .thenReturn(List.of(kyugyobi(LocalDate.of(1, 1, 1))));

        var form = service.findByNendo("2026");

        assertThat(form.getHolidayDts()).isEmpty();
    }

    // ── findNendoList ─────────────────────────────────────────────

@Test
    void findNendoList_自治体と共通テンプレートの年リストを合わせてソート重複排除して返す() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(holidayRepository.findDistinctNenByJichitaiCd(JICHITAI_CD))
                .thenReturn(List.of("2025", "2026"));
        when(holidayRepository.findDistinctNenByJichitaiCd("99999"))
                .thenReturn(List.of("2024", "2025"));

        var result = service.findNendoList();

        assertThat(result).containsExactly("2024", "2025", "2026");
    }

    @Test
    void findNendoList_自治体に登録がなく共通テンプレートのみの場合共通テンプレートの年リストを返す() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(holidayRepository.findDistinctNenByJichitaiCd(JICHITAI_CD)).thenReturn(List.of());
        when(holidayRepository.findDistinctNenByJichitaiCd("99999")).thenReturn(List.of("2026"));

        var result = service.findNendoList();

        assertThat(result).containsExactly("2026");
    }

    // ── getInitialHolidays ────────────────────────────────────────

    @Test
    void getInitialHolidays_常に共通テンプレートから取得する() {
        when(holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi("99999", "2026"))
                .thenReturn(List.of(kyugyobi(LocalDate.of(2026, 1, 1)), kyugyobi(LocalDate.of(2026, 1, 2))));

        var result = service.getInitialHolidays("2026");

        assertThat(result).containsExactly("20260101", "20260102");
        verifyNoInteractions(jichitaiContext);
    }

    // ── save ──────────────────────────────────────────────────────

    @Test
    void save_削除してから新規登録する順序保証() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        var form = new jp.lg.asp.accommodation.dto.HolidayConfigForm();
        form.setNendo("2026");
        form.setHolidayDts(List.of("20260101"));

        var order = inOrder(holidayRepository);
        service.save(form);

        order.verify(holidayRepository).deleteByJichitaiCdAndNen(JICHITAI_CD, "2026");
        order.verify(holidayRepository).save(any());
    }

    @Test
    void save_休業日の件数ぶんレコードが保存される() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        var form = new jp.lg.asp.accommodation.dto.HolidayConfigForm();
        form.setNendo("2026");
        form.setHolidayDts(List.of("20260101", "20260503"));

        service.save(form);

        var captor = org.mockito.ArgumentCaptor.forClass(Kyugyobi.class);
        verify(holidayRepository, times(2)).save(captor.capture());
        var saved = captor.getAllValues();
        assertThat(saved).allSatisfy(k -> {
            assertThat(k.getJichitaiCd()).isEqualTo(JICHITAI_CD);
            assertThat(k.getNen()).isEqualTo("2026");
        });
        assertThat(saved.get(0).getKyugyobi()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(saved.get(1).getKyugyobi()).isEqualTo(LocalDate.of(2026, 5, 3));
    }

    @Test
    void save_空リストの場合は番兵レコードを1件保存する() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        var form = new jp.lg.asp.accommodation.dto.HolidayConfigForm();
        form.setNendo("2026");
        form.setHolidayDts(List.of());

        service.save(form);

        var captor = org.mockito.ArgumentCaptor.forClass(Kyugyobi.class);
        verify(holidayRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getKyugyobi()).isEqualTo(LocalDate.of(1, 1, 1));
        assertThat(captor.getValue().getJichitaiCd()).isEqualTo(JICHITAI_CD);
        assertThat(captor.getValue().getNen()).isEqualTo("2026");
    }

    @Test
    void save_nullの場合は番兵レコードを1件保存する() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        var form = new jp.lg.asp.accommodation.dto.HolidayConfigForm();
        form.setNendo("2026");
        form.setHolidayDts(null);

        service.save(form);

        var captor = org.mockito.ArgumentCaptor.forClass(Kyugyobi.class);
        verify(holidayRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getKyugyobi()).isEqualTo(LocalDate.of(1, 1, 1));
    }

    @Test
    void save_不正日付形式はDateTimeParseExceptionをスローする() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        var form = new jp.lg.asp.accommodation.dto.HolidayConfigForm();
        form.setNendo("2026");
        form.setHolidayDts(List.of("2026-01-01"));

        assertThatThrownBy(() -> service.save(form))
                .isInstanceOf(java.time.format.DateTimeParseException.class);
    }
}

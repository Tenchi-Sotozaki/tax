package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.HolidayConfigForm;
import jp.lg.asp.accommodation.entity.Kyugyobi;
import jp.lg.asp.accommodation.repository.HolidayRepository;
import jp.lg.asp.accommodation.service.impl.HolidayConfigServiceImpl;

@ExtendWith(MockitoExtension.class)
class HolidayConfigServiceImplTest {

    @Mock HolidayRepository holidayRepository;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks HolidayConfigServiceImpl service;

    private static final String JICHITAI_CD = "01100";

    // ===== getInitialHolidays =====

    @Test
    void getInitialHolidays_自治体コードに関わらず99999から取得する() {
        Kyugyobi k1 = new Kyugyobi();
        k1.setKyugyobi(LocalDate.of(2026, 1, 1));
        Kyugyobi k2 = new Kyugyobi();
        k2.setKyugyobi(LocalDate.of(2026, 1, 2));
        when(holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi("99999", "2026")).thenReturn(List.of(k1, k2));

        List<String> result = service.getInitialHolidays("2026");

        assertThat(result).containsExactly("20260101", "20260102");
        verify(holidayRepository).findByJichitaiCdAndNenOrderByKyugyobi("99999", "2026");
    }

    // ===== save =====

    @Test
    void save_deleteがsaveより先に呼ばれる() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        HolidayConfigForm form = new HolidayConfigForm();
        form.setNendo("2026");
        form.setHolidayDts(List.of("20260101"));

        service.save(form);

        InOrder inOrder = inOrder(holidayRepository);
        inOrder.verify(holidayRepository).deleteByJichitaiCdAndNen(JICHITAI_CD, "2026");
        inOrder.verify(holidayRepository).save(any());
    }

    @Test
    void save_休業日の件数ぶんレコードが保存される() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        HolidayConfigForm form = new HolidayConfigForm();
        form.setNendo("2026");
        form.setHolidayDts(List.of("20260101", "20260503"));

        service.save(form);

        ArgumentCaptor<Kyugyobi> captor = ArgumentCaptor.forClass(Kyugyobi.class);
        verify(holidayRepository, times(2)).save(captor.capture());
        List<Kyugyobi> saved = captor.getAllValues();
        assertThat(saved.get(0).getJichitaiCd()).isEqualTo(JICHITAI_CD);
        assertThat(saved.get(0).getNen()).isEqualTo("2026");
        assertThat(saved.get(0).getKyugyobi()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(saved.get(1).getKyugyobi()).isEqualTo(LocalDate.of(2026, 5, 3));
    }

    @Test
    void save_休業日が空リストの場合_番兵レコードを1件保存する() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        HolidayConfigForm form = new HolidayConfigForm();
        form.setNendo("2026");
        form.setHolidayDts(List.of());

        service.save(form);

        ArgumentCaptor<Kyugyobi> captor = ArgumentCaptor.forClass(Kyugyobi.class);
        verify(holidayRepository, times(1)).save(captor.capture());
        Kyugyobi saved = captor.getValue();
        assertThat(saved.getJichitaiCd()).isEqualTo(JICHITAI_CD);
        assertThat(saved.getNen()).isEqualTo("2026");
        assertThat(saved.getKyugyobi()).isEqualTo(LocalDate.of(1, 1, 1));
    }

    @Test
    void save_休業日がnullの場合_番兵レコードを1件保存する() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        HolidayConfigForm form = new HolidayConfigForm();
        form.setNendo("2026");
        form.setHolidayDts(null);

        service.save(form);

        ArgumentCaptor<Kyugyobi> captor = ArgumentCaptor.forClass(Kyugyobi.class);
        verify(holidayRepository, times(1)).save(captor.capture());
        Kyugyobi saved = captor.getValue();
        assertThat(saved.getJichitaiCd()).isEqualTo(JICHITAI_CD);
        assertThat(saved.getNen()).isEqualTo("2026");
        assertThat(saved.getKyugyobi()).isEqualTo(LocalDate.of(1, 1, 1));
    }

    @Test
    void save_日付形式がyyyyMMdd以外の場合_DateTimeParseExceptionがスローされる() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        HolidayConfigForm form = new HolidayConfigForm();
        form.setNendo("2026");
        form.setHolidayDts(List.of("2026-01-01"));

        assertThatThrownBy(() -> service.save(form)).isInstanceOf(DateTimeParseException.class);
    }
}

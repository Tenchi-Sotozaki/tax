package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.TekiyoNozeiShukiForm;
import jp.lg.asp.accommodation.entity.NozeiShuki;
import jp.lg.asp.accommodation.entity.TekiyoNozeiShuki;
import jp.lg.asp.accommodation.repository.NozeiShukiRepository;
import jp.lg.asp.accommodation.repository.TekiyoNozeiShukiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.TekiyoNozeiShukiServiceImpl;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class TekiyoNozeiShukiServiceImplTest {

    @Mock TekiyoNozeiShukiRepository tekiyoNozeiShukiRepository;
    @Mock TokugimuRepository tokugimuRepository;
    @Mock NozeiShukiRepository nozeiShukiRepository;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks TekiyoNozeiShukiServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "00000001";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    @Test
    void getNozeiShukiOptions_returnsMappedList() {
        NozeiShuki n = new NozeiShuki();
        n.setSeq(BigDecimal.ONE);
        n.setShuki(BigDecimal.valueOf(3));
        when(nozeiShukiRepository.findActiveByJichitaiCd(JICHITAI_CD)).thenReturn(List.of(n));

        assertThat(service.getNozeiShukiOptions()).hasSize(1);
    }

    @Test
    void getByShiteiNo_noHistory_returnsEmptyForm() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of());
        when(tekiyoNozeiShukiRepository.findLatestByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());

        TekiyoNozeiShukiForm form = service.getByShiteiNo(SHITEI_NO);

        assertThat(form.getShiteiNo()).isEqualTo(SHITEI_NO);
        assertThat(form.isEdit()).isFalse();
    }

    @Test
    void save_startAfterEnd_throwsException() {
        TekiyoNozeiShukiForm form = new TekiyoNozeiShukiForm();
        form.setRno(1);
        form.setTekiyoStMonth("2024-06");
        form.setTekiyoEdMonth("2024-03");

        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.save(SHITEI_NO, form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("適用開始年月");
    }

    @Test
    void save_overlappingPeriod_throwsException() {
        TekiyoNozeiShukiForm form = new TekiyoNozeiShukiForm();
        form.setRno(1);
        form.setTekiyoStMonth("2024-04");
        form.setTekiyoEdMonth("2024-09");

        TekiyoNozeiShuki existing = new TekiyoNozeiShuki();
        existing.setTekiyoStYmd(LocalDate.of(2024, 1, 1));
        existing.setTekiyoEdYmd(LocalDate.of(2024, 6, 30));
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.save(SHITEI_NO, form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("重複");
    }

    @Test
    void save_validPeriod_savesEntity() {
        TekiyoNozeiShukiForm form = new TekiyoNozeiShukiForm();
        form.setRno(1);
        form.setTekiyoStMonth("2024-10");

        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());
        when(tekiyoNozeiShukiRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(0);
        when(tekiyoNozeiShukiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.save(SHITEI_NO, form);

        verify(tekiyoNozeiShukiRepository).save(argThat(e -> e.getRno() == 1));
    }

    @Test
    void delete_setsDelFlg1() {
        TekiyoNozeiShuki entity = new TekiyoNozeiShuki();
        entity.setDelFlg("0");
        entity.setRno(1);
        when(tekiyoNozeiShukiRepository.findLatestByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(entity));
        when(tekiyoNozeiShukiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.delete(SHITEI_NO);

        assertThat(entity.getDelFlg()).isEqualTo("1");
    }

    @Test
    void delete_notFound_throwsException() {
        when(tekiyoNozeiShukiRepository.findLatestByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.delete(SHITEI_NO))
                .isInstanceOf(IllegalStateException.class);
    }
}

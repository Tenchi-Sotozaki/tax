package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
import jp.lg.asp.accommodation.dto.TokureiTekiyoForm;
import jp.lg.asp.accommodation.dto.TokureiTekiyoHistoryDto;
import jp.lg.asp.accommodation.entity.TekiyoNozeiShuki;
import jp.lg.asp.accommodation.repository.TekiyoNozeiShukiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.TokureiTekiyoServiceImpl;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class TokureiTekiyoServiceImplTest {

    @Mock TekiyoNozeiShukiRepository tekiyoNozeiShukiRepository;
    @Mock TokugimuRepository tokugimuRepository;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks TokureiTekiyoServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "00000001";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    // ========== getHistories ==========

    @Test
    void getHistories_レコードなし_空リストを返す() {
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());

        List<TokureiTekiyoHistoryDto> result = service.getHistories(SHITEI_NO);

        assertThat(result).isEmpty();
    }

    @Test
    void getHistories_レコードあり_DTOリストを返す() {
        TekiyoNozeiShuki t = new TekiyoNozeiShuki();
        t.setRno(1);
        t.setTekiyoStYmd(LocalDate.of(2024, 4, 1));
        t.setTekiyoEdYmd(LocalDate.of(2024, 9, 30));
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(t));

        List<TokureiTekiyoHistoryDto> result = service.getHistories(SHITEI_NO);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIdx()).isEqualTo(1);
        assertThat(result.get(0).getTekiyoStMonth()).isEqualTo("2024年04月");
    }

    // ========== getForView ==========

    @Test
    void getForView_存在するrno_フォームを返す() {
        TekiyoNozeiShuki t = new TekiyoNozeiShuki();
        t.setRno(1);
        t.setTekiyoStYmd(LocalDate.of(2024, 4, 1));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of());
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(t));

        TokureiTekiyoForm form = service.getForView(SHITEI_NO, 1);

        assertThat(form.getShiteiNo()).isEqualTo(SHITEI_NO);
        assertThat(form.getRno()).isEqualTo(1);
        assertThat(form.getTekiyoStMonth()).isEqualTo("2024-04");
    }

    @Test
    void getForView_存在しないrno_例外をスロー() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of());
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.getForView(SHITEI_NO, 99))
                .isInstanceOf(IllegalStateException.class);
    }

    // ========== save ==========

    @Test
    void save_適用開始年月なし_例外をスロー() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        form.setTekiyoStMonth(null);

        assertThatThrownBy(() -> service.save(SHITEI_NO, form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("適用開始年月は必須");
    }

    @Test
    void save_開始が終了より後_例外をスロー() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        form.setTekiyoStMonth("2024-06");
        form.setTekiyoEdMonth("2024-03");

        assertThatThrownBy(() -> service.save(SHITEI_NO, form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("適用開始年月");
    }

    @Test
    void save_期間重複_例外をスロー() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        form.setTekiyoStMonth("2024-04");
        form.setTekiyoEdMonth("2024-09");

        TekiyoNozeiShuki existing = new TekiyoNozeiShuki();
        existing.setRno(1);
        existing.setTekiyoStYmd(LocalDate.of(2024, 1, 1));
        existing.setTekiyoEdYmd(LocalDate.of(2024, 6, 30));
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.save(SHITEI_NO, form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("重複");
    }

    @Test
    void save_正常_エンティティを保存() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        form.setTekiyoStMonth("2024-10");

        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());
        when(tekiyoNozeiShukiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.save(SHITEI_NO, form);

        verify(tekiyoNozeiShukiRepository).save(any());
    }

    // ========== update ==========

    @Test
    void update_正常_エンティティを更新() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        form.setTekiyoStMonth("2024-10");

        TekiyoNozeiShuki entity = new TekiyoNozeiShuki();
        entity.setRno(1);
        entity.setTekiyoStYmd(LocalDate.of(2024, 4, 1));
        entity.setTekiyoEdYmd(LocalDate.of(2024, 9, 30));

        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(entity));
        when(tekiyoNozeiShukiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(SHITEI_NO, 1, form);

        verify(tekiyoNozeiShukiRepository).save(entity);
    }

    // ========== delete ==========

    @Test
    void delete_正常_delFlgを1に設定() {
        TekiyoNozeiShuki entity = new TekiyoNozeiShuki();
        entity.setRno(1);
        entity.setDelFlg("0");
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(entity));
        when(tekiyoNozeiShukiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.delete(SHITEI_NO, 1);

        assertThat(entity.getDelFlg()).isEqualTo("1");
    }

    @Test
    void delete_存在しないrno_例外をスロー() {
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.delete(SHITEI_NO, 99))
                .isInstanceOf(IllegalStateException.class);
    }
}

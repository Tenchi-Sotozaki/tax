package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.EltaxRenkeiDto;
import jp.lg.asp.accommodation.entity.EltaxRenkei;
import jp.lg.asp.accommodation.entity.EltaxRenkeiId;
import jp.lg.asp.accommodation.repository.EltaxRenkeiRepository;
import jp.lg.asp.accommodation.service.impl.EltaxRenkeiServiceImpl;

@ExtendWith(MockitoExtension.class)
class EltaxRenkeiServiceImplTest {

    @Mock EltaxRenkeiRepository eltaxRenkeiRepository;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks EltaxRenkeiServiceImpl service;

    private static final String JICHITAI_CD = "01202";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    private EltaxRenkei buildEntity(BigDecimal seq, String fileName, String shubetsu,
            LocalDateTime shoriDt, String shoriKekka) {
        EltaxRenkei e = new EltaxRenkei();
        e.setSeq(seq);
        e.setFileName(fileName);
        e.setShubetsu(shubetsu);
        e.setShoriDt(shoriDt);
        e.setShoriKekka(shoriKekka);
        return e;
    }

    // -------------------------------------------------------------------------
    // findAll
    // -------------------------------------------------------------------------

    // No.9: 対象自治体のeLTAX連携情報をDTO一覧へ変換して返す
    @Test
    void findAll_対象自治体の連携情報をDTO一覧へ変換して返す() {
        var entities = List.of(
                buildEntity(BigDecimal.ONE, "a.xml", "1", LocalDateTime.now(), "OK"),
                buildEntity(BigDecimal.TWO, "b.xml", "2", LocalDateTime.now(), "NG"));
        when(eltaxRenkeiRepository.findByJichitaiCd(JICHITAI_CD, PageRequest.of(0, 1000)))
                .thenReturn(entities);

        List<EltaxRenkeiDto> result = service.findAll();

        verify(jichitaiContext).getJichitaiCd();
        verify(eltaxRenkeiRepository).findByJichitaiCd(JICHITAI_CD, PageRequest.of(0, 1000));
        assertThat(result).hasSize(2);
    }

    // No.10: エンティティの各項目をDTOへ正しくマッピングする
    @Test
    void findAll_エンティティの各項目をDTOへ正しくマッピングする() {
        LocalDateTime shoriDt = LocalDateTime.of(2024, 1, 15, 10, 0);
        EltaxRenkei entity = buildEntity(BigDecimal.ONE, "result.xml", "1", shoriDt, "OK");
        when(eltaxRenkeiRepository.findByJichitaiCd(JICHITAI_CD, PageRequest.of(0, 1000)))
                .thenReturn(List.of(entity));

        EltaxRenkeiDto dto = service.findAll().get(0);

        assertThat(dto.getSeq()).isEqualTo(BigDecimal.ONE);
        assertThat(dto.getFileName()).isEqualTo("result.xml");
        assertThat(dto.getShubetsu()).isEqualTo("1");
        assertThat(dto.getShoriDt()).isEqualTo(shoriDt);
        assertThat(dto.getShoriKekka()).isEqualTo("OK");
    }

    // No.11: 複数件取得時にリポジトリの取得順を保持する
    @Test
    void findAll_複数件取得時にリポジトリの取得順を保持する() {
        var entities = List.of(
                buildEntity(BigDecimal.ONE, "a.xml", "1", null, null),
                buildEntity(BigDecimal.TWO, "b.xml", "2", null, null));
        when(eltaxRenkeiRepository.findByJichitaiCd(JICHITAI_CD, PageRequest.of(0, 1000)))
                .thenReturn(entities);

        List<EltaxRenkeiDto> result = service.findAll();

        assertThat(result.get(0).getSeq()).isEqualTo(BigDecimal.ONE);
        assertThat(result.get(1).getSeq()).isEqualTo(BigDecimal.TWO);
    }

    // No.12: 対象自治体の連携情報が存在しない場合は空リストを返す
    @Test
    void findAll_連携情報が存在しない場合は空リストを返す() {
        when(eltaxRenkeiRepository.findByJichitaiCd(JICHITAI_CD, PageRequest.of(0, 1000)))
                .thenReturn(List.of());

        List<EltaxRenkeiDto> result = service.findAll();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    // No.13: リポジトリ検索で例外が発生した場合は例外をそのまま送出する
    @Test
    void findAll_リポジトリ検索で例外が発生した場合は例外をそのまま送出する() {
        when(eltaxRenkeiRepository.findByJichitaiCd(JICHITAI_CD, PageRequest.of(0, 1000)))
                .thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> service.findAll())
                .isInstanceOf(RuntimeException.class);
    }

    // -------------------------------------------------------------------------
    // findBySeq
    // -------------------------------------------------------------------------

    // No.22: 自治体コードと連番を複合キーにして対象のeLTAX連携情報を返す
    @Test
    void findBySeq_自治体コードと連番を複合キーにして対象の連携情報を返す() {
        EltaxRenkei entity = buildEntity(BigDecimal.ONE, "test.xml", "1", null, null);
        when(eltaxRenkeiRepository.findById(new EltaxRenkeiId(JICHITAI_CD, BigDecimal.ONE)))
                .thenReturn(Optional.of(entity));

        EltaxRenkei result = service.findBySeq(BigDecimal.ONE);

        verify(jichitaiContext).getJichitaiCd();
        verify(eltaxRenkeiRepository).findById(new EltaxRenkeiId(JICHITAI_CD, BigDecimal.ONE));
        assertThat(result).isSameAs(entity);
    }

    // No.23: 指定した複合キーの連携情報が存在しない場合はnullを返す
    @Test
    void findBySeq_指定した複合キーの連携情報が存在しない場合はnullを返す() {
        BigDecimal seq = new BigDecimal("999");
        when(eltaxRenkeiRepository.findById(new EltaxRenkeiId(JICHITAI_CD, seq)))
                .thenReturn(Optional.empty());

        EltaxRenkei result = service.findBySeq(seq);

        verify(eltaxRenkeiRepository).findById(new EltaxRenkeiId(JICHITAI_CD, seq));
        assertThat(result).isNull();
    }

    // No.24: 自治体コードの取得で例外が発生した場合は例外をそのまま送出する
    @Test
    void findBySeq_自治体コード取得で例外が発生した場合は例外をそのまま送出する() {
        when(jichitaiContext.getJichitaiCd()).thenThrow(new RuntimeException("context error"));

        assertThatThrownBy(() -> service.findBySeq(BigDecimal.ONE))
                .isInstanceOf(RuntimeException.class);
        verify(eltaxRenkeiRepository, never()).findById(any());
    }

    // No.25: 複合キー検索で例外が発生した場合は例外をそのまま送出する
    @Test
    void findBySeq_複合キー検索で例外が発生した場合は例外をそのまま送出する() {
        when(eltaxRenkeiRepository.findById(any())).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> service.findBySeq(BigDecimal.ONE))
                .isInstanceOf(RuntimeException.class);
    }
}

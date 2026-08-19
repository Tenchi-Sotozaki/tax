package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.AtenaId;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.exception.ResourceNotFoundException;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.service.impl.AtenaConfigServiceImpl;

/**
 * 宛名管理台帳 / 宛名登録・編集・照会（ACCOMMODATION_TAX-343 / 366）の Service 単体テスト。
 *
 * DBには接続せず、リポジトリをモックに差し替えて
 * AtenaConfigServiceImpl のロジックのみを検証する。
 *
 * 検証の中心は次の2点。
 *   1. 宛名番号の採番（自治体の atena_st_no を+1して払い出し、自治体側も更新する）
 *   2. 区分の自動判定（個人番号があれば 1、無ければ 2）
 */
@ExtendWith(MockitoExtension.class)
class AtenaConfigServiceImplTest {

    @Mock AtenaRepository atenaRepository;
    @Mock JichitaiRepository jichitaiRepository;

    @InjectMocks AtenaConfigServiceImpl service;

    private static final String JICHITAI_CD = "01100";

    // ===================================================================
    // テストデータ
    // ===================================================================

    private Jichitai jichitai(BigDecimal atenaStNo) {
        Jichitai j = new Jichitai();
        j.setAtenaStNo(atenaStNo);
        return j;
    }

    private Atena atena(String kojinNo) {
        Atena a = new Atena();
        a.setName("山田太郎");
        a.setKojinNo(kojinNo);
        return a;
    }

    /** save() に渡された宛名を取り出す */
    private Atena savedAtena() {
        ArgumentCaptor<Atena> captor = ArgumentCaptor.forClass(Atena.class);
        verify(atenaRepository).save(captor.capture());
        return captor.getValue();
    }

    // ===================================================================
    // findByAtenaNo — 照会
    // ===================================================================

    @Test
    void findByAtenaNo_該当があればそのまま返す() {
        Atena expected = atena("123456789012");
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, new BigDecimal("1001")))
                .thenReturn(Optional.of(expected));

        assertThat(service.findByAtenaNo(JICHITAI_CD, new BigDecimal("1001"))).isSameAs(expected);
    }

    @Test
    void findByAtenaNo_該当が無ければ例外を投げる() {
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, new BigDecimal("9999")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByAtenaNo(JICHITAI_CD, new BigDecimal("9999")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("宛名が見つかりません");
    }

    // ===================================================================
    // register — 新規登録
    // ===================================================================

    @Test
    void register_自治体の採番値を1つ進めて宛名番号にする() {
        when(jichitaiRepository.findById(JICHITAI_CD))
                .thenReturn(Optional.of(jichitai(new BigDecimal("1000"))));

        service.register(atena("123456789012"), JICHITAI_CD);

        Atena saved = savedAtena();
        assertThat(saved.getAtenaNo()).isEqualByComparingTo("1001");
        assertThat(saved.getJichitaiCd()).isEqualTo(JICHITAI_CD);
    }

    @Test
    void register_採番値も自治体側に書き戻される() {
        Jichitai jichitai = jichitai(new BigDecimal("1000"));
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));

        service.register(atena("123456789012"), JICHITAI_CD);

        ArgumentCaptor<Jichitai> captor = ArgumentCaptor.forClass(Jichitai.class);
        verify(jichitaiRepository).save(captor.capture());
        assertThat(captor.getValue().getAtenaStNo()).isEqualByComparingTo("1001");
    }

    @Test
    void register_採番値がnullなら1から始まる() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai(null)));

        service.register(atena("123456789012"), JICHITAI_CD);

        assertThat(savedAtena().getAtenaNo()).isEqualByComparingTo("1");
    }

    @Test
    void register_個人番号があれば区分は1になる() {
        when(jichitaiRepository.findById(JICHITAI_CD))
                .thenReturn(Optional.of(jichitai(new BigDecimal("1000"))));

        service.register(atena("123456789012"), JICHITAI_CD);

        assertThat(savedAtena().getKbn()).isEqualTo("1");
    }

    @Test
    void register_個人番号がnullまたは空白なら区分は2になる() {
        when(jichitaiRepository.findById(JICHITAI_CD))
                .thenReturn(Optional.of(jichitai(new BigDecimal("1000"))));

        service.register(atena(null), JICHITAI_CD);
        assertThat(savedAtena().getKbn()).isEqualTo("2");

        clearInvocations(atenaRepository);
        service.register(atena("   "), JICHITAI_CD);
        assertThat(savedAtena().getKbn()).isEqualTo("2");
    }

    @Test
    void register_自治体が見つからなければ例外を投げて保存しない() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(atena("123456789012"), JICHITAI_CD))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("自治体情報が見つかりません");

        verify(atenaRepository, never()).save(any());
        verify(jichitaiRepository, never()).save(any());
    }

    // ===================================================================
    // update — 更新
    // ===================================================================

    @Test
    void update_既存があれば自治体コードと区分を設定して保存する() {
        Atena input = atena("123456789012");
        input.setAtenaNo(new BigDecimal("1001"));
        when(atenaRepository.findById(new AtenaId(JICHITAI_CD, new BigDecimal("1001"))))
                .thenReturn(Optional.of(new Atena()));

        service.update(input, JICHITAI_CD);

        Atena saved = savedAtena();
        assertThat(saved.getJichitaiCd()).isEqualTo(JICHITAI_CD);
        assertThat(saved.getAtenaNo()).isEqualByComparingTo("1001");
        assertThat(saved.getKbn()).isEqualTo("1");
    }

    @Test
    void update_個人番号が無ければ区分は2になる() {
        Atena input = atena(null);
        input.setAtenaNo(new BigDecimal("1001"));
        when(atenaRepository.findById(new AtenaId(JICHITAI_CD, new BigDecimal("1001"))))
                .thenReturn(Optional.of(new Atena()));

        service.update(input, JICHITAI_CD);

        assertThat(savedAtena().getKbn()).isEqualTo("2");
    }

    /** 採番は行わないため、自治体テーブルには触らない */
    @Test
    void update_自治体の採番値は更新しない() {
        Atena input = atena("123456789012");
        input.setAtenaNo(new BigDecimal("1001"));
        when(atenaRepository.findById(any(AtenaId.class))).thenReturn(Optional.of(new Atena()));

        service.update(input, JICHITAI_CD);

        verify(jichitaiRepository, never()).save(any());
    }

    @Test
    void update_既存が無ければ例外を投げて保存しない() {
        Atena input = atena("123456789012");
        input.setAtenaNo(new BigDecimal("9999"));
        when(atenaRepository.findById(any(AtenaId.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(input, JICHITAI_CD))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("宛名が見つかりません");

        verify(atenaRepository, never()).save(any());
    }
}

package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.TaxManagerForm;
import jp.lg.asp.accommodation.entity.TaxManager;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.TaxManagerRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.TaxManagerServiceImpl;

/**
 * 納税管理人 登録/編集/照会（ACCOMMODATION_TAX-338）の単体テスト。
 *
 * DBには接続せず、リポジトリと自治体コンテキストをモックに差し替えて
 * TaxManagerServiceImpl のロジックのみを検証する。
 */
@ExtendWith(MockitoExtension.class)
class TaxManagerServiceImplTest {

    @Mock TaxManagerRepository taxManagerRepository;
    @Mock TokugimuRepository tokugimuRepository;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks TaxManagerServiceImpl service;

    private static final String JICHITAI_CD = "01100";
    private static final String SHITEI_NO = "00100001";

    @BeforeEach
    void setUp() {
        // isSamePerson のテストでは参照されないため lenient にしておく
        lenient().when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    // ===================================================================
    // テストデータ
    // ===================================================================

    /** 納税管理人の履歴レコード（t_nokan） */
    private TaxManager nokan(int rno) {
        TaxManager e = new TaxManager();
        e.setJichitaiCd(JICHITAI_CD);
        e.setShiteiNo(SHITEI_NO);
        e.setRno(rno);
        e.setKbn("1");
        e.setTorokuYmd(LocalDate.of(2026, 4, 1));
        e.setShinkokuYmd(LocalDate.of(2026, 3, 25));
        e.setAtenaNo("2002");
        e.setName("山田太郎");
        e.setNameKana("ヤマダタロウ");
        e.setYubinNo("060-0001");
        e.setJusho("札幌市中央区北1条西1丁目");
        e.setTel("011-000-0000");
        e.setRiyu("転居のため");
        e.setNewFlg("1");
        e.setDelFlg("0");
        return e;
    }

    /** 特別徴収義務者（t_tokugimu） */
    private Tokugimu tokugimu() {
        Tokugimu t = new Tokugimu();
        t.setKyokaName("株式会社ホテルA");
        t.setShisetsuName("ホテルA 札幌");
        t.setAtenaNo(new BigDecimal("1001"));
        return t;
    }

    /** 登録可能な入力フォーム（特徴とは別人物） */
    private TaxManagerForm validForm() {
        TaxManagerForm f = new TaxManagerForm();
        f.setKbn("1");
        f.setRegistrationDate(LocalDate.of(2026, 4, 1));
        f.setDeclarationDate(LocalDate.of(2026, 3, 25));
        f.setAtenaNo("2002");
        f.setObligorAtenaNo("1001");
        f.setManagerName("山田太郎");
        f.setManagerNameKana("ヤマダタロウ");
        f.setManagerYubinNo("060-0001");
        f.setManagerAddress("札幌市中央区北1条西1丁目");
        f.setManagerPhone("011-000-0000");
        f.setReason("転居のため");
        return f;
    }

    /** save() に渡されたエンティティを取り出す */
    private TaxManager savedEntity() {
        ArgumentCaptor<TaxManager> captor = ArgumentCaptor.forClass(TaxManager.class);
        verify(taxManagerRepository).save(captor.capture());
        return captor.getValue();
    }

    // ===================================================================
    // isSamePerson — 特別徴収義務者との同一人物チェック
    // ===================================================================

    @Test
    void isSamePerson_宛名番号が一致すればtrue() {
        assertThat(service.isSamePerson("1001", "1001")).isTrue();
    }

    @Test
    void isSamePerson_前後に空白があっても一致とみなす() {
        assertThat(service.isSamePerson("  1001  ", "1001")).isTrue();
    }

    @Test
    void isSamePerson_宛名番号が異なればfalse() {
        assertThat(service.isSamePerson("1001", "1002")).isFalse();
    }

    @Test
    void isSamePerson_納税管理人側がnullまたは空ならfalse() {
        assertThat(service.isSamePerson(null, "1001")).isFalse();
        assertThat(service.isSamePerson("   ", "1001")).isFalse();
    }

    @Test
    void isSamePerson_特別徴収義務者側がnullまたは空ならfalse() {
        assertThat(service.isSamePerson("1001", null)).isFalse();
        assertThat(service.isSamePerson("1001", "   ")).isFalse();
    }

    // ===================================================================
    // getByShiteiNo — 照会（最新レコード）
    // ===================================================================

    @Test
    void getByShiteiNo_納管レコードがあれば編集モードで各項目が載る() {
        when(taxManagerRepository.findLatestByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(Optional.of(nokan(2)));

        TaxManagerForm form = service.getByShiteiNo(SHITEI_NO);

        assertThat(form.isEdit()).isTrue();
        assertThat(form.getRno()).isEqualTo(2);
        assertThat(form.getAtenaNo()).isEqualTo("2002");
        assertThat(form.getManagerName()).isEqualTo("山田太郎");
        assertThat(form.getManagerNameKana()).isEqualTo("ヤマダタロウ");
        assertThat(form.getManagerYubinNo()).isEqualTo("060-0001");
        assertThat(form.getManagerAddress()).isEqualTo("札幌市中央区北1条西1丁目");
        assertThat(form.getManagerPhone()).isEqualTo("011-000-0000");
        assertThat(form.getKbn()).isEqualTo("1");
        assertThat(form.getReason()).isEqualTo("転居のため");
        assertThat(form.getRegistrationDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(form.getDeclarationDate()).isEqualTo(LocalDate.of(2026, 3, 25));
    }

    @Test
    void getByShiteiNo_納管レコードが無ければ新規モードで登録日と申告日が本日になる() {
        when(taxManagerRepository.findLatestByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(Optional.empty());

        TaxManagerForm form = service.getByShiteiNo(SHITEI_NO);

        assertThat(form.isEdit()).isFalse();
        assertThat(form.getShiteiNo()).isEqualTo(SHITEI_NO);
        assertThat(form.getRegistrationDate()).isEqualTo(LocalDate.now());
        assertThat(form.getDeclarationDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void getByShiteiNo_特別徴収義務者の宛名番号がフォームに載る() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tokugimu()));

        TaxManagerForm form = service.getByShiteiNo(SHITEI_NO);

        assertThat(form.getObligorAtenaNo()).isEqualTo("1001");
    }

    @Test
    void getByShiteiNo_履歴番号の最大と最小が載る() {
        when(taxManagerRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(3);
        when(taxManagerRepository.findMinRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(1);

        TaxManagerForm form = service.getByShiteiNo(SHITEI_NO);

        assertThat(form.getMaxRno()).isEqualTo(3);
        assertThat(form.getMinRno()).isEqualTo(1);
    }

    /**
     * リポジトリの例外は握りつぶさず、そのまま呼び出し元へ伝播させる。
     * 「該当データ無し」は Optional.empty で表現され、新規扱いのフォームが返る（上のテスト）。
     */
    @Test
    void getByShiteiNo_リポジトリが例外を投げたらそのまま伝播する() {
        when(taxManagerRepository.findLatestByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenThrow(new RuntimeException("DB接続エラー"));

        assertThatThrownBy(() -> service.getByShiteiNo(SHITEI_NO))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB接続エラー");
    }

    // ===================================================================
    // getByShiteiNoAndRno — 照会（履歴番号指定）
    // ===================================================================

    @Test
    void getByShiteiNoAndRno_指定した履歴番号のレコードが載る() {
        when(taxManagerRepository.findByJichitaiCdAndShiteiNoAndRno(JICHITAI_CD, SHITEI_NO, 2))
                .thenReturn(Optional.of(nokan(2)));

        TaxManagerForm form = service.getByShiteiNoAndRno(SHITEI_NO, 2);

        assertThat(form.isEdit()).isTrue();
        assertThat(form.getRno()).isEqualTo(2);
        assertThat(form.getManagerName()).isEqualTo("山田太郎");
    }

    @Test
    void getByShiteiNoAndRno_指定した履歴番号が存在しなければ新規モードになる() {
        when(taxManagerRepository.findByJichitaiCdAndShiteiNoAndRno(JICHITAI_CD, SHITEI_NO, 9))
                .thenReturn(Optional.empty());

        TaxManagerForm form = service.getByShiteiNoAndRno(SHITEI_NO, 9);

        assertThat(form.isEdit()).isFalse();
    }

    @Test
    void getByShiteiNoAndRno_最新取得ではなく履歴番号指定で取得する() {
        when(taxManagerRepository.findByJichitaiCdAndShiteiNoAndRno(JICHITAI_CD, SHITEI_NO, 2))
                .thenReturn(Optional.of(nokan(2)));

        service.getByShiteiNoAndRno(SHITEI_NO, 2);

        verify(taxManagerRepository).findByJichitaiCdAndShiteiNoAndRno(JICHITAI_CD, SHITEI_NO, 2);
        verify(taxManagerRepository, never()).findLatestByJichitaiCdAndShiteiNo(any(), any());
    }

    @Test
    void getByShiteiNoAndRno_特別徴収義務者の宛名番号がフォームに載る() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tokugimu()));

        TaxManagerForm form = service.getByShiteiNoAndRno(SHITEI_NO, 2);

        assertThat(form.getObligorAtenaNo()).isEqualTo("1001");
    }

    /** getByShiteiNo と同様、リポジトリの例外は伝播させる。 */
    @Test
    void getByShiteiNoAndRno_リポジトリが例外を投げたらそのまま伝播する() {
        when(taxManagerRepository.findByJichitaiCdAndShiteiNoAndRno(JICHITAI_CD, SHITEI_NO, 2))
                .thenThrow(new RuntimeException("DB接続エラー"));

        assertThatThrownBy(() -> service.getByShiteiNoAndRno(SHITEI_NO, 2))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB接続エラー");
    }

    // ===================================================================
    // saveByShiteiNo — 登録／編集
    // ===================================================================

    @Test
    void saveByShiteiNo_新規登録では履歴番号が1になり最新フラグ更新は行わない() {
        when(taxManagerRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(0);

        service.saveByShiteiNo(SHITEI_NO, validForm());

        TaxManager saved = savedEntity();
        assertThat(saved.getJichitaiCd()).isEqualTo(JICHITAI_CD);
        assertThat(saved.getShiteiNo()).isEqualTo(SHITEI_NO);
        assertThat(saved.getRno()).isEqualTo(1);
        assertThat(saved.getNewFlg()).isEqualTo("1");
        assertThat(saved.getDelFlg()).isEqualTo("0");
        verify(taxManagerRepository, never()).updateNewFlgToZero(any(), any());
    }

    @Test
    void saveByShiteiNo_既存がある場合は履歴番号が繰り上がり既存の最新フラグを落とす() {
        when(taxManagerRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(2);

        service.saveByShiteiNo(SHITEI_NO, validForm());

        assertThat(savedEntity().getRno()).isEqualTo(3);
        verify(taxManagerRepository).updateNewFlgToZero(JICHITAI_CD, SHITEI_NO);
    }

    @Test
    void saveByShiteiNo_特別徴収義務者と同一人物なら例外を投げて保存しない() {
        TaxManagerForm form = validForm();
        form.setAtenaNo("1001");
        form.setObligorAtenaNo("1001");

        assertThatThrownBy(() -> service.saveByShiteiNo(SHITEI_NO, form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("同一人物");

        verify(taxManagerRepository, never()).save(any());
    }

    @Test
    void saveByShiteiNo_免除区分なら同一人物でも登録でき個人情報はクリアされる() {
        when(taxManagerRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(0);
        TaxManagerForm form = validForm();
        form.setKbn("3");
        form.setAtenaNo("1001");
        form.setObligorAtenaNo("1001");
        form.setReason("納税管理人を選任しないため");

        service.saveByShiteiNo(SHITEI_NO, form);

        TaxManager saved = savedEntity();
        assertThat(saved.getKbn()).isEqualTo("3");
        assertThat(saved.getAtenaNo()).isNull();
        assertThat(saved.getName()).isNull();
        assertThat(saved.getNameKana()).isNull();
        assertThat(saved.getYubinNo()).isNull();
        assertThat(saved.getJusho()).isNull();
        assertThat(saved.getTel()).isNull();
        assertThat(saved.getRiyu()).isEqualTo("納税管理人を選任しないため");
    }

    @Test
    void saveByShiteiNo_宛名番号が空なら同一人物チェックをせず登録できる() {
        when(taxManagerRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(0);
        TaxManagerForm form = validForm();
        form.setAtenaNo("   ");
        form.setObligorAtenaNo("   ");

        service.saveByShiteiNo(SHITEI_NO, form);

        verify(taxManagerRepository).save(any(TaxManager.class));
    }

    @Test
    void saveByShiteiNo_登録日と申告日と入力項目がエンティティに転記される() {
        when(taxManagerRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(0);

        service.saveByShiteiNo(SHITEI_NO, validForm());

        TaxManager saved = savedEntity();
        assertThat(saved.getTorokuYmd()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(saved.getShinkokuYmd()).isEqualTo(LocalDate.of(2026, 3, 25));
        assertThat(saved.getAtenaNo()).isEqualTo("2002");
        assertThat(saved.getName()).isEqualTo("山田太郎");
        assertThat(saved.getNameKana()).isEqualTo("ヤマダタロウ");
        assertThat(saved.getYubinNo()).isEqualTo("060-0001");
        assertThat(saved.getJusho()).isEqualTo("札幌市中央区北1条西1丁目");
        assertThat(saved.getTel()).isEqualTo("011-000-0000");
        assertThat(saved.getRiyu()).isEqualTo("転居のため");
    }

    // ===================================================================
    // deleteByShiteiNo — 削除
    // ===================================================================

    @Test
    void deleteByShiteiNo_削除フラグを立てて前履歴を最新に戻す() {
        when(taxManagerRepository.findLatestByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(Optional.of(nokan(3)));

        service.deleteByShiteiNo(SHITEI_NO);

        verify(taxManagerRepository).updateDelFlgToOne(JICHITAI_CD, SHITEI_NO, 3);
        verify(taxManagerRepository).updateNewFlgToOneByRno(JICHITAI_CD, SHITEI_NO, 2);
    }

    @Test
    void deleteByShiteiNo_履歴が1件だけなら最新フラグの戻しは行わない() {
        when(taxManagerRepository.findLatestByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(Optional.of(nokan(1)));

        service.deleteByShiteiNo(SHITEI_NO);

        verify(taxManagerRepository).updateDelFlgToOne(JICHITAI_CD, SHITEI_NO, 1);
        verify(taxManagerRepository, never()).updateNewFlgToOneByRno(any(), any(), any());
    }

    @Test
    void deleteByShiteiNo_対象が存在しなければ例外を投げる() {
        when(taxManagerRepository.findLatestByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteByShiteiNo(SHITEI_NO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(SHITEI_NO);
    }
}

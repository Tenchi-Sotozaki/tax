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

@ExtendWith(MockitoExtension.class)
class TaxManagerServiceImplTest {

    @Mock TaxManagerRepository taxManagerRepository;
    @Mock TokugimuRepository tokugimuRepository;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks TaxManagerServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "S001";

    @BeforeEach
    void setUp() {
        lenient().when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    // ===================================================================
    // テストデータ
    // ===================================================================

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

    private Tokugimu tokugimu() {
        Tokugimu t = new Tokugimu();
        t.setAtenaNo(new BigDecimal("12345"));
        return t;
    }

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

    private TaxManager savedEntity() {
        ArgumentCaptor<TaxManager> captor = ArgumentCaptor.forClass(TaxManager.class);
        verify(taxManagerRepository).save(captor.capture());
        return captor.getValue();
    }

    // ===================================================================
    // No.24 isSamePerson - 同じ宛名番号
    // ===================================================================

    @Test
    void isSamePerson_同じ宛名番号_trueを返す() {
        assertThat(service.isSamePerson("A001", "A001")).isTrue();
    }

    // ===================================================================
    // No.25 isSamePerson - 異なる宛名番号
    // ===================================================================

    @Test
    void isSamePerson_異なる宛名番号_falseを返す() {
        assertThat(service.isSamePerson("A001", "B001")).isFalse();
    }

    // ===================================================================
    // No.26 isSamePerson - taxManagerAtenaNoがnull
    // ===================================================================

    @Test
    void isSamePerson_taxManagerAtenaNoがnull_falseを返す() {
        assertThat(service.isSamePerson(null, "A001")).isFalse();
    }

    // ===================================================================
    // No.27 isSamePerson - obligorAtenaNoがnull
    // ===================================================================

    @Test
    void isSamePerson_obligorAtenaNoがnull_falseを返す() {
        assertThat(service.isSamePerson("A001", null)).isFalse();
    }

    // ===================================================================
    // No.28 isSamePerson - taxManagerAtenaNoが空文字
    // ===================================================================

    @Test
    void isSamePerson_taxManagerAtenaNoが空文字_falseを返す() {
        assertThat(service.isSamePerson("", "A001")).isFalse();
    }

    // ===================================================================
    // No.29 isSamePerson - 前後スペースを除いて一致
    // ===================================================================

    @Test
    void isSamePerson_前後スペースを除いて一致_trueを返す() {
        assertThat(service.isSamePerson(" A001 ", "A001")).isTrue();
    }

    // ===================================================================
    // No.30 getByShiteiNo - 納税管理人あり
    // ===================================================================

    @Test
    void getByShiteiNo_納税管理人あり_editTrue_各フィールドが設定される() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tokugimu()));
        when(taxManagerRepository.findLatestByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(Optional.of(nokan(2)));
        when(taxManagerRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(2);
        when(taxManagerRepository.findMinRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(1);

        TaxManagerForm form = service.getByShiteiNo(SHITEI_NO);

        assertThat(form.isEdit()).isTrue();
        assertThat(form.getRno()).isEqualTo(2);
        assertThat(form.getManagerName()).isEqualTo("山田太郎");
        assertThat(form.getMaxRno()).isEqualTo(2);
        assertThat(form.getMinRno()).isEqualTo(1);
    }

    // ===================================================================
    // No.31 getByShiteiNo - 納税管理人なし
    // ===================================================================

    @Test
    void getByShiteiNo_納税管理人なし_editFalse_デフォルト値が設定される() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tokugimu()));
        when(taxManagerRepository.findLatestByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(Optional.empty());
        when(taxManagerRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(0);
        when(taxManagerRepository.findMinRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(0);

        TaxManagerForm form = service.getByShiteiNo(SHITEI_NO);

        assertThat(form.isEdit()).isFalse();
        assertThat(form.getMaxRno()).isEqualTo(0);
        assertThat(form.getMinRno()).isEqualTo(0);
    }

    // ===================================================================
    // No.32 getByShiteiNo - 特別徴収義務者あり
    // ===================================================================

    @Test
    void getByShiteiNo_特別徴収義務者あり_obligorAtenaNoが設定される() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tokugimu()));
        when(taxManagerRepository.findLatestByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(Optional.empty());

        TaxManagerForm form = service.getByShiteiNo(SHITEI_NO);

        assertThat(form.getObligorAtenaNo()).isEqualTo("12345");
    }

    // ===================================================================
    // No.33 getByShiteiNo - 特別徴収義務者なし
    // ===================================================================

    @Test
    void getByShiteiNo_特別徴収義務者なし_IllegalArgumentExceptionをスロー() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.getByShiteiNo(SHITEI_NO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("特別徴収義務者が設定されていません。");
    }

    // ===================================================================
    // No.34 getByShiteiNoAndRno - 指定rnoのレコードあり
    // ===================================================================

    @Test
    void getByShiteiNoAndRno_指定rnoのレコードあり_editTrue_各フィールドが設定される() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tokugimu()));
        when(taxManagerRepository.findByJichitaiCdAndShiteiNoAndRno(JICHITAI_CD, SHITEI_NO, 2))
                .thenReturn(Optional.of(nokan(2)));
        when(taxManagerRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(2);
        when(taxManagerRepository.findMinRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(1);

        TaxManagerForm form = service.getByShiteiNoAndRno(SHITEI_NO, 2);

        assertThat(form.isEdit()).isTrue();
        assertThat(form.getRno()).isEqualTo(2);
        assertThat(form.getMaxRno()).isEqualTo(2);
        assertThat(form.getMinRno()).isEqualTo(1);
    }

    // ===================================================================
    // No.35 getByShiteiNoAndRno - 指定rnoのレコードなし
    // ===================================================================

    @Test
    void getByShiteiNoAndRno_指定rnoのレコードなし_editFalse() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tokugimu()));
        when(taxManagerRepository.findByJichitaiCdAndShiteiNoAndRno(JICHITAI_CD, SHITEI_NO, 99))
                .thenReturn(Optional.empty());

        TaxManagerForm form = service.getByShiteiNoAndRno(SHITEI_NO, 99);

        assertThat(form.isEdit()).isFalse();
    }

    // ===================================================================
    // No.36 saveByShiteiNo - 新規登録（maxRno=0）
    // ===================================================================

    @Test
    void saveByShiteiNo_新規登録_rno1でsaveが呼ばれる_updateNewFlgToZeroは呼ばれない() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tokugimu()));
        when(taxManagerRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(0);

        service.saveByShiteiNo(SHITEI_NO, validForm());

        assertThat(savedEntity().getRno()).isEqualTo(1);
        verify(taxManagerRepository, never()).updateNewFlgToZero(any(), any());
    }

    // ===================================================================
    // No.37 saveByShiteiNo - 更新（maxRno=1）
    // ===================================================================

    @Test
    void saveByShiteiNo_更新_rno2でsaveが呼ばれる_updateNewFlgToZeroが呼ばれる() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tokugimu()));
        when(taxManagerRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(1);

        service.saveByShiteiNo(SHITEI_NO, validForm());

        assertThat(savedEntity().getRno()).isEqualTo(2);
        verify(taxManagerRepository).updateNewFlgToZero(JICHITAI_CD, SHITEI_NO);
    }

    // ===================================================================
    // No.38 saveByShiteiNo - kbn="3"（免除）
    // ===================================================================

    @Test
    void saveByShiteiNo_免除_個人情報フィールドがnullでsaveが呼ばれる() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tokugimu()));
        when(taxManagerRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(0);
        TaxManagerForm form = validForm();
        form.setKbn("3");
        form.setAtenaNo(null);
        form.setReason("免除理由");

        service.saveByShiteiNo(SHITEI_NO, form);

        TaxManager saved = savedEntity();
        assertThat(saved.getAtenaNo()).isNull();
        assertThat(saved.getName()).isNull();
        assertThat(saved.getYubinNo()).isNull();
    }

    // ===================================================================
    // No.39 saveByShiteiNo - kbn="1"（非免除）
    // ===================================================================

    @Test
    void saveByShiteiNo_非免除_個人情報フィールドが設定されてsaveが呼ばれる() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tokugimu()));
        when(taxManagerRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(0);

        service.saveByShiteiNo(SHITEI_NO, validForm());

        TaxManager saved = savedEntity();
        assertThat(saved.getName()).isEqualTo("山田太郎");
        assertThat(saved.getNewFlg()).isEqualTo("1");
        assertThat(saved.getDelFlg()).isEqualTo("0");
    }

    // ===================================================================
    // No.40 saveByShiteiNo - 非免除かつ同一人物
    // ===================================================================

    @Test
    void saveByShiteiNo_非免除かつ同一人物_IllegalArgumentExceptionをスロー() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tokugimu()));
        TaxManagerForm form = validForm();
        form.setAtenaNo("A001");
        form.setObligorAtenaNo("A001");

        assertThatThrownBy(() -> service.saveByShiteiNo(SHITEI_NO, form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("特別徴収義務者と同一人物のため、納税管理人として登録できません。");

        verify(taxManagerRepository, never()).save(any());
    }

    // ===================================================================
    // No.41 saveByShiteiNo - kbn="3"（免除）かつ同一宛名番号
    // ===================================================================

    @Test
    void saveByShiteiNo_免除かつ同一宛名番号_IllegalArgumentExceptionをスロー() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tokugimu()));
        TaxManagerForm form = validForm();
        form.setKbn("3");
        form.setAtenaNo("A001");
        form.setObligorAtenaNo("A001");

        assertThatThrownBy(() -> service.saveByShiteiNo(SHITEI_NO, form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("免除時は納税管理人選択不可です。");

        verify(taxManagerRepository, never()).save(any());
    }

    // ===================================================================
    // No.42 saveByShiteiNo - atenaNoがnull（kbn=1）
    // ===================================================================

    @Test
    void saveByShiteiNo_kbn1かつatenaNoがnull_IllegalArgumentExceptionをスロー() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tokugimu()));
        TaxManagerForm form = validForm();
        form.setKbn("1");
        form.setAtenaNo(null);

        assertThatThrownBy(() -> service.saveByShiteiNo(SHITEI_NO, form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("宛名番号は必須です。");

        verify(taxManagerRepository, never()).save(any());
    }

    // ===================================================================
    // No.43 deleteByShiteiNo - rno=1（履歴1件）
    // ===================================================================

    @Test
    void deleteByShiteiNo_rno1_delFlg更新のみ_前履歴のnewFlg更新は呼ばれない() {
        when(taxManagerRepository.findLatestByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(Optional.of(nokan(1)));

        service.deleteByShiteiNo(SHITEI_NO);

        verify(taxManagerRepository).updateDelFlgToOne(JICHITAI_CD, SHITEI_NO, 1);
        verify(taxManagerRepository, never()).updateNewFlgToOneByRno(any(), any(), any());
    }

    // ===================================================================
    // No.44 deleteByShiteiNo - rno=2（履歴2件）
    // ===================================================================

    @Test
    void deleteByShiteiNo_rno2_delFlg更新_前履歴のnewFlg更新が呼ばれる() {
        when(taxManagerRepository.findLatestByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(Optional.of(nokan(2)));

        service.deleteByShiteiNo(SHITEI_NO);

        verify(taxManagerRepository).updateDelFlgToOne(JICHITAI_CD, SHITEI_NO, 2);
        verify(taxManagerRepository).updateNewFlgToOneByRno(JICHITAI_CD, SHITEI_NO, 1);
    }

    // ===================================================================
    // No.45 deleteByShiteiNo - 削除対象なし
    // ===================================================================

    @Test
    void deleteByShiteiNo_削除対象なし_IllegalArgumentExceptionをスロー() {
        when(taxManagerRepository.findLatestByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteByShiteiNo(SHITEI_NO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(SHITEI_NO);
    }
}

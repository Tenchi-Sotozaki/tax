package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.TopPageConfigForm;
import jp.lg.asp.accommodation.entity.TopPageContent;
import jp.lg.asp.accommodation.entity.TopPageContentId;
import jp.lg.asp.accommodation.repository.TopPageContentRepository;
import jp.lg.asp.accommodation.service.impl.TopPageServiceImpl;
import org.junit.jupiter.api.DisplayName;

import java.util.Set;

@ExtendWith(MockitoExtension.class)
class TopPageServiceImplTest {

    private static final String JICHITAI_CD = "99999";

    @InjectMocks TopPageServiceImpl service;

    @Mock TopPageContentRepository repository;
    @Mock JichitaiContext jichitaiContext;

    // ─── findShared ──────────────────────────────────────────────────────────

    @Test
    void findShared_公開期間内の共有お知らせを取得する() {
        List<TopPageContent> expected = List.of(new TopPageContent());
        when(repository.findByJichitaiCdAndPostingStartDateLessThanEqualAndPostingEndDateGreaterThanEqual(
                anyString(), any(LocalDate.class), any(LocalDate.class))).thenReturn(expected);

        assertThat(service.findShared()).isEqualTo(expected);
    }

    // ─── loadForm ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("#確認1 loadForm 正常系 新規登録用の初期フォームが返る")
    void 確認1_loadForm_初期フォーム() {
        TopPageConfigForm form = service.loadForm();

        assertThat(form.getTitle()).isEmpty();
        assertThat(form.getHtmlContent()).isEmpty();
        assertThat(form.getSeq()).isNull();
        assertThat(form.getPostingStartDate()).isNull();
        assertThat(form.getPostingEndDate()).isNull();
        verify(repository, never()).findAll();
    }

    // ─── findAll ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("#確認2 findAll 正常系 登録済みコンテンツが全件返る")
    void 確認2_findAll_全件返る() {
        TopPageContent c1 = content(JICHITAI_CD, 1);
        TopPageContent c2 = content(JICHITAI_CD, 2);
        when(repository.findAll()).thenReturn(List.of(c1, c2));

        List<TopPageContent> result = service.findAll();

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("#確認3 findAll 異常系 0件の場合")
    void 確認3_findAll_0件() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.findAll()).isEmpty();
    }

    // ─── findBySeq ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("#確認4 findBySeq 正常系 存在するseqの場合")
    void 確認4_findBySeq_存在する() {
        TopPageContent c = content(JICHITAI_CD, 1);
        c.setTitle("お知らせ");
        when(repository.findById(new TopPageContentId(JICHITAI_CD, 1)))
                .thenReturn(Optional.of(c));

        TopPageContent result = service.findBySeq(1);

        assertThat(result.getSeq()).isEqualTo(1);
        assertThat(result.getTitle()).isEqualTo("お知らせ");

        ArgumentCaptor<TopPageContentId> captor = ArgumentCaptor.forClass(TopPageContentId.class);
        verify(repository).findById(captor.capture());
        assertThat(captor.getValue().getJichitaiCd()).isEqualTo(JICHITAI_CD);
    }

    @Test
    @DisplayName("#確認5 findBySeq 異常系 存在しないseqの場合")
    void 確認5_findBySeq_存在しない() {
        when(repository.findById(new TopPageContentId(JICHITAI_CD, 999)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findBySeq(999))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("データが存在しません。");
    }

    @Test
    @DisplayName("#確認6 findBySeq 異常系 seqがnullの場合")
    void 確認6_findBySeq_seqNull() {
        when(repository.findById(new TopPageContentId(JICHITAI_CD, null)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findBySeq(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("データが存在しません。");
    }

    // ─── save ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("#確認7 save 正常系 新規登録：採番して保存される")
    void 確認7_save_新規登録() {
        when(repository.getNextSeq(JICHITAI_CD)).thenReturn(3);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle("お知らせ");
        form.setHtmlContent("本文");
        form.setPostingStartDate(LocalDate.of(2026, 4, 1));
        form.setPostingEndDate(LocalDate.of(2026, 4, 30));

        service.save(form);

        ArgumentCaptor<TopPageContent> captor = ArgumentCaptor.forClass(TopPageContent.class);
        verify(repository, times(1)).save(captor.capture());
        TopPageContent saved = captor.getValue();
        assertThat(saved.getJichitaiCd()).isEqualTo(JICHITAI_CD);
        assertThat(saved.getSeq()).isEqualTo(3);
        assertThat(saved.getTitle()).isEqualTo("お知らせ");
        assertThat(saved.getHtmlContent()).isEqualTo("本文");
        assertThat(saved.getPostingStartDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(saved.getPostingEndDate()).isEqualTo(LocalDate.of(2026, 4, 30));
        verify(repository, times(1)).getNextSeq(JICHITAI_CD);
        verify(repository, never()).findById(any());
    }

    @Test
    @DisplayName("#確認8 save 正常系 更新：既存エンティティの項目が上書きされる")
    void 確認8_save_更新() {
        TopPageContent existing = content(JICHITAI_CD, 1);
        existing.setTitle("旧タイトル");
        existing.setHtmlContent("旧本文");
        existing.setAddUser("user0");
        when(repository.findById(new TopPageContentId(JICHITAI_CD, 1)))
                .thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TopPageConfigForm form = new TopPageConfigForm();
        form.setSeq(1);
        form.setTitle("新タイトル");
        form.setHtmlContent("新本文");
        form.setPostingStartDate(LocalDate.of(2026, 5, 1));
        form.setPostingEndDate(LocalDate.of(2026, 5, 31));

        service.save(form);

        ArgumentCaptor<TopPageContent> captor = ArgumentCaptor.forClass(TopPageContent.class);
        verify(repository, times(1)).save(captor.capture());
        TopPageContent saved = captor.getValue();
        assertThat(saved).isSameAs(existing);
        assertThat(saved.getTitle()).isEqualTo("新タイトル");
        assertThat(saved.getHtmlContent()).isEqualTo("新本文");
        assertThat(saved.getPostingStartDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(saved.getPostingEndDate()).isEqualTo(LocalDate.of(2026, 5, 31));
        assertThat(saved.getJichitaiCd()).isEqualTo(JICHITAI_CD);
        assertThat(saved.getSeq()).isEqualTo(1);
        assertThat(saved.getAddUser()).isEqualTo("user0");
        verify(repository, never()).getNextSeq(any());
    }

    @Test
    @DisplayName("#確認9 save 異常系 更新対象が存在しない場合")
    void 確認9_save_更新対象なし() {
        when(repository.findById(new TopPageContentId(JICHITAI_CD, 999)))
                .thenReturn(Optional.empty());

        TopPageConfigForm form = new TopPageConfigForm();
        form.setSeq(999);
        form.setTitle("お知らせ");
        form.setHtmlContent("本文");

        assertThatThrownBy(() -> service.save(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("データが存在しません。");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("#確認10 save 異常系 タイトル・本文がnullの場合")
    void 確認10_save_タイトル本文null() {
        when(repository.getNextSeq(JICHITAI_CD)).thenReturn(1);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle(null);
        form.setHtmlContent(null);
        form.setPostingStartDate(null);
        form.setPostingEndDate(null);

        service.save(form);

        ArgumentCaptor<TopPageContent> captor = ArgumentCaptor.forClass(TopPageContent.class);
        verify(repository, times(1)).save(captor.capture());
        TopPageContent saved = captor.getValue();
        assertThat(saved.getTitle()).isNull();
        assertThat(saved.getHtmlContent()).isNull();
        assertThat(saved.getPostingStartDate()).isNull();
        assertThat(saved.getPostingEndDate()).isNull();
    }

    @Test
    @DisplayName("#確認11 save 正常系 登録が1件もない状態での採番")
    void 確認11_save_初回採番() {
        when(repository.getNextSeq(JICHITAI_CD)).thenReturn(1);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle("お知らせ");
        form.setHtmlContent("本文");

        service.save(form);

        ArgumentCaptor<TopPageContent> captor = ArgumentCaptor.forClass(TopPageContent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getSeq()).isEqualTo(1);
    }

    @Test
    @DisplayName("#確認12 save 異常系 保存先の自治体コードが固定値であること")
    void 確認12_save_jichitaiCd固定値() {
        when(repository.getNextSeq(JICHITAI_CD)).thenReturn(1);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle("お知らせ");
        form.setHtmlContent("本文");

        service.save(form);

        ArgumentCaptor<TopPageContent> captor = ArgumentCaptor.forClass(TopPageContent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getJichitaiCd()).isEqualTo(JICHITAI_CD);
        verify(jichitaiContext, never()).getJichitaiCd();
    }

    // ─── delete ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("#確認13 delete 正常系 対象が物理削除される")
    void 確認13_delete_物理削除() {
        TopPageContent c = content(JICHITAI_CD, 1);
        when(repository.findById(new TopPageContentId(JICHITAI_CD, 1)))
                .thenReturn(Optional.of(c));

        service.delete(1);

        ArgumentCaptor<TopPageContent> captor = ArgumentCaptor.forClass(TopPageContent.class);
        verify(repository, times(1)).delete(captor.capture());
        assertThat(captor.getValue().getSeq()).isEqualTo(1);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("#確認14 delete 異常系 存在しないseqの場合")
    void 確認14_delete_存在しないseq() {
        when(repository.findById(new TopPageContentId(JICHITAI_CD, 999)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(999))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("データが存在しません。");
        verify(repository, never()).delete(any());
    }

    // ─── バリデーション ──────────────────────────────────────────────────────

    @Test
    @DisplayName("#確認15 バリデーション 正常系 必須項目がすべて入力されている場合")
    void 確認15_validation_正常() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle("お知らせ");
        form.setHtmlContent("本文");
        form.setPostingStartDate(LocalDate.of(2026, 4, 1));
        form.setPostingEndDate(LocalDate.of(2026, 4, 30));

        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    @DisplayName("#確認16 バリデーション 異常系 タイトルがnullの場合")
    void 確認16_validation_タイトルNull() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle(null);
        form.setHtmlContent("本文");
        form.setPostingStartDate(LocalDate.of(2026, 4, 1));

        Set<ConstraintViolation<TopPageConfigForm>> violations = validator.validate(form);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("title");
        assertThat(violations.iterator().next().getMessage()).isEqualTo("タイトルを入力してください");
    }

    @Test
    @DisplayName("#確認17 バリデーション 異常系 タイトルが空文字の場合")
    void 確認17_validation_タイトル空文字() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle("");
        form.setHtmlContent("本文");
        form.setPostingStartDate(LocalDate.of(2026, 4, 1));

        Set<ConstraintViolation<TopPageConfigForm>> violations = validator.validate(form);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("タイトルを入力してください");
    }

    @Test
    @DisplayName("#確認18 バリデーション 異常系 タイトルが空白のみの場合")
    void 確認18_validation_タイトル空白のみ() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle("   ");
        form.setHtmlContent("本文");
        form.setPostingStartDate(LocalDate.of(2026, 4, 1));

        Set<ConstraintViolation<TopPageConfigForm>> violations = validator.validate(form);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("タイトルを入力してください");
    }

    @Test
    @DisplayName("#確認19 バリデーション 異常系 本文がnullの場合")
    void 確認19_validation_本文Null() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle("お知らせ");
        form.setHtmlContent(null);
        form.setPostingStartDate(LocalDate.of(2026, 4, 1));

        Set<ConstraintViolation<TopPageConfigForm>> violations = validator.validate(form);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("htmlContent");
        assertThat(violations.iterator().next().getMessage()).isEqualTo("内容を入力してください");
    }

    @Test
    @DisplayName("#確認20 バリデーション 異常系 本文が空文字の場合")
    void 確認20_validation_本文空文字() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle("お知らせ");
        form.setHtmlContent("");
        form.setPostingStartDate(LocalDate.of(2026, 4, 1));

        Set<ConstraintViolation<TopPageConfigForm>> violations = validator.validate(form);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("内容を入力してください");
    }

    @Test
    @DisplayName("#確認21 バリデーション 異常系 本文が空白のみの場合")
    void 確認21_validation_本文空白のみ() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle("お知らせ");
        form.setHtmlContent("   ");
        form.setPostingStartDate(LocalDate.of(2026, 4, 1));

        Set<ConstraintViolation<TopPageConfigForm>> violations = validator.validate(form);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("内容を入力してください");
    }

    @Test
    @DisplayName("#確認22 バリデーション 異常系 掲載開始日が未入力の場合")
    void 確認22_validation_開始日未入力() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle("お知らせ");
        form.setHtmlContent("本文");
        form.setPostingStartDate(null);
        form.setPostingEndDate(null);

        Set<ConstraintViolation<TopPageConfigForm>> violations = validator.validate(form);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("postingStartDate");
        assertThat(violations.iterator().next().getMessage()).isEqualTo("掲載開始日を入力してください");
    }

    @Test
    @DisplayName("#確認23 バリデーション 異常系 掲載開始日が終了日より後の場合はDTO単体では違反なし")
    void 確認23_validation_開始日が終了日より後はDTO単体違反なし() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle("お知らせ");
        form.setHtmlContent("本文");
        form.setPostingStartDate(LocalDate.of(2026, 4, 30));
        form.setPostingEndDate(LocalDate.of(2026, 4, 1));

        // 相関チェックはコントローラ側で行うためDTO単体では違反なし
        assertThat(validator.validate(form)).isEmpty();
    }

    // ─── helper ──────────────────────────────────────────────────────────────

    private TopPageContent content(String jichitaiCd, Integer seq) {
        TopPageContent c = new TopPageContent();
        c.setJichitaiCd(jichitaiCd);
        c.setSeq(seq);
        return c;
    }

    // =====================================================================
    // トップページ_単体テストチェックリスト（#5〜#8）
    // =====================================================================

    private static final String SHARED_JICHITAI_CD = "99999";

    @Test
    @DisplayName("#5 findShared 正常系 共有自治体コードと本日日付で掲載中のものを取得する")
    void findShared_共有自治体コードと本日日付で取得する() {
        LocalDate today = LocalDate.now();
        TopPageContent content = new TopPageContent();
        content.setSeq(1);
        content.setTitle("お知らせ");
        when(repository.findByJichitaiCdAndPostingStartDateLessThanEqualAndPostingEndDateGreaterThanEqual(
                SHARED_JICHITAI_CD, today, today)).thenReturn(List.of(content));

        List<TopPageContent> result = service.findShared();

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getSeq());

        ArgumentCaptor<String> jichitaiCdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(repository).findByJichitaiCdAndPostingStartDateLessThanEqualAndPostingEndDateGreaterThanEqual(
                jichitaiCdCaptor.capture(), startCaptor.capture(), endCaptor.capture());

        // 全自治体共有の固定値。ログイン自治体のコードを使わないこと
        assertEquals(SHARED_JICHITAI_CD, jichitaiCdCaptor.getValue());
        assertEquals(today, startCaptor.getValue());
        assertEquals(today, endCaptor.getValue());
    }

    @Test
    @DisplayName("#6 findShared 異常系 掲載中のコンテンツが無い場合")
    void findShared_掲載中のコンテンツが無い() {
        LocalDate today = LocalDate.now();
        when(repository.findByJichitaiCdAndPostingStartDateLessThanEqualAndPostingEndDateGreaterThanEqual(
                SHARED_JICHITAI_CD, today, today)).thenReturn(List.of());

        List<TopPageContent> result = service.findShared();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("#7 findShared 異常系 掲載終了日NULLのレコードを取得できること")
    void findShared_掲載終了日がNULLのレコードを取得できる() {
        LocalDate today = LocalDate.now();
        TopPageContent content = new TopPageContent();
        content.setSeq(1);
        content.setPostingStartDate(today.minusDays(1));
        content.setPostingEndDate(null);
        when(repository.findByJichitaiCdAndPostingStartDateLessThanEqualAndPostingEndDateGreaterThanEqual(
                SHARED_JICHITAI_CD, today, today)).thenReturn(List.of(content));

        List<TopPageContent> result = service.findShared();

        assertEquals(1, result.size());
        assertNull(result.get(0).getPostingEndDate());
    }

    @Test
    @DisplayName("#8 findShared 異常系 リポジトリが例外をスローした場合")
    void findShared_リポジトリが例外をスロー() {
        LocalDate today = LocalDate.now();
        when(repository.findByJichitaiCdAndPostingStartDateLessThanEqualAndPostingEndDateGreaterThanEqual(
                SHARED_JICHITAI_CD, today, today)).thenThrow(new RuntimeException("DB error"));

        RuntimeException e = assertThrows(RuntimeException.class, () -> service.findShared());
        assertEquals("DB error", e.getMessage());
    }
}

package jp.lg.asp.accommodation.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
import jp.lg.asp.accommodation.repository.TopPageContentRepository;
import jp.lg.asp.accommodation.service.impl.TopPageServiceImpl;

@ExtendWith(MockitoExtension.class)
class TopPageServiceImplTest {

    @InjectMocks
    private TopPageServiceImpl service;

    @Mock
    private TopPageContentRepository repository;
    
    @Mock
    private JichitaiContext jichitaiContext;

    @Test
    void findShared_公開期間内の共有お知らせを取得する() {

        // Arrange
        List<TopPageContent> expected = List.of(new TopPageContent());

        when(repository.findByJichitaiCdAndPostingStartDateLessThanEqualAndPostingEndDateGreaterThanEqual(
                        anyString(),
                        any(LocalDate.class),
                        any(LocalDate.class))).thenReturn(expected);

        // Act
        List<TopPageContent> actual = service.findShared();

        // Assert
        assertEquals(expected, actual);

        verify(repository).findByJichitaiCdAndPostingStartDateLessThanEqualAndPostingEndDateGreaterThanEqual(
                        anyString(),
                        any(LocalDate.class),
                        any(LocalDate.class));
    }
    
   
    @Test
    void loadForm_初期フォームを生成する() {

        TopPageConfigForm form = service.loadForm();

        assertEquals("", form.getTitle());
        assertEquals("", form.getHtmlContent());
    }
    
    @Test
    void save_新規登録する() {

        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle("タイトル");
        form.setHtmlContent("本文");

        when(repository.getNextSeq("99999")).thenReturn(1);

        service.save(form);

        ArgumentCaptor<TopPageContent> captor = ArgumentCaptor.forClass(TopPageContent.class);

        verify(repository).save(captor.capture());

        TopPageContent saved = captor.getValue();

        assertEquals("99999", saved.getJichitaiCd());
        assertEquals(1, saved.getSeq());
        assertEquals("タイトル", saved.getTitle());
        assertEquals("本文", saved.getHtmlContent());
    }
    
    @Test
    void save_更新する() {

        TopPageConfigForm form = new TopPageConfigForm();

        form.setSeq(1);
        form.setTitle("更新タイトル");

        TopPageContent entity = new TopPageContent();

        when(repository.findById(any())).thenReturn(Optional.of(entity));

        service.save(form);

        assertEquals("更新タイトル", entity.getTitle());

        verify(repository).save(entity);
    }
    
    @Test
    void findAll_一覧取得する() {

        List<TopPageContent> expected = List.of(new TopPageContent());

        when(repository.findAll()).thenReturn(expected);

        List<TopPageContent> result = service.findAll();

        assertEquals(expected, result);
    }
    
    @Test
    void findBySeq_シーケンスで取得する() {

        TopPageContent content = new TopPageContent();

        when(repository.findById(any())).thenReturn(Optional.of(content));

        TopPageContent result = service.findBySeq(1);

        assertEquals(content, result);
    }

    @Test
    void findBySeq_データが存在しない場合は例外() {

        when(repository.findById(any())).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.findBySeq(1));
    }
    
    @Test
    void delete_削除する() {

        TopPageContent content = new TopPageContent();

        when(repository.findById(any())).thenReturn(Optional.of(content));

        service.delete(1);

        verify(repository).delete(content);
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

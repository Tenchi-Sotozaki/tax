package jp.lg.asp.accommodation.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
}
package jp.lg.asp.accommodation.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.TopPageConfigForm;
import jp.lg.asp.accommodation.entity.TopPageContent;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.service.MarkdownService;
import jp.lg.asp.accommodation.service.TopPageService;

@ExtendWith(MockitoExtension.class)
class TopPageControllerTest {
	
	private static final String SCREEN_ID_CONFIG = ScreenManagement.TOP_PAGE_CONFIG;
	
	@Mock
	private ScreenAccessChecker accessChecker;

    @InjectMocks
    private TopPageController controller;

    @Mock
    private TopPageService topPageService;

    @Mock
    private MarkdownService markdownService;

    @Mock
    private Model model;
    
    @Mock
    private RedirectAttributes redirectAttributes;
    
    @Mock
    private JichitaiRepository jichitaiRepository;

    @Mock
    private JichitaiContext jichitaiContext;

    @Test
    void index_お知らせを取得して画面に表示する() {

        // Arrange
        TopPageContent content = new TopPageContent();
        content.setTitle("タイトル");
        content.setHtmlContent("本文");

        List<TopPageContent> sharedList = List.of(content);

        when(topPageService.findShared()).thenReturn(sharedList);
        when(markdownService.toHtml("タイトル"))
                .thenReturn("<h1>タイトル</h1>");
        when(markdownService.toHtml("本文"))
                .thenReturn("<p>本文</p>");

        // Act
        String result = controller.index(model);

        // Assert
        assertEquals("top/topPage", result);
        assertEquals("<h1>タイトル</h1>", content.getTitleHtml());
        assertEquals("<p>本文</p>", content.getContentHtml());

        verify(topPageService).findShared();
        verify(markdownService).toHtml("タイトル");
        verify(markdownService).toHtml("本文");
        verify(model).addAttribute("sharedList", sharedList);
    }
    
    @Test
    void index_お知らせが0件でも画面表示できる() {

	    // Arrange
	    List<TopPageContent> emptyList = Collections.emptyList();
	
	    when(topPageService.findShared()).thenReturn(emptyList);
	
	    // Act
	    String result = controller.index(model);
	
	    // Assert
	    assertEquals("top/topPage", result);
	
	    verify(topPageService).findShared();
	    verify(model).addAttribute("sharedList", emptyList);
	
	    // お知らせがないためMarkdown変換は呼ばれない
	    verifyNoInteractions(markdownService);
    }
    
    @Test
    void list_一覧を表示する() {

        List<TopPageContent> items = List.of(new TopPageContent());

        when(topPageService.findAll()).thenReturn(items);

        String result = controller.list(10, model);

        assertEquals("top/topPageConfigDaicho", result);

        verify(topPageService).findAll();
        verify(model).addAttribute("items", items);
        verify(model).addAttribute("pageSize", 10);
    }
    
    @Test
    void config_新規登録画面を表示する() {

        TopPageConfigForm form = new TopPageConfigForm();

        when(topPageService.loadForm()).thenReturn(form);

        String result = controller.config(model);

        assertEquals("top/topPageConfig", result);

        verify(accessChecker).checkAccess(SCREEN_ID_CONFIG);
        verify(model).addAttribute("form", form);
    }
    
    @Test
    void preview_プレビューを表示する() {

        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle("タイトル");
        form.setHtmlContent("本文");

        when(markdownService.toHtml("タイトル"))
                .thenReturn("<h1>タイトル</h1>");
        when(markdownService.toHtml("本文"))
                .thenReturn("<p>本文</p>");

        String result = controller.preview(form, model);

        assertEquals("top/topPageConfig", result);

        verify(model).addAttribute(
                "previewTitle",
                "<h1>タイトル</h1>");

        verify(model).addAttribute(
                "previewHtml",
                "<p>本文</p>");

        verify(model).addAttribute("form", form);
        verify(model).addAttribute("preview", true);
    }
    
    @Test
    void save_保存する() {

        TopPageConfigForm form = new TopPageConfigForm();

        String result =
                controller.save(form, model, redirectAttributes);

        assertEquals("redirect:/top/config", result);

        verify(topPageService).save(form);

        verify(redirectAttributes)
                .addFlashAttribute(
                        "successMessage",
                        "トップページコンテンツを保存しました。");
    }
    
    @Test
    void save_保存失敗時は編集画面を表示する() {

        TopPageConfigForm form = new TopPageConfigForm();

        doThrow(new RuntimeException("DBエラー"))
                .when(topPageService)
                .save(form);

        String result =
                controller.save(form, model, redirectAttributes);

        assertEquals("top/topPageConfig", result);

        verify(model).addAttribute("form", form);

        verify(model).addAttribute(
                eq("errorMessage"),
                eq("保存に失敗しました: DBエラー"));
    }
    
    @Test
    void edit_編集画面を表示する() {

        TopPageContent content = new TopPageContent();

        content.setSeq(1);
        content.setTitle("タイトル");
        content.setHtmlContent("本文");

        when(topPageService.findBySeq(1))
                .thenReturn(content);

        String result =
                controller.edit(1, model);

        assertEquals("top/topPageConfig", result);

        verify(accessChecker).checkAccess(SCREEN_ID_CONFIG);

        ArgumentCaptor<TopPageConfigForm> captor =
                ArgumentCaptor.forClass(TopPageConfigForm.class);

        verify(model).addAttribute(
                eq("form"),
                captor.capture());

        assertEquals(1, captor.getValue().getSeq());
        assertEquals("本文", captor.getValue().getHtmlContent());
        assertEquals("タイトル", captor.getValue().getTitle());
    }
    
    @Test
    void delete_削除する() {

        String result =
                controller.delete(1, redirectAttributes);

        assertEquals(
                "redirect:/top/topPageConfigDaicho",
                result);

        verify(accessChecker).checkAccess(SCREEN_ID_CONFIG);
        verify(topPageService).delete(1);

        verify(redirectAttributes)
                .addFlashAttribute(
                        "successMessage",
                        "削除しました。");
    }   
    
}
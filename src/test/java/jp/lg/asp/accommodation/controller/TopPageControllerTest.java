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
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.TopPageConfigForm;
import jp.lg.asp.accommodation.entity.TopPageContent;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.service.MarkdownService;
import jp.lg.asp.accommodation.service.TopPageService;
import org.junit.jupiter.api.DisplayName;
import org.springframework.ui.ExtendedModelMap;

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

        BindingResult bindingResult =
                new BeanPropertyBindingResult(form, "form");

        String result =
                controller.save(
                        form,
                        bindingResult,
                        model,
                        redirectAttributes);

        assertEquals("redirect:/top/config", result);

        verify(topPageService).save(form);

        verify(redirectAttributes)
                .addFlashAttribute(
                        "successMessage",
                        "トップページコンテンツを保存しました。");
    }
    
    @Test
    void save_保存失敗時は編集画面を表示する() {
        // 1. テストデータの準備（バリデーションエラーにならない正常な値をセット）
        TopPageConfigForm form = new TopPageConfigForm();
        // 必要に応じて form に title 等をセットしてバリデーションを通過させる
        form.setTitle("テストタイトル"); 

        // 2. モックの設定（any を使用して確実に例外を発生させる）
        doThrow(new RuntimeException("DBエラー"))
                .when(topPageService)
                .save(any(TopPageConfigForm.class));

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");

        // 3. 実行
        String result = controller.save(form, bindingResult, model, redirectAttributes);

        // 4. 検証
        assertEquals("top/topPageConfig", result);
        verify(model).addAttribute("errorMessage", "保存に失敗しました: DBエラー");
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
    
    @Test
    void save_タイトル未入力の場合() {

        TopPageConfigForm form = new TopPageConfigForm();

        BindingResult bindingResult =
                new BeanPropertyBindingResult(form, "form");
        bindingResult.rejectValue(
                "title",
                "NotBlank",
                "タイトルを入力してください");

        String result =
                controller.save(
                        form,
                        bindingResult,
                        model,
                        redirectAttributes);

        assertEquals("top/topPageConfig", result);

        verify(topPageService, never()).save(any());
    }
    
    @Test
    void save_内容未入力の場合() {

        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle("テストタイトル");
        form.setHtmlContent(""); // 未入力

        BindingResult bindingResult =
                new BeanPropertyBindingResult(form, "form");

        bindingResult.rejectValue(
                "htmlContent",
                "NotBlank",
                "内容を入力してください");

        String result =
                controller.save(
                        form,
                        bindingResult,
                        model,
                        redirectAttributes);

        assertEquals("top/topPageConfig", result);

        verify(topPageService, never()).save(any());

        verifyNoInteractions(redirectAttributes);
    }

    // =====================================================================
    // トップページ_単体テストチェックリスト（#1〜#4）
    // =====================================================================

    private TopPageContent content(Integer seq, String title, String htmlContent) {
        TopPageContent c = new TopPageContent();
        c.setSeq(seq);
        c.setTitle(title);
        c.setHtmlContent(htmlContent);
        return c;
    }

    @Test
    @DisplayName("#1 index 正常系 掲載中の共有コンテンツがHTML変換されて画面に渡る")
    void index_掲載中の共有コンテンツがHTML変換されて画面に渡る() {
        TopPageContent content1 = content(1, "# お知らせ", "**本文1**");
        TopPageContent content2 = content(2, "## 更新情報", "本文2");
        when(topPageService.findShared()).thenReturn(List.of(content1, content2));
        when(markdownService.toHtml("# お知らせ")).thenReturn("<h1>お知らせ</h1>");
        when(markdownService.toHtml("**本文1**")).thenReturn("<p><strong>本文1</strong></p>");
        when(markdownService.toHtml("## 更新情報")).thenReturn("<h2>更新情報</h2>");
        when(markdownService.toHtml("本文2")).thenReturn("<p>本文2</p>");

        Model model = new ExtendedModelMap();
        String result = controller.index(model);

        assertEquals("top/topPage", result);

        @SuppressWarnings("unchecked")
        List<TopPageContent> sharedList = (List<TopPageContent>) model.asMap().get("sharedList");
        assertEquals(2, sharedList.size());
        assertEquals("<h1>お知らせ</h1>", sharedList.get(0).getTitleHtml());
        assertEquals("<p><strong>本文1</strong></p>", sharedList.get(0).getContentHtml());
        assertEquals("<h2>更新情報</h2>", sharedList.get(1).getTitleHtml());
        assertEquals("<p>本文2</p>", sharedList.get(1).getContentHtml());

        // 元の title / htmlContent が書き換えられていないこと
        assertEquals("# お知らせ", sharedList.get(0).getTitle());
        assertEquals("**本文1**", sharedList.get(0).getHtmlContent());
        assertEquals("## 更新情報", sharedList.get(1).getTitle());
        assertEquals("本文2", sharedList.get(1).getHtmlContent());

        // 各コンテンツのタイトル・本文が正確な引数で渡されていること
        ArgumentCaptor<String> markdownCaptor = ArgumentCaptor.forClass(String.class);
        verify(markdownService, times(4)).toHtml(markdownCaptor.capture());
        assertEquals(List.of("# お知らせ", "**本文1**", "## 更新情報", "本文2"),
                markdownCaptor.getAllValues());
    }

    @Test
    @DisplayName("#2 index 異常系 掲載中のコンテンツが0件の場合")
    void index_掲載中のコンテンツが0件() {
        when(topPageService.findShared()).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        String result = controller.index(model);

        assertEquals("top/topPage", result);

        List<?> sharedList = (List<?>) model.asMap().get("sharedList");
        assertNotNull(sharedList);
        assertTrue(sharedList.isEmpty());

        verify(markdownService, never()).toHtml(any());
    }

    @Test
    @DisplayName("#3 index 異常系 タイトル・本文が null のコンテンツがある場合")
    void index_タイトルと本文がnull() {
        TopPageContent target = content(1, null, null);
        when(topPageService.findShared()).thenReturn(List.of(target));
        when(markdownService.toHtml(null)).thenReturn("");

        Model model = new ExtendedModelMap();
        assertDoesNotThrow(() -> controller.index(model));

        @SuppressWarnings("unchecked")
        List<TopPageContent> sharedList = (List<TopPageContent>) model.asMap().get("sharedList");
        assertEquals("", sharedList.get(0).getTitleHtml());
        assertEquals("", sharedList.get(0).getContentHtml());

        verify(markdownService, times(2)).toHtml(null);
    }

    @Test
    @DisplayName("#4 index 異常系 サービスが例外をスローした場合")
    void index_サービスが例外をスロー() {
        when(topPageService.findShared()).thenThrow(new RuntimeException("DB error"));

        Model model = new ExtendedModelMap();

        // try-catch を持たないため、そのまま伝播すること
        RuntimeException e = assertThrows(RuntimeException.class, () -> controller.index(model));
        assertEquals("DB error", e.getMessage());

        verify(markdownService, never()).toHtml(any());
    }
}

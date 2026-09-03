package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.TopPageConfigForm;
import jp.lg.asp.accommodation.entity.TopPageContent;
import jp.lg.asp.accommodation.service.MarkdownService;
import jp.lg.asp.accommodation.service.TopPageService;

@ExtendWith(MockitoExtension.class)
class TopPageControllerTest {

    private static final String SCREEN_ID_CONFIG = ScreenManagement.TOP_PAGE_CONFIG;

    @Mock ScreenAccessChecker accessChecker;
    @Mock TopPageService topPageService;
    @Mock MarkdownService markdownService;

    @InjectMocks TopPageController controller;

    // ─── index ───────────────────────────────────────────────────────────────

    @Test
    void index_お知らせを取得して画面に表示する() {
        TopPageContent content = new TopPageContent();
        content.setTitle("タイトル");
        content.setHtmlContent("本文");
        List<TopPageContent> sharedList = List.of(content);

        when(topPageService.findShared()).thenReturn(sharedList);
        when(markdownService.toHtml("タイトル")).thenReturn("<h1>タイトル</h1>");
        when(markdownService.toHtml("本文")).thenReturn("<p>本文</p>");

        Model model = new ExtendedModelMap();
        String result = controller.index(model);

        assertThat(result).isEqualTo("top/topPage");
        assertThat(content.getTitleHtml()).isEqualTo("<h1>タイトル</h1>");
        assertThat(content.getContentHtml()).isEqualTo("<p>本文</p>");
        verify(topPageService).findShared();
        verify(model).addAttribute("sharedList", sharedList);
    }

    @Test
    void index_お知らせが0件でも画面表示できる() {
        List<TopPageContent> emptyList = Collections.emptyList();
        when(topPageService.findShared()).thenReturn(emptyList);

        Model model = new ExtendedModelMap();
        String result = controller.index(model);

        assertThat(result).isEqualTo("top/topPage");
        verify(topPageService).findShared();
        verifyNoInteractions(markdownService);
    }

    // ─── list ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("#確認1 list 正常系 一覧初期表示：件数と表示件数がモデルに設定される")
    void 確認1_list_一覧初期表示() {
        TopPageContent c1 = content("99999", 1, "お知らせ",
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));
        TopPageContent c2 = content("99999", 2, "更新情報", null, null);
        when(topPageService.findAll()).thenReturn(List.of(c1, c2));

        Model model = new ExtendedModelMap();
        String result = controller.list(10, model);

        assertThat(result).isEqualTo("top/topPageConfigDaicho");
        assertThat((List<TopPageContent>) model.getAttribute("items")).hasSize(2);
        assertThat(((List<TopPageContent>) model.getAttribute("items")).get(0).getSeq()).isEqualTo(1);
        assertThat(model.getAttribute("pageSize")).isEqualTo(10);
        verify(topPageService, times(1)).findAll();
        verify(accessChecker, times(1)).checkAccess(SCREEN_ID_CONFIG);
    }

    @Test
    @DisplayName("#確認2 list 正常系 表示件数を指定した場合")
    void 確認2_list_表示件数指定() {
        when(topPageService.findAll()).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        controller.list(50, model);

        assertThat(model.getAttribute("pageSize")).isEqualTo(50);
        verify(accessChecker, times(1)).checkAccess(SCREEN_ID_CONFIG);
    }

    @Test
    @DisplayName("#確認3 list 異常系 登録が0件の場合")
    void 確認3_list_0件() {
        when(topPageService.findAll()).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        String result = controller.list(10, model);

        assertThat(result).isEqualTo("top/topPageConfigDaicho");
        assertThat((List<TopPageContent>) model.getAttribute("items")).isEmpty();
        verify(accessChecker, times(1)).checkAccess(SCREEN_ID_CONFIG);
    }

    @Test
    @DisplayName("#確認4 list 異常系 表示件数に0が指定された場合")
    void 確認4_list_pageSize0() {
        when(topPageService.findAll()).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        assertThatCode(() -> controller.list(0, model)).doesNotThrowAnyException();
        assertThat(model.getAttribute("pageSize")).isEqualTo(0);
        verify(accessChecker, times(1)).checkAccess(SCREEN_ID_CONFIG);
    }

    @Test
    @DisplayName("#確認5 list 異常系 表示件数に負数が指定された場合")
    void 確認5_list_pageSizeNegative() {
        when(topPageService.findAll()).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        assertThatCode(() -> controller.list(-1, model)).doesNotThrowAnyException();
        assertThat(model.getAttribute("pageSize")).isEqualTo(-1);
        verify(accessChecker, times(1)).checkAccess(SCREEN_ID_CONFIG);
    }

    // ─── config ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("#確認6 config 正常系 新規登録画面の初期表示")
    void 確認6_config_新規登録画面初期表示() {
        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle("");
        form.setHtmlContent("");
        when(topPageService.loadForm()).thenReturn(form);

        Model model = new ExtendedModelMap();
        String result = controller.config(model);

        assertThat(result).isEqualTo("top/topPageConfig");
        assertThat(((TopPageConfigForm) model.getAttribute("form")).getSeq()).isNull();
        assertThat(((TopPageConfigForm) model.getAttribute("form")).getTitle()).isEmpty();
        assertThat(((TopPageConfigForm) model.getAttribute("form")).getHtmlContent()).isEmpty();
        assertThat(model.getAttribute("preview")).isNull();
        verify(topPageService, times(1)).loadForm();
        verify(accessChecker, times(1)).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    // ─── preview ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("#確認7 preview 正常系 入力内容がプレビュー用HTMLに変換される")
    void 確認7_preview_HTML変換() {
        when(markdownService.toHtml("# タイトル")).thenReturn("<h1>タイトル</h1>");
        when(markdownService.toHtml("**本文**")).thenReturn("<p><strong>本文</strong></p>");

        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle("# タイトル");
        form.setHtmlContent("**本文**");

        Model model = new ExtendedModelMap();
        String result = controller.preview(form, model);

        assertThat(result).isEqualTo("top/topPageConfig");
        assertThat(model.getAttribute("previewTitle")).isEqualTo("<h1>タイトル</h1>");
        assertThat(model.getAttribute("previewHtml")).isEqualTo("<p><strong>本文</strong></p>");
        assertThat(model.getAttribute("preview")).isEqualTo(true);
        assertThat(model.getAttribute("form")).isSameAs(form);
        verify(topPageService, never()).save(any());
        verify(accessChecker, times(1)).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    @Test
    @DisplayName("#確認8 preview 異常系 タイトル・本文が未入力の場合")
    void 確認8_preview_未入力() {
        when(markdownService.toHtml(null)).thenReturn("");

        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle(null);
        form.setHtmlContent(null);

        Model model = new ExtendedModelMap();
        String result = controller.preview(form, model);

        assertThat(result).isEqualTo("top/topPageConfig");
        assertThat(model.getAttribute("previewTitle")).isEqualTo("");
        assertThat(model.getAttribute("previewHtml")).isEqualTo("");
        assertThat(model.getAttribute("preview")).isEqualTo(true);
        verify(accessChecker, times(1)).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    @Test
    @DisplayName("#確認9 preview 正常系 編集中データ（seqあり）のプレビュー")
    void 確認9_preview_seq有り() {
        when(markdownService.toHtml("タイトル")).thenReturn("<p>タイトル</p>");
        when(markdownService.toHtml("本文")).thenReturn("<p>本文</p>");

        TopPageConfigForm form = new TopPageConfigForm();
        form.setSeq(1);
        form.setTitle("タイトル");
        form.setHtmlContent("本文");

        Model model = new ExtendedModelMap();
        controller.preview(form, model);

        assertThat(((TopPageConfigForm) model.getAttribute("form")).getSeq()).isEqualTo(1);
        assertThat(model.getAttribute("preview")).isEqualTo(true);
        verify(accessChecker, times(1)).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    // ─── save ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("#確認10 save 正常系 新規登録が成功する")
    void 確認10_save_新規登録成功() {
        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle("お知らせ");
        form.setHtmlContent("本文");
        form.setPostingStartDate(LocalDate.of(2026, 4, 1));
        form.setPostingEndDate(LocalDate.of(2026, 4, 30));

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        Model model = new ExtendedModelMap();

        String result = controller.save(form, bindingResult, model, redirectAttributes);

        assertThat(result).isEqualTo("redirect:/top/topPageConfigDaicho");
        verify(topPageService, times(1)).save(form);
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
                .isEqualTo("トップページコンテンツを保存しました。");
        assertThat(model.getAttribute("errorMessage")).isNull();
        assertThat(model.getAttribute("validationErrors")).isNull();
        verify(accessChecker, times(1)).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    @Test
    @DisplayName("#確認11 save 正常系 更新が成功する")
    void 確認11_save_更新成功() {
        TopPageConfigForm form = new TopPageConfigForm();
        form.setSeq(1);
        form.setTitle("お知らせ（更新）");
        form.setHtmlContent("本文");
        form.setPostingStartDate(LocalDate.of(2026, 4, 1));
        form.setPostingEndDate(LocalDate.of(2026, 4, 30));

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        Model model = new ExtendedModelMap();

        String result = controller.save(form, bindingResult, model, redirectAttributes);

        assertThat(result).isEqualTo("redirect:/top/topPageConfigDaicho");

        ArgumentCaptor<TopPageConfigForm> captor = ArgumentCaptor.forClass(TopPageConfigForm.class);
        verify(topPageService, times(1)).save(captor.capture());
        assertThat(captor.getValue().getSeq()).isEqualTo(1);

        assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
                .isEqualTo("トップページコンテンツを保存しました。");
        verify(accessChecker, times(1)).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    @Test
    @DisplayName("#確認12 save 正常系 掲載開始日と終了日が同日の場合")
    void 確認12_save_開始日終了日同日() {
        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle("お知らせ");
        form.setHtmlContent("本文");
        form.setPostingStartDate(LocalDate.of(2026, 4, 1));
        form.setPostingEndDate(LocalDate.of(2026, 4, 1));

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        Model model = new ExtendedModelMap();

        String result = controller.save(form, bindingResult, model, redirectAttributes);

        assertThat(result).isEqualTo("redirect:/top/topPageConfigDaicho");
        verify(topPageService, times(1)).save(form);
        verify(accessChecker, times(1)).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    @Test
    @DisplayName("#確認13 save 異常系 掲載開始日が未入力の場合")
    void 確認13_save_開始日未入力() {
        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle("お知らせ");
        form.setHtmlContent("本文");
        form.setPostingStartDate(null);
        form.setPostingEndDate(null);

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        bindingResult.rejectValue("postingStartDate", "NotNull", "掲載開始日を入力してください");

        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String result = controller.save(form, bindingResult, model, redirectAttributes);

        assertThat(result).isEqualTo("top/topPageConfig");
        assertThat((List<String>) model.getAttribute("validationErrors"))
                .contains("掲載開始日を入力してください");
        verify(topPageService, never()).save(any());
        verify(accessChecker, times(1)).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    @Test
    @DisplayName("#確認14 save 異常系 掲載終了日が未入力の場合は正常保存")
    void 確認14_save_終了日未入力は正常保存() {
        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle("お知らせ");
        form.setHtmlContent("本文");
        form.setPostingStartDate(LocalDate.of(2026, 4, 1));
        form.setPostingEndDate(null);

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        Model model = new ExtendedModelMap();

        String result = controller.save(form, bindingResult, model, redirectAttributes);

        assertThat(result).isEqualTo("redirect:/top/topPageConfigDaicho");
        verify(topPageService, times(1)).save(form);
        verify(accessChecker, times(1)).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    @Test
    @DisplayName("#確認15 save 異常系 タイトルが未入力の場合")
    void 確認15_save_タイトル未入力() {
        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle("");
        form.setHtmlContent("本文");

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        bindingResult.rejectValue("title", "NotBlank", "タイトルを入力してください");

        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String result = controller.save(form, bindingResult, model, redirectAttributes);

        assertThat(result).isEqualTo("top/topPageConfig");
        assertThat((List<String>) model.getAttribute("validationErrors"))
                .contains("タイトルを入力してください");
        verify(topPageService, never()).save(any());
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage")).isNull();
        verify(accessChecker, times(1)).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    @Test
    @DisplayName("#確認16 save 異常系 本文が未入力の場合")
    void 確認16_save_本文未入力() {
        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle("お知らせ");
        form.setHtmlContent("");

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        bindingResult.rejectValue("htmlContent", "NotBlank", "内容を入力してください");

        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String result = controller.save(form, bindingResult, model, redirectAttributes);

        assertThat(result).isEqualTo("top/topPageConfig");
        assertThat((List<String>) model.getAttribute("validationErrors"))
                .contains("内容を入力してください");
        verify(topPageService, never()).save(any());
        verify(accessChecker, times(1)).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    @Test
    @DisplayName("#確認17 save 異常系 掲載開始日が終了日より後の場合")
    void 確認17_save_開始日が終了日より後() {
        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle("お知らせ");
        form.setHtmlContent("本文");
        form.setPostingStartDate(LocalDate.of(2026, 4, 30));
        form.setPostingEndDate(LocalDate.of(2026, 4, 1));

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String result = controller.save(form, bindingResult, model, redirectAttributes);

        assertThat(result).isEqualTo("top/topPageConfig");
        assertThat(bindingResult.getFieldError("postingStartDate")).isNotNull();
        assertThat(bindingResult.getFieldError("postingStartDate").getCode()).isEqualTo("date.reverse");
        assertThat((List<String>) model.getAttribute("validationErrors"))
                .contains("掲載開始日は掲載終了日以前の日付を入力してください。");
        verify(topPageService, never()).save(any());
        verify(accessChecker, times(1)).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    @Test
    @DisplayName("#確認18 save 異常系 入力エラーが複数ある場合")
    void 確認18_save_複数エラー() {
        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle("");
        form.setHtmlContent("");
        form.setPostingStartDate(LocalDate.of(2026, 4, 30));
        form.setPostingEndDate(LocalDate.of(2026, 4, 1));

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        bindingResult.rejectValue("title", "NotBlank", "タイトルを入力してください");
        bindingResult.rejectValue("htmlContent", "NotBlank", "内容を入力してください");

        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String result = controller.save(form, bindingResult, model, redirectAttributes);

        assertThat(result).isEqualTo("top/topPageConfig");
        // 相関チェックで date.reverse が追加され合計3件
        assertThat((List<String>) model.getAttribute("validationErrors")).hasSize(3);
        verify(topPageService, never()).save(any());
        verify(accessChecker, times(1)).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    @Test
    @DisplayName("#確認19 save 異常系 サービスが例外をスローした場合")
    void 確認19_save_サービス例外() {
        TopPageConfigForm form = new TopPageConfigForm();
        form.setTitle("お知らせ");
        form.setHtmlContent("本文");
        form.setPostingStartDate(LocalDate.of(2026, 4, 1));
        form.setPostingEndDate(LocalDate.of(2026, 4, 30));

        doThrow(new RuntimeException("DB error")).when(topPageService).save(any());

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String result = controller.save(form, bindingResult, model, redirectAttributes);

        assertThat(result).isEqualTo("top/topPageConfig");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("保存に失敗しました: DB error");
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage")).isNull();
        verify(accessChecker, times(1)).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    @Test
    @DisplayName("#確認20 save 異常系 存在しないseqを更新しようとした場合")
    void 確認20_save_存在しないseq() {
        TopPageConfigForm form = new TopPageConfigForm();
        form.setSeq(999);
        form.setTitle("お知らせ");
        form.setHtmlContent("本文");
        form.setPostingStartDate(LocalDate.of(2026, 4, 1));

        doThrow(new IllegalArgumentException("データが存在しません。")).when(topPageService).save(any());

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String result = controller.save(form, bindingResult, model, redirectAttributes);

        assertThat(result).isEqualTo("top/topPageConfig");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("保存に失敗しました: データが存在しません。");
        verify(accessChecker, times(1)).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    // ─── edit ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("#確認21 edit 正常系 既存データが編集フォームに設定される")
    void 確認21_edit_既存データ設定() {
        TopPageContent c = content("99999", 1, "お知らせ",
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));
        c.setHtmlContent("本文");
        when(topPageService.findBySeq(1)).thenReturn(c);

        Model model = new ExtendedModelMap();
        String result = controller.edit(1, model);

        assertThat(result).isEqualTo("top/topPageConfig");
        TopPageConfigForm form = (TopPageConfigForm) model.getAttribute("form");
        assertThat(form.getSeq()).isEqualTo(1);
        assertThat(form.getTitle()).isEqualTo("お知らせ");
        assertThat(form.getHtmlContent()).isEqualTo("本文");
        assertThat(form.getPostingStartDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(form.getPostingEndDate()).isEqualTo(LocalDate.of(2026, 4, 30));
        verify(topPageService, times(1)).findBySeq(1);
        verify(accessChecker, times(1)).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    @Test
    @DisplayName("#確認22 edit 異常系 掲載期間が未設定のデータを編集する場合")
    void 確認22_edit_掲載期間未設定() {
        TopPageContent c = content("99999", 1, "お知らせ", null, null);
        c.setHtmlContent("本文");
        when(topPageService.findBySeq(1)).thenReturn(c);

        Model model = new ExtendedModelMap();
        assertThatCode(() -> controller.edit(1, model)).doesNotThrowAnyException();

        TopPageConfigForm form = (TopPageConfigForm) model.getAttribute("form");
        assertThat(form.getPostingStartDate()).isNull();
        assertThat(form.getPostingEndDate()).isNull();
        verify(accessChecker, times(1)).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    @Test
    @DisplayName("#確認23 edit 異常系 存在しないseqを指定した場合")
    void 確認23_edit_存在しないseq() {
        when(topPageService.findBySeq(999))
                .thenThrow(new IllegalArgumentException("データが存在しません。"));

        Model model = new ExtendedModelMap();
        assertThatThrownBy(() -> controller.edit(999, model))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(model.getAttribute("form")).isNull();
        verify(accessChecker, times(1)).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    // ─── delete ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("#確認24 delete 正常系 削除が成功する")
    void 確認24_delete_成功() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String result = controller.delete(1, redirectAttributes);

        assertThat(result).isEqualTo("redirect:/top/topPageConfigDaicho");
        verify(topPageService, times(1)).delete(1);
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
                .isEqualTo("削除しました。");
        verify(accessChecker, times(1)).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    @Test
    @DisplayName("#確認25 delete 異常系 存在しないseqを指定した場合")
    void 確認25_delete_存在しないseq() {
        doThrow(new IllegalArgumentException("データが存在しません。"))
                .when(topPageService).delete(999);

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        assertThatThrownBy(() -> controller.delete(999, redirectAttributes))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage")).isNull();
        verify(accessChecker, times(1)).checkWriteAccess(SCREEN_ID_CONFIG);
    }

    // ─── helper ──────────────────────────────────────────────────────────────

    private TopPageContent content(String jichitaiCd, Integer seq, String title,
            LocalDate start, LocalDate end) {
        TopPageContent c = new TopPageContent();
        c.setJichitaiCd(jichitaiCd);
        c.setSeq(seq);
        c.setTitle(title);
        c.setPostingStartDate(start);
        c.setPostingEndDate(end);
        return c;
    }
}

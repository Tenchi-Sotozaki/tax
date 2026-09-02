package jp.lg.asp.accommodation.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jp.lg.asp.accommodation.service.impl.MarkdownServiceImpl;

/**
 * トップページ_単体テストチェックリスト（#9〜#13）
 *
 * MarkdownServiceImpl は外部依存を持たないため、モックではなく実インスタンスを使用する。
 */
class MarkdownServiceImplTest {

    private final MarkdownServiceImpl service = new MarkdownServiceImpl();

    @Test
    @DisplayName("#9 toHtml 正常系 Markdown の見出し・強調・表が HTML に変換される")
    void toHtml_見出しと強調と表がHTMLに変換される() {
        String markdown = "# 見出し\n\n**太字**\n\n| a | b |\n|---|---|\n| 1 | 2 |";

        String html = service.toHtml(markdown);

        assertTrue(html.contains("<h1>"), "見出しが <h1> に変換されること");
        assertTrue(html.contains("<strong>"), "強調が <strong> に変換されること");
        assertTrue(html.contains("<table>"), "表が <table> に変換されること");
    }

    @Test
    @DisplayName("#10 toHtml 異常系 null・空文字・空白のみの場合")
    void toHtml_nullと空文字と空白のみ() {
        assertEquals("", service.toHtml(null));
        assertEquals("", service.toHtml(""));
        assertEquals("", service.toHtml("   "));
    }

    @Test
    @DisplayName("#11 toHtml 異常系 script タグが含まれる場合（XSS）")
    void toHtml_scriptタグが除去される() {
        String html = service.toHtml("<script>alert(1)</script>お知らせ");

        // トップページは th:utext で出力するため、ここでのサニタイズが唯一の防御
        assertFalse(html.contains("<script"), "script タグが除去されること");
    }

    @Test
    @DisplayName("#12 toHtml 異常系 イベントハンドラ属性が含まれる場合（XSS）")
    void toHtml_イベントハンドラ属性が除去される() {
        String html = service.toHtml("<img src=\"x\" onerror=\"alert(1)\">");

        assertFalse(html.toLowerCase().contains("onerror"), "onerror 属性が除去されること");
    }

    @Test
    @DisplayName("#13 toHtml 異常系 javascript: スキームのリンクが含まれる場合（XSS）")
    void toHtml_javascriptスキームが除去される() {
        String html = service.toHtml("[link](javascript:alert(1))");

        assertFalse(html.contains("javascript:"), "javascript: スキームが除去されること");
    }
}

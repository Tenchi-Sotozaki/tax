package jp.lg.asp.accommodation.service;

public interface MarkdownService {

    /**
     * MarkdownをHTMLへ変換しサニタイズする
     *
     * @param markdown Markdown文字列
     * @return サニタイズ済みHTML
     */
    String toHtml(String markdown);
}
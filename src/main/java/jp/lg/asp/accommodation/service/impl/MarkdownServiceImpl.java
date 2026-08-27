package jp.lg.asp.accommodation.service.impl;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;

import jp.lg.asp.accommodation.service.MarkdownService;

@Service
public class MarkdownServiceImpl implements MarkdownService {

    private final Parser parser;
    private final HtmlRenderer renderer;
    private final Safelist safelist;

    public MarkdownServiceImpl() {
        this.parser = Parser.builder().build();
        this.renderer = HtmlRenderer.builder().build();

        /*
         * relaxed:
         * a, p, ul, ol, li, strong, em, blockquote などを許可
         */
        this.safelist = Safelist.relaxed();

        // Markdownでよく使うタグを追加
        safelist.addTags(
                "h1", "h2", "h3", "h4", "h5", "h6",
                "pre", "code",
                "table", "thead", "tbody", "tr", "th", "td");

        safelist.addAttributes("code", "class");
        safelist.addAttributes("pre", "class");
    }

    public String toHtml(String markdown) {

        if (markdown == null || markdown.isBlank()) {
            return "";
        }

        // Markdown → HTML
        String html = renderer.render(
                parser.parse(markdown)
        );

        // XSS対策
        return Jsoup.clean(html, safelist);
    }
}

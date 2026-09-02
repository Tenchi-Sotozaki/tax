package jp.lg.asp.accommodation.service.impl;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

import jp.lg.asp.accommodation.service.MarkdownService;

@Service
public class MarkdownServiceImpl implements MarkdownService {

    private final Parser parser;
    private final HtmlRenderer renderer;
    private final Safelist safelist;

    public MarkdownServiceImpl() {
        // 表（GFM tables）を有効にする。Parser / HtmlRenderer の双方に同じ options を渡す
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(TablesExtension.create()));

        this.parser = Parser.builder(options).build();
        this.renderer = HtmlRenderer.builder(options).build();

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

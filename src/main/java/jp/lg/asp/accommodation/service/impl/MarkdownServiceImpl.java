package jp.lg.asp.accommodation.service.impl;

<<<<<<< HEAD
import java.util.List;
=======
import java.util.Arrays;
>>>>>>> refs/remotes/origin/master

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

<<<<<<< HEAD
=======
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
>>>>>>> refs/remotes/origin/master
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
<<<<<<< HEAD
        // 表（GFM tables）を有効にする。Parser / HtmlRenderer の双方に同じ options を渡す
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(TablesExtension.create()));

        this.parser = Parser.builder(options).build();
        this.renderer = HtmlRenderer.builder(options).build();
=======
        // 1. Flexmarkの拡張機能（GFM機能）を有効化
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),        // テーブル (GFM)
                StrikethroughExtension.create(), // 打ち消し線 (~~text~~)
                TaskListExtension.create()       // タスクリスト (- [x])
        ));
>>>>>>> refs/remotes/origin/master

        this.parser = Parser.builder(options).build();
        this.renderer = HtmlRenderer.builder(options).build();

        // 2. Jsoupの安全なSafelistを設定
        this.safelist = Safelist.relaxed()
        		// relaxed() に含まれないタグのみ追加 (pre, code, strike は元からあるため除外)
                .addTags("h1", "h2", "h3", "h4", "h5", "h6")
                .addTags("hr", "del", "s", "sup", "sub")
                .addTags("table", "thead", "tbody", "tr", "th", "td")
                .addTags("input")
                // 属性の追加
                .addAttributes("th", "align")
                .addAttributes("td", "align")
                .addAttributes("code", "class")
                .addAttributes("pre", "class")
                .addAttributes("input", "type", "checked", "disabled")
                // relaxed() に含まれないプロトコル (#: ページ内リンク) のみ追加
                .addProtocols("a", "href", "#");
    }

    @Override
    public String toHtml(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return "";
        }
        
        // Markdown -> HTML 変換
        String rawHtml = renderer.render(parser.parse(markdown));
        
        // Jsoup によるサニタイズ処理
        return Jsoup.clean(rawHtml, safelist);
    }
}

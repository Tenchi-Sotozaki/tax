package jp.lg.asp.accommodation.controller;

import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jp.lg.asp.accommodation.dto.FukaDaichoForm;
import jp.lg.asp.accommodation.service.FukaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/declaration") // 仕様書に合わせて申告関連のパスに
public class FukaController {

    private final FukaService fukaService;
    
    // HTMLファイル名（規約のロウワーキャメルケース）
    private static final String DAICHO_VIEW = "fuka/tFukaDaicho";

    /**
     * 納入金額管理台帳 表示・検索処理
     */
    @GetMapping("/payment-ledger/{shiteiNo}")
    public String showDaicho(
            @PathVariable String shiteiNo,
            @RequestParam(required = false) String nendo,
            @RequestParam(required = false) String status,
            Model model) {

        // 初回アクセス時は現在の年度をデフォルトとする簡易ロジック
    	if (nendo == null) {
    	    LocalDate now = LocalDate.now();
    	    int nendoInt = now.getMonthValue() >= 4 ? now.getYear() : now.getYear() - 1;
    	    // 数値を文字列に変換して代入
    	    nendo = String.valueOf(nendoInt);
    	}

        // Serviceを呼び出して画面用データを生成
        FukaDaichoForm form = fukaService.getDaichoData(shiteiNo, nendo, status);

        model.addAttribute("fukaDaichoForm", form);       // HTML 23行目の ${fukaDaichoForm...} 用
        model.addAttribute("searchForm", form);           // 検索フォーム th:object="${searchForm}" 用
        model.addAttribute("items", form.getItems());       // 明細一覧用
        model.addAttribute("totalAmount", form.getTotalAmount()); // 合計金額用
        model.addAttribute("obligorId", shiteiNo);          // ボタンリンク用

        return DAICHO_VIEW;
    }
    
    
}
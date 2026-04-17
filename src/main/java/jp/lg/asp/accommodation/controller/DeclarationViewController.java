package jp.lg.asp.accommodation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/declarations")
public class DeclarationViewController {

    /**
     * 宿泊税情報登録/編集画面
     * GET /declarations/form        → 新規登録
     * GET /declarations/form?id=xxx → 編集
     */
    @GetMapping("/form")
    public String form() {
        return "declaration/form";
    }
}

package jp.lg.asp.accommodation.controller;


import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.dto.FukaDaichoForm;
import jp.lg.asp.accommodation.dto.FukaDeclarationForm;
import jp.lg.asp.accommodation.service.FukaService;
import jp.lg.asp.accommodation.service.FukaValidatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
/**
 * 宿泊税納入情報の管理に関するリクエストを制御するコントローラークラス。
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/declaration")
public class FukaController {

    private final FukaService fukaService;

    private static final String DAICHO_VIEW = "fuka/tFukaDaicho";

    private static final String CONFIG_VIEW = "fuka/tFukaConfig";
    
    private final FukaValidatorService fukaValidatorService;

    /**
     * 納入金額管理台帳を表示し、検索処理を行う。
     * @param shiteiNo 指定番号
     * @param nendo 対象年度
     * @param status 抽出ステータス
     * @param model モデルオブジェクト
     * @return 画面パス
     */
    @GetMapping("/payment-ledger/{shiteiNo}")
    public String showDaicho(
            @PathVariable String shiteiNo,
            @RequestParam(name = "nendo", required = false) String nendo,
            @RequestParam(required = false) String status,
            Model model) {

        // 年度指定がない場合のデフォルト年度設定
        if (nendo == null || nendo.isEmpty()) {
            LocalDate now = LocalDate.now();
            int nendoInt = now.getMonthValue() >= 4 ? now.getYear() : now.getYear() - 1;
            nendo = String.valueOf(nendoInt);
        }

        // サービスを呼び出して表示用データを生成
        FukaDaichoForm form = fukaService.getDaichoData(shiteiNo, nendo, status);

        model.addAttribute("fukaDaichoForm", form);
        model.addAttribute("searchForm", form);
        model.addAttribute("items", form.getItems());
        model.addAttribute("totalAmount", form.getTotalAmount());
        model.addAttribute("obligorId", shiteiNo);

        return DAICHO_VIEW;
    }

    /**
     * 宿泊税情報の登録画面を表示する。
     * @param shiteiNo 指定番号
     * @param month 対象年月
     * @param redirectAttributes リダイレクト属性
     * @param model モデルオブジェクト
     * @return 画面パス
     */
    @GetMapping("/register/{shiteiNo}")
    public String register(
            @PathVariable("shiteiNo") String shiteiNo,
            @RequestParam(name = "month", required = false) String month,
            RedirectAttributes redirectAttributes,
            Model model) {

        // 二重申告を防止するためのアクセスガード
        if (month != null && !month.isEmpty()) {
            if (fukaService.isAlreadyRegistered(shiteiNo, month)) {
                redirectAttributes.addFlashAttribute("errorMessage", "申告済みのデータです。「照会」ボタンから確認してください。");
                return "redirect:/declaration/payment-ledger/" + shiteiNo;
            }
        }

        FukaDeclarationForm form = fukaService.getDeclarationFormForRegister(shiteiNo, month);
        model.addAttribute("fukaDeclarationForm", form);
        return CONFIG_VIEW;
    }

    /**
     * 宿泊税情報の編集画面を表示する。
     * @param shiteiNo 指定番号
     * @param nendo 対象年度
     * @param kibetsu 期別
     * @param redirectAttributes リダイレクト属性
     * @param model モデルオブジェクト
     * @return 画面パス
     */
    @GetMapping("/edit/{shiteiNo}/{nendo}/{kibetsu}")
    public String showEdit(
            @PathVariable("shiteiNo") String shiteiNo,
            @PathVariable("nendo") String nendo,
            @PathVariable("kibetsu") Integer kibetsu,
            RedirectAttributes redirectAttributes,
            Model model) {

        // 未申告データに対する編集アクセス制限
        if (!fukaService.isAlreadyRegisteredByKibetsu(shiteiNo, nendo, kibetsu)) {
            redirectAttributes.addFlashAttribute("errorMessage", "未申告のデータです。「新規登録」ボタンから登録してください。");
            return "redirect:/declaration/payment-ledger/" + shiteiNo;
        }

        FukaDeclarationForm form = fukaService.getDeclarationFormForEdit(shiteiNo, nendo, kibetsu);
        form.setView(false);
        form.setEdit(true);
        model.addAttribute("fukaDeclarationForm", form);
        return CONFIG_VIEW;
    }

    /**
     * 宿泊税情報の照会画面を表示する。
     * @param shiteiNo 指定番号
     * @param nendo 対象年度
     * @param kibetsu 期別
     * @param redirectAttributes リダイレクト属性
     * @param model モデルオブジェクト
     * @return 画面パス
     */
    @GetMapping("/view/{shiteiNo}/{nendo}/{kibetsu}")
    public String showView(
            @PathVariable("shiteiNo") String shiteiNo,
            @PathVariable("nendo") String nendo,
            @PathVariable("kibetsu") Integer kibetsu,
            RedirectAttributes redirectAttributes,
            Model model) {

        // 未申告データに対する照会アクセス制限
        if (!fukaService.isAlreadyRegisteredByKibetsu(shiteiNo, nendo, kibetsu)) {
            redirectAttributes.addFlashAttribute("errorMessage", "未申告のデータです。「新規登録」ボタンから登録してください。");
            return "redirect:/declaration/payment-ledger/" + shiteiNo;
        }

        FukaDeclarationForm form = fukaService.getDeclarationFormForView(shiteiNo, nendo, kibetsu);
        form.setView(true);
        form.setEdit(false);
        
        log.info("【DEBUG-2:照会直前】DBから読み込んだ fukaKbn = {}", form.getFukaKbn());
        model.addAttribute("fukaDeclarationForm", form);
        return CONFIG_VIEW;
    }

    /**
     * 宿泊税情報の保存（登録・更新）を行う。
     * @param form 申告情報フォーム
     * @param result バリデーション結果
     * @param model モデルオブジェクト
     * @param redirectAttributes リダイレクト属性
     * @return 画面パス
     */
    @PostMapping("/save")
    public String save(@Validated @ModelAttribute("fukaDeclarationForm") FukaDeclarationForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
    	
    	log.info("【DEBUG-1:保存直前】画面から届いた fukaKbn = {}", form.getFukaKbn());

        // 1. Springが自動判定したバリデーション結果を確認
        if (bindingResult.hasErrors()) {
            bindingResult.getAllErrors().forEach(error -> {
                log.error("【バリデーションエラー】項目: {}, 内容: {}", error.getObjectName(), error.getDefaultMessage());
            });
            fukaService.hydrateFormMetadata(form); // 画面再表示用にメタ情報をセット
            return "fuka/tFukaConfig"; 
        }

        // 2. 独自の相関チェック（定額・定率の分岐チェック）
        try {
            fukaValidatorService.validateCorrelation(form);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            fukaService.hydrateFormMetadata(form);
            return "fuka/tFukaConfig";
        }
        
        // 3. 編集区分チェック（変数名を bindingResult に修正）
        if (form.isEdit() && !org.springframework.util.StringUtils.hasText(form.getModificationCategory())) {
            bindingResult.rejectValue("modificationCategory", "error.modificationCategory", "編集時は変更の区分を選択してください。");
            fukaService.hydrateFormMetadata(form);
            return "fuka/tFukaConfig";
        }

        // 4. 税額の不整合チェック
        if (!form.isTaxCheckBypassed() && fukaService.hasTaxAmountDiscrepancy(form)) {
            fukaService.hydrateFormMetadata(form);
            model.addAttribute("showTaxWarningModal", true);
            return "fuka/tFukaConfig";
        }

        // 5. 保存処理
        try {
            fukaService.saveDeclaration(form);
            // ...成功時のリダイレクト処理...
            return "redirect:/declaration/payment-ledger/" + form.getShiteiNo();
        } catch (RuntimeException e) {
            log.error("保存処理中にエラーが発生しました", e);
            model.addAttribute("errorMessage", "保存に失敗しました：" + e.getMessage());
            fukaService.hydrateFormMetadata(form);
            return "fuka/tFukaConfig";
        }
    }
}
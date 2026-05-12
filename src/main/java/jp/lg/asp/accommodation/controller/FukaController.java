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
import jp.lg.asp.accommodation.repository.FukaMonthlyDeclarationRepository;
import jp.lg.asp.accommodation.service.FukaService;
import jp.lg.asp.accommodation.service.FukaValidatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/declaration")
public class FukaController {

	private final FukaService fukaService;

	private static final String DAICHO_VIEW = "fuka/tFukaDaicho";

	private static final String CONFIG_VIEW = "fuka/tFukaConfig";

	private final FukaMonthlyDeclarationRepository repository;
	
    private final FukaValidatorService validatorService;
	/**
	 * 納入金額管理台帳 表示・検索処理
	 */
	@GetMapping("/payment-ledger/{shiteiNo}")
	public String showDaicho(
			@PathVariable String shiteiNo,
			@RequestParam(name = "nendo", required = false) String nendo, // パラメータ名を明示
			@RequestParam(required = false) String status,
			Model model) {

		// 1. 年度がない場合のデフォルト設定（ここだけ残す）
		if (nendo == null || nendo.isEmpty()) {
			LocalDate now = LocalDate.now();
			int nendoInt = now.getMonthValue() >= 4 ? now.getYear() : now.getYear() - 1;
			nendo = String.valueOf(nendoInt);
		}
		// Serviceを呼び出して画面用データを生成
		FukaDaichoForm form = fukaService.getDaichoData(shiteiNo, nendo, status);

		model.addAttribute("fukaDaichoForm", form); // HTML 23行目の ${fukaDaichoForm...} 用
		model.addAttribute("searchForm", form); // 検索フォーム th:object="${searchForm}" 用
		model.addAttribute("items", form.getItems()); // 明細一覧用
		model.addAttribute("totalAmount", form.getTotalAmount()); // 合計金額用
		model.addAttribute("obligorId", shiteiNo); // ボタンリンク用

		return DAICHO_VIEW;
	}
	// =======================================================
	// 宿泊税情報 登録/編集/照会
	// =======================================================
	/**
     * 宿泊税情報 登録画面表示
     */
    @GetMapping("/register/{shiteiNo}")
    public String register(
            @PathVariable("shiteiNo") String shiteiNo,
            @RequestParam(name = "month", required = false) String month,
            RedirectAttributes redirectAttributes, // 🔴 追加：リダイレクト先にメッセージを渡すための部品
            Model model) {
        
        // 💡 サーバーサイドでのアクセスガード（済なら新規登録させない）
        if (month != null && !month.isEmpty()) {
            if (fukaService.isAlreadyRegistered(shiteiNo, month)) {
                redirectAttributes.addFlashAttribute("errorMessage", "申告済みのデータです。「照会」ボタンから確認してください。");
                return "redirect:/declaration/payment-ledger/" + shiteiNo; // 台帳画面へ強制送還
            }
        }

        FukaDeclarationForm form = fukaService.getDeclarationFormForRegister(shiteiNo, month);
        model.addAttribute("fukaDeclarationForm", form);
        return CONFIG_VIEW; 
    }

	/**
	 * 宿泊税情報 照会画面表示処理
	 */
	@GetMapping("/view/{shiteiNo}/{nendo}/{kibetsu}")
	public String showView(
			@PathVariable("shiteiNo") String shiteiNo,
			@PathVariable("nendo") String nendo,
			@PathVariable("kibetsu") Integer kibetsu,
			RedirectAttributes redirectAttributes, // 🔴 追加
			Model model) {
		
        // 💡 サーバーサイドでのアクセスガード（未なら照会させない）
        if (!fukaService.isAlreadyRegisteredByKibetsu(shiteiNo, nendo, kibetsu)) {
            redirectAttributes.addFlashAttribute("errorMessage", "未申告のデータです。「新規登録」ボタンから登録してください。");
            return "redirect:/declaration/payment-ledger/" + shiteiNo;
        }

		FukaDeclarationForm form = fukaService.getDeclarationFormForView(shiteiNo, nendo, kibetsu);
		form.setView(true);
		form.setEdit(false);
		model.addAttribute("fukaDeclarationForm", form);
		return CONFIG_VIEW;
	}

	/**
	 * 宿泊税情報 編集画面表示処理
	 */
	@GetMapping("/edit/{shiteiNo}/{nendo}/{kibetsu}")
	public String showEdit(
			@PathVariable("shiteiNo") String shiteiNo,
			@PathVariable("nendo") String nendo,
			@PathVariable("kibetsu") Integer kibetsu,
			RedirectAttributes redirectAttributes, // 🔴 追加
			Model model) {

        // 💡 編集画面も同様に未申告データをガードする
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
     * 宿泊税情報の保存（登録・更新）
     */
    @PostMapping("/save")
    public String save(@Validated @ModelAttribute("fukaDeclarationForm") FukaDeclarationForm form, 
                       BindingResult result, 
                       Model model,
                       RedirectAttributes redirectAttributes) {

        // 1. 基本的なバリデーションエラー（相関チェックなど）がある場合
        if (result.hasErrors()) {
            // エラーがある場合は、画面に必要なメタデータ（施設名など）を再セットして戻すぜ
            fukaService.hydrateFormMetadata(form);
            return "fuka/tFukaConfig";
        }

        try {
            // 2. サービスの保存処理を実行
            // ここで「更生理由未入力」などの RuntimeException が投げられる可能性があるぜ
            fukaService.saveDeclaration(form);

            // 成功した場合は一覧画面へリダイレクト
            redirectAttributes.addFlashAttribute("successMessage", "保存が完了しました。");
            return "redirect:/declaration/payment-ledger/" + form.getShiteiNo();

        } catch (RuntimeException e) {
            // 💡 3. サービス層からの例外をキャッチして、画面にエラーメッセージを渡す
            // HTML側の th:if="${errorMessage}" にこのメッセージが表示される仕組みだぜ
            model.addAttribute("errorMessage", e.getMessage());

            // 入力内容を保持したまま編集・登録画面に戻す
            // 施設名などの、フォームに含まれない表示専用データを再取得する
            fukaService.hydrateFormMetadata(form);
            
            // ログにもエラーを残しておくと、後で調査しやすいぜ
            log.error("保存処理でエラーが発生しました: {}", e.getMessage());

            return "fuka/tFukaConfig";
        }
    }
}
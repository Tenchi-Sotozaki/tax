package jp.lg.asp.accommodation.controller;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.IntStream;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.FukaDaichoForm;
import jp.lg.asp.accommodation.dto.FukaDeclarationForm;
import jp.lg.asp.accommodation.dto.FukaMonthlyDeclarationDto;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.service.FukaService;
import jp.lg.asp.accommodation.service.FukaValidatorService;
import jp.lg.asp.accommodation.service.NokigenService;
import jp.lg.asp.accommodation.util.SessionHelper;
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
	private final ScreenAccessChecker accessChecker;
	private final FukaValidatorService fukaValidatorService;
	private final NokigenService nokigenService;

	private static final String SCREEN_ID = ScreenManagement.FUKA_DAICHO;
	private static final String DAICHO_VIEW = "fuka/tFukaDaicho";

	private static final String CONFIG_VIEW = "fuka/tFukaConfig";

	/** 入力欄の下に出す必須エラーの文言。行の中に置くので短くする */
	private static final String MSG_RATE_REQUIRED = "割合を入力してください";
	private static final String MSG_AMOUNT_REQUIRED = "金額を入力してください";

	/**
	 * サマリに出す必須エラーの文言。
	 * 加算金額は3行あり、短い文言のままだと同じ文字列が並んでどの行か分からないため、
	 * サマリ側だけ項目名を付ける。
	 */
	private static final Map<String, String> SUMMARY_REQUIRED_MESSAGES = Map.of(
			"additionalRate1", "加算割合1を入力してください",
			"additionalRate2", "加算割合2を入力してください",
			"additionalRate3", "加算割合3を入力してください",
			"additionalAmount1", "加算金額1を入力してください",
			"additionalAmount2", "加算金額2を入力してください",
			"additionalAmount3", "加算金額3を入力してください");

	/**
	 * 納入金額管理台帳を表示し、検索処理を行う。
	 * @param shiteiNo 指定番号
	 * @param nendo 対象年度
	 * @param status 抽出ステータス
	 * @param model モデルオブジェクト
	 * @return 画面パス
	 */
	@GetMapping("/payment-ledger")
	@OpeLog(screenId = SCREEN_ID, operation = "初期表示")
	public String showDaicho(
			@RequestParam(required = false) String nendo,
			@RequestParam(required = false) String status,
			HttpSession session,
			Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		ShiteiGassanSearchDto selected = SessionHelper.getShiteiGassan(session);
		if (selected == null || selected.getShiteiNo() == null || selected.getShiteiNo().isEmpty()) {
			model.addAttribute("showShiteiGassanModal", true);
			return DAICHO_VIEW;
		}
		String shiteiNo = selected.getShiteiNo();
		
		// 今年度と前年度を計算
	    LocalDate now = LocalDate.now();
	    int thisNendo = now.getMonthValue() >= 4 ? now.getYear() : now.getYear() - 1;
	    int prevNendo = thisNendo - 1;

	    // パラメータ指定がない場合のデフォルト年度設定
	    if (nendo == null || nendo.isEmpty()) {
	        nendo = String.valueOf(thisNendo);
	    }
	    int currentNendo = Integer.parseInt(nendo);

	    // DBからデータが存在する年度リストを取得
	    List<Integer> existingNendoList = fukaService.getExistingNendoList(shiteiNo);

	    // 最古年度と最新年度を特定
	    Set<Integer> baseSet = new TreeSet<>(existingNendoList);
	    baseSet.add(prevNendo);
	    baseSet.add(thisNendo);

	    int minNendo = Collections.min(baseSet);
	    int maxNendo = Collections.max(baseSet);

	    // 最小年度から最大年度まで連番のリストを作成
	    List<Integer> nendoList = IntStream.rangeClosed(minNendo, maxNendo)
	                                       .boxed()
	                                       .toList();

		// サービスを呼び出して表示用データを生成
		FukaDaichoForm form = fukaService.getDaichoData(shiteiNo, nendo, status);
		
		// 納入期限が未登録の場合
		if(nokigenService.findAll().isEmpty()) {
			model.addAttribute("errorMessage", "納入期限が登録されていません。");
		}

		model.addAttribute("fukaDaichoForm", form);
		model.addAttribute("searchForm", form);
		model.addAttribute("items", form.getItems());
		model.addAttribute("totalAmount", form.getTotalAmount());
		model.addAttribute("obligorId", shiteiNo);
	    model.addAttribute("selectedNendo", currentNendo);
	    model.addAttribute("nendoList", nendoList);
	    model.addAttribute("currentStatus", status);

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
	@GetMapping("/register")
	@OpeLog(screenId = SCREEN_ID, operation = "登録画面表示")
	public String register(
			@RequestParam(required = false) String month,
			HttpSession session,
			RedirectAttributes redirectAttributes,
			Model model) {
		accessChecker.checkWriteAccess(SCREEN_ID);
		ShiteiGassanSearchDto selected = SessionHelper.getShiteiGassan(session);
		if (selected == null || selected.getShiteiNo() == null || selected.getShiteiNo().isEmpty()) {
			model.addAttribute("showShiteiGassanModal", true);
			return DAICHO_VIEW;
		}
		String shiteiNo = selected.getShiteiNo();

		// 二重申告を防止するためのアクセスガード
		if (StringUtils.hasText(month)) {
			if (fukaService.isAlreadyRegistered(shiteiNo, month)) {
				redirectAttributes.addFlashAttribute("errorMessage", "申告済みのデータです。「照会」ボタンから確認してください。");
				return "redirect:/declaration/payment-ledger";
			}
		}

		try {
			FukaDeclarationForm form = fukaService.getDeclarationFormForRegister(shiteiNo, month);
			model.addAttribute("fukaDeclarationForm", form);
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
			return "redirect:/declaration/payment-ledger";
		}
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
	@GetMapping("/edit")
	@OpeLog(screenId = SCREEN_ID, operation = "編集画面表示")
	public String showEdit(
			@RequestParam String nendo,
			@RequestParam Integer kibetsu,
			HttpSession session,
			RedirectAttributes redirectAttributes,
			Model model) {
		accessChecker.checkWriteAccess(SCREEN_ID);
		ShiteiGassanSearchDto selected = SessionHelper.getShiteiGassan(session);
		if (selected == null || selected.getShiteiNo() == null || selected.getShiteiNo().isEmpty()) {
			model.addAttribute("showShiteiGassanModal", true);
			return DAICHO_VIEW;
		}
		String shiteiNo = selected.getShiteiNo();

		// 未申告データに対する編集アクセス制限
		if (!fukaService.isAlreadyRegisteredByKibetsu(shiteiNo, nendo, kibetsu)) {
			redirectAttributes.addFlashAttribute("errorMessage", "未申告のデータです。「新規登録」ボタンから登録してください。");
			return "redirect:/declaration/payment-ledger";
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
	@GetMapping("/view")
	@OpeLog(screenId = SCREEN_ID, operation = "照会")
	public String showView(
			@RequestParam String nendo,
			@RequestParam Integer kibetsu,
			@RequestParam(required = false) Integer rno,
			HttpSession session,
			RedirectAttributes redirectAttributes,
			Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		ShiteiGassanSearchDto selected = SessionHelper.getShiteiGassan(session);
		if (selected == null || selected.getShiteiNo() == null || selected.getShiteiNo().isEmpty()) {
			model.addAttribute("showShiteiGassanModal", true);
			return DAICHO_VIEW;
		}
		String shiteiNo = selected.getShiteiNo();

		if (!fukaService.isAlreadyRegisteredByKibetsu(shiteiNo, nendo, kibetsu)) {
			redirectAttributes.addFlashAttribute("errorMessage", "未申告のデータです。「新規登録」ボタンから登録してください。");
			return "redirect:/declaration/payment-ledger";
		}

		FukaDeclarationForm form = (rno != null)
				? fukaService.getDeclarationFormForViewByRno(shiteiNo, nendo, kibetsu, rno)
				: fukaService.getDeclarationFormForView(shiteiNo, nendo, kibetsu);
		form.setView(true);
		form.setEdit(false);
		model.addAttribute("fukaDeclarationForm", form);
		return CONFIG_VIEW;
	}

	/**
	 * 宿泊税情報の保存（登録・更新）を行う。
	 * @param form 申告情報フォーム
	 * @param bindingResult バリデーション結果
	 * @param model モデルオブジェクト
	 * @param redirectAttributes リダイレクト属性
	 * @return 画面パス
	 */
	@PostMapping("/save")
	@OpeLog(screenId = SCREEN_ID, operation = "保存（登録・更新）")
	public String save(@Validated @ModelAttribute("fukaDeclarationForm") FukaDeclarationForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {

		accessChecker.checkWriteAccess(SCREEN_ID);

		// 申告を受けてから登録するため、申告年月日は登録年月日以前とする。
		// 未入力のときは @NotNull 側に任せ、ここでは比較しない。
		if (form.getShinkokuDate() != null && form.getTorokuDate() != null
				&& form.getShinkokuDate().isAfter(form.getTorokuDate())) {
			bindingResult.rejectValue("shinkokuDate", "error.shinkokuDate",
					"申告年月日は登録年月日以前の日付を入力してください");
		}

		// 加算金額区分を選択した場合は、割合の入力も必須
		if (StringUtils.hasText(form.getAdditionalCategory1())
				&& !StringUtils.hasText(form.getAdditionalRate1())) {
			bindingResult.rejectValue("additionalRate1", "error.additionalRate1",
					MSG_RATE_REQUIRED);
		}
		if (StringUtils.hasText(form.getAdditionalCategory2())
				&& !StringUtils.hasText(form.getAdditionalRate2())) {
			bindingResult.rejectValue("additionalRate2", "error.additionalRate2",
					MSG_RATE_REQUIRED);
		}
		if (StringUtils.hasText(form.getAdditionalCategory3())
				&& !StringUtils.hasText(form.getAdditionalRate3())) {
			bindingResult.rejectValue("additionalRate3", "error.additionalRate3",
					MSG_RATE_REQUIRED);
		}

		// 加算金額区分を選択した場合は、加算金額の入力も必須（0は未入力として扱う）
		if (StringUtils.hasText(form.getAdditionalCategory1())
				&& (form.getAdditionalAmount1() == null || form.getAdditionalAmount1() <= 0)) {
			bindingResult.rejectValue("additionalAmount1", "error.additionalAmount1",
					MSG_AMOUNT_REQUIRED);
		}
		if (StringUtils.hasText(form.getAdditionalCategory2())
				&& (form.getAdditionalAmount2() == null || form.getAdditionalAmount2() <= 0)) {
			bindingResult.rejectValue("additionalAmount2", "error.additionalAmount2",
					MSG_AMOUNT_REQUIRED);
		}
		if (StringUtils.hasText(form.getAdditionalCategory3())
				&& (form.getAdditionalAmount3() == null || form.getAdditionalAmount3() <= 0)) {
			bindingResult.rejectValue("additionalAmount3", "error.additionalAmount3",
					MSG_AMOUNT_REQUIRED);
		}

		// 1. 基本的な入力チェック（Spring Bootによる自動バリデーション）
		if (bindingResult.hasErrors()) {
			bindingResult.getAllErrors().forEach(error -> {
				log.error("【バリデーションエラー】項目: {}, 内容: {}", error.getObjectName(), error.getDefaultMessage());
			});
			// 表示順を入力欄順に制御
			java.util.List<String> validationErrors = new java.util.ArrayList<>();
			java.util.List<String> fieldOrder = java.util.List.of(
					"torokuDate", "shinkokuDate",
					"modificationCategory",
					"additionalRate1", "additionalAmount1",
					"additionalRate2", "additionalAmount2",
					"additionalRate3", "additionalAmount3");
			for (String field : fieldOrder) {
				bindingResult.getAllErrors().stream()
						.filter(e -> e instanceof org.springframework.validation.FieldError
								? ((org.springframework.validation.FieldError) e).getField().equals(field)
								: e.getCode() != null && e.getCode().contains(field))
						.map(org.springframework.context.support.DefaultMessageSourceResolvable::getDefaultMessage)
						.map(msg -> toSummaryMessage(field, msg))
						.forEach(validationErrors::add);
			}
			model.addAttribute("validationErrors", validationErrors);
			return CONFIG_VIEW;
		}

		// 2. 編集時の必須項目チェック
		if (form.isEdit() && !StringUtils.hasText(form.getModificationCategory())) {
			bindingResult.rejectValue("modificationCategory", "error.modificationCategory", "※編集時は変更の区分を選択してください");
			return CONFIG_VIEW;
		}

		// 3. 金額と宿泊数のソフトバリデーション（Soft Validation）
		if (!form.isTaxCheckBypassed()) {
			List<String> discrepancyMessages = fukaValidatorService.getDiscrepancyMessages(form);
			if (!discrepancyMessages.isEmpty()) {
				log.debug("金額または宿泊数のズレを検知しました。確認モーダルを表示します。");
				model.addAttribute("showTaxWarningModal", true);
				model.addAttribute("discrepancyMessages", discrepancyMessages);
				return CONFIG_VIEW;
			}
		}

		// 4. 保存処理（Transaction：DBへの書き込み）
		try {
			// バリデーションをすべて通過、またはユーザーが警告を承認（バイパス）したため保存を実行
			fukaService.saveDeclaration(form);
			redirectAttributes.addFlashAttribute("successMessage", "賦課情報を更新しました。");
			return "redirect:/declaration/payment-ledger";

		} catch (RuntimeException e) {
			log.error("保存処理中に予期せぬエラーが発生しました", e);
			model.addAttribute("errorMessage", "保存に失敗しました：" + e.getMessage());
			return CONFIG_VIEW;
		}
	}

	/**
	 * 入力中の申告内容から、市区町村税額・都道府県税額の内訳を試算する（内訳試算ボタン）。
	 * 保存は行わず、計算結果のみを返す。
	 * @param fukaKbn 賦課区分コード（"1"=定額, "2"=定率）
	 * @param monthlyDetail 画面入力中の申告情報
	 * @return 内訳を設定した申告情報
	 */
	@PostMapping("/estimate-breakdown")
	@ResponseBody
	@OpeLog(screenId = SCREEN_ID, operation = "内訳試算")
	public ResponseEntity<?> estimateBreakdown(
			@RequestParam("fukaKbn") String fukaKbn,
			@RequestBody FukaMonthlyDeclarationDto monthlyDetail) {

		try {
			accessChecker.checkWriteAccess(SCREEN_ID);
			return ResponseEntity.ok(fukaService.estimateBreakdown(fukaKbn, monthlyDetail));
		} catch (RuntimeException e) {
			// FukaControllerは@Controllerのため、GlobalExceptionHandler（@RestController限定）の対象外。
			// ここで捕捉しないとHTMLエラーページが返り、画面側のfetchでJSONとして解釈できず
			// 「内訳試算に失敗しました」という不明瞭なエラーになってしまう。
			log.error("内訳試算に失敗しました", e);
			return ResponseEntity.internalServerError()
					.body(Collections.singletonMap("message", "内訳試算に失敗しました：" + e.getMessage()));
		}
	}

	/**
	 * サマリ用にメッセージを差し替える。
	 * 入力欄の下は「割合を入力してください」のように短く出し、
	 * サマリは3行分が並ぶのでどの行かが分かるよう項目名を付ける。
	 * 差し替えるのは必須エラーだけで、形式エラー（@Pattern）はそのまま通す。
	 */
	private String toSummaryMessage(String field, String message) {
		if (MSG_RATE_REQUIRED.equals(message) || MSG_AMOUNT_REQUIRED.equals(message)) {
			return SUMMARY_REQUIRED_MESSAGES.getOrDefault(field, message);
		}
		return message;
	}
}
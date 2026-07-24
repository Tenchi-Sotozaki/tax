package jp.lg.asp.accommodation.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.constant.FukaConstants;
import jp.lg.asp.accommodation.constant.ZeiritsuConstants;
import jp.lg.asp.accommodation.dto.ZeiritsuDetailForm;
import jp.lg.asp.accommodation.dto.ZeiritsuForm;
import jp.lg.asp.accommodation.dto.ZeiritsuListItem;
import jp.lg.asp.accommodation.dto.ZeiritsuSearchForm;
import jp.lg.asp.accommodation.entity.Zeiritsu;
import jp.lg.asp.accommodation.entity.ZeiritsuId;
import jp.lg.asp.accommodation.entity.ZeiritsuTeigaku;
import jp.lg.asp.accommodation.entity.ZeiritsuTeigakuId;
import jp.lg.asp.accommodation.entity.ZeiritsuTeiritsu;
import jp.lg.asp.accommodation.entity.ZeiritsuTeiritsuId;
import jp.lg.asp.accommodation.exception.ResourceNotFoundException;
import jp.lg.asp.accommodation.repository.ZeiritsuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeigakuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeiritsuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/zeiritsu")
public class ZeiritsuController {

	private final ZeiritsuRepository zeiritsuRepository;
	private final ZeiritsuTeigakuRepository zeiritsuTeigakuRepository;
	private final ZeiritsuTeiritsuRepository zeiritsuTeiritsuRepository;
	private final ScreenAccessChecker accessChecker;

	private final JichitaiContext jichitaiContext;

	private static final String SCREEN_ID = ScreenManagement.ZEIRITSU_DAICHO;
	private static final String SCREEN_ID_CONFIG = ScreenManagement.ZEIRITSU_CONFIG;
	private static final String LIST_VIEW = "admin/zeiritsuDaicho";
	private static final String FORM_VIEW = "admin/zeiritsuConfig";

	// ========== 一覧 ==========

	@GetMapping("/list")
	@OpeLog(screenId = SCREEN_ID, operation = "一覧表示")
	public String list(@ModelAttribute ZeiritsuSearchForm searchForm,
			Model model) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkAccess(SCREEN_ID);
		List<ZeiritsuListItem> items = search(jichitaiCd, searchForm);
		model.addAttribute("items", items);
		model.addAttribute("searchForm", searchForm);
		addConstants(model);
		return LIST_VIEW;
	}

	// ========== 照会 ==========

	@GetMapping("/view/{seq}")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "照会")
	public String view(@PathVariable("seq") Long seq,
			Model model) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkAccess(SCREEN_ID_CONFIG);
		Zeiritsu z = findOrThrow(jichitaiCd, BigDecimal.valueOf(seq));
		model.addAttribute("zeiritsuForm", toForm(z, jichitaiCd));
		model.addAttribute("isView", true);
		model.addAttribute("isEdit", false);
		model.addAttribute("seq", seq);
		addConstants(model);
		return FORM_VIEW;
	}

	// ========== 編集画面 ==========

	@GetMapping("/edit/{seq}")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "編集画面表示")
	public String edit(@PathVariable("seq") Long seq,
			Model model) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);
		Zeiritsu z = findOrThrow(jichitaiCd, BigDecimal.valueOf(seq));
		model.addAttribute("zeiritsuForm", toForm(z, jichitaiCd));
		model.addAttribute("isView", false);
		model.addAttribute("isEdit", true);
		model.addAttribute("seq", seq);
		model.addAttribute("isLatest", isLatestRecord(jichitaiCd, z));
		model.addAttribute("isDetailEditable", isFutureStartYm(z.getTekiyoStYm()));
		addConstants(model);
		return FORM_VIEW;
	}

	// ========== 更新処理 ==========

	@PostMapping("/edit/{seq}")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "編集")
	public String update(@PathVariable("seq") Long seq,
			@Validated @ModelAttribute("zeiritsuForm") ZeiritsuForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);

		BigDecimal seqDec = BigDecimal.valueOf(seq);
		Zeiritsu entity = findOrThrow(jichitaiCd, seqDec);
		boolean latest = isLatestRecord(jichitaiCd, entity);
		boolean detailEditable = isFutureStartYm(entity.getTekiyoStYm());

		// 最新でない場合は開始年月・賦課方式・区分をDBの値で上書き
		if (!latest) {
			form.setFukaKbn(entity.getFukaKbn());
			form.setTekiyoStYm(entity.getTekiyoStYm());
			form.setTaishoKbn(entity.getTaishoKbn());
		}

		validateDetails(form, bindingResult);

		// 期間重複チェック（編集対象は除外）
		validatePeriodOverlap(form, bindingResult, jichitaiCd, seqDec);
		if (!bindingResult.hasErrors() && latest) {
			validatePeriodGap(form, bindingResult, jichitaiCd, seqDec);
		}

		if (bindingResult.hasErrors()) {
			model.addAttribute("isView", false);
			model.addAttribute("isEdit", true);
			model.addAttribute("seq", seq);
			model.addAttribute("isLatest", latest);
			model.addAttribute("isDetailEditable", detailEditable);
			addConstants(model);
			model.addAttribute("validationErrors", ZeiritsuForm.validate(form).values());
			return FORM_VIEW;
		}

		String tekiyoStYm = form.getTekiyoStYm().replace("-", "");

		// 既存データの自動更新処理（編集対象は除外）
		if (latest) {
			autoUpdateExistingPeriod(form, jichitaiCd, seqDec);
		}

		entity.setFukaKbn(form.getFukaKbn());
		entity.setTekiyoStYm(tekiyoStYm);
		String tekiyoEdYm = form.getTekiyoEdYm();
		if (tekiyoEdYm != null && !tekiyoEdYm.isBlank()) {
			entity.setTekiyoEdYm(tekiyoEdYm.replace("-", ""));
		} else {
			entity.setTekiyoEdYm(null);
		}
		entity.setTaishoKbn(form.getTaishoKbn());
		zeiritsuRepository.save(entity);

		boolean isTeigaku = FukaConstants.TEIGAKU.getValue().equals(form.getFukaKbn());
		if (!detailEditable) {
			// 明細編集不可の場合はDBの値をそのまま保持（何もしない）
			log.debug("明細編集不可（適用開始年月が未来でない）のため、明細の更新をスキップします。seq: {}", seqDec);
		} else {
			if (isTeigaku) {
				zeiritsuTeigakuRepository.findActiveBySeq(jichitaiCd, seqDec)
						.forEach(d -> {
							d.setDelFlg("1");
							zeiritsuTeigakuRepository.save(d);
						});
			} else {
				zeiritsuTeiritsuRepository.findActiveBySeq(jichitaiCd, seqDec)
						.forEach(d -> {
							d.setDelFlg("1");
							zeiritsuTeiritsuRepository.save(d);
						});
			}

			int detailSeq = 1;
			for (ZeiritsuDetailForm detail : form.getDetails()) {
				if (detail.getZeiValue() == null || detail.getZeiValue().isBlank())
					continue;
				if (isTeigaku) {
					ZeiritsuTeigaku d = zeiritsuTeigakuRepository
							.findById(new ZeiritsuTeigakuId(jichitaiCd, seqDec, BigDecimal.valueOf(detailSeq)))
							.orElse(new ZeiritsuTeigaku());
					d.setJichitaiCd(jichitaiCd);
					d.setSeq(seqDec);
					d.setTeigakuSeq(BigDecimal.valueOf(detailSeq));
					d.setZeigaku(parseLong(detail.getZeiValue()));
					d.setRyokinSt(parseLong(detail.getRyokinSt()));
					d.setRyokinEd(parseLong(detail.getRyokinEd()));
					d.setDelFlg("0");
					zeiritsuTeigakuRepository.save(d);
				} else {
					ZeiritsuTeiritsu d = zeiritsuTeiritsuRepository
							.findById(new ZeiritsuTeiritsuId(jichitaiCd, seqDec, BigDecimal.valueOf(detailSeq)))
							.orElse(new ZeiritsuTeiritsu());
					d.setJichitaiCd(jichitaiCd);
					d.setSeq(seqDec);
					d.setTeiritsuSeq(BigDecimal.valueOf(detailSeq));
					// パーセント表記をそのまま使用（200 → 200.0）
					BigDecimal zeiRitsu = new BigDecimal(detail.getZeiValue());
					d.setZeiRitsu(zeiRitsu);
					d.setKbnName(detail.getKbnName());
					d.setDelFlg("0");
					zeiritsuTeiritsuRepository.save(d);
				}
				detailSeq++;
			}
		} // end detailEditable

		log.debug("税率管理マスタを更新しました。jichitaiCd: {}, seq: {}", jichitaiCd, seq);
		redirectAttributes.addFlashAttribute("successMessage", "税率管理マスタを更新しました。");
		return "redirect:/admin/zeiritsu/view/" + seq;
	}

	// ========== 削除処理 ==========

	@PostMapping("/delete/{seq}")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "削除")
	public String delete(@PathVariable("seq") Long seq,
			RedirectAttributes redirectAttributes) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);

		BigDecimal seqDec = BigDecimal.valueOf(seq);

		// メインレコードの論理削除
		Zeiritsu entity = findOrThrow(jichitaiCd, seqDec);
		entity.setDelFlg("1");
		zeiritsuRepository.save(entity);

		// 関連する詳細レコードも論理削除
		boolean isTeigaku = FukaConstants.TEIGAKU.getValue().equals(entity.getFukaKbn());
		if (isTeigaku) {
			zeiritsuTeigakuRepository.findActiveBySeq(jichitaiCd, seqDec)
					.forEach(d -> {
						d.setDelFlg("1");
						zeiritsuTeigakuRepository.save(d);
					});
		} else {
			zeiritsuTeiritsuRepository.findActiveBySeq(jichitaiCd, seqDec)
					.forEach(d -> {
						d.setDelFlg("1");
						zeiritsuTeiritsuRepository.save(d);
					});
		}

		log.debug("税率管理マスタを削除しました。jichitaiCd: {}, seq: {}", jichitaiCd, seq);
		redirectAttributes.addFlashAttribute("successMessage", "税率管理マスタを削除しました。");
		return "redirect:/admin/zeiritsu/list";
	}

	// ========== 新規登録画面 ==========

	@GetMapping("/register")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "登録画面表示")
	public String showForm(Model model) {
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);
		model.addAttribute("zeiritsuForm", new ZeiritsuForm());
		model.addAttribute("isView", false);
		model.addAttribute("isEdit", false);
		addConstants(model);
		return FORM_VIEW;
	}

	// ========== 登録処理 ==========

	@PostMapping("/register")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "登録")
	public String save(@Validated @ModelAttribute("zeiritsuForm") ZeiritsuForm form,
			BindingResult bindingResult,
			@org.springframework.web.bind.annotation.RequestParam(value = "confirmAutoUpdate", required = false) String confirmAutoUpdate,
			Model model,
			RedirectAttributes redirectAttributes) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);

		validateDetails(form, bindingResult);

		boolean confirmed = "true".equals(confirmAutoUpdate);
		Zeiritsu autoUpdateTarget = null;
		if (!confirmed) {
			autoUpdateTarget = findAutoUpdateTarget(form, jichitaiCd, null);
			if (autoUpdateTarget != null) {
				// 自動更新確認が必要 → モーダル表示のためエラーなしで画面返却
				if (!bindingResult.hasErrors()) {
					model.addAttribute("isView", false);
					model.addAttribute("isEdit", false);
					model.addAttribute("autoUpdateTarget", autoUpdateTarget);
					model.addAttribute("autoUpdateNewEdYm",
							formatYm(getPreviousMonth(form.getTekiyoStYm().replace("-", ""))));
					addConstants(model);
					return FORM_VIEW;
				}
			} else {
				validatePeriodOverlap(form, bindingResult, jichitaiCd, null);
				if (!bindingResult.hasErrors()) {
					validatePeriodGap(form, bindingResult, jichitaiCd, null);
				}
			}
		}
		if (bindingResult.hasErrors()) {
			model.addAttribute("isView", false);
			model.addAttribute("isEdit", false);
			addConstants(model);
			model.addAttribute("validationErrors", ZeiritsuForm.validate(form).values());
			return FORM_VIEW;
		}

		String tekiyoStYm = form.getTekiyoStYm().replace("-", "");

		// 既存データの自動更新処理
		autoUpdateExistingPeriod(form, jichitaiCd, null);

		// 物理テーブルから全レコードを取得して最大seqを取得
		BigDecimal nextSeq = zeiritsuRepository.findAll().stream()
				.filter(z -> z.getJichitaiCd().equals(jichitaiCd))
				.map(Zeiritsu::getSeq)
				.max(BigDecimal::compareTo)
				.map(s -> s.add(BigDecimal.ONE))
				.orElse(BigDecimal.ONE);

		Zeiritsu entity = new Zeiritsu();
		entity.setJichitaiCd(jichitaiCd);
		entity.setSeq(nextSeq);
		entity.setFukaKbn(form.getFukaKbn());
		entity.setTekiyoStYm(tekiyoStYm);
		String tekiyoEdYm = form.getTekiyoEdYm();
		if (tekiyoEdYm != null && !tekiyoEdYm.isBlank()) {
			entity.setTekiyoEdYm(tekiyoEdYm.replace("-", ""));
		} else {
			entity.setTekiyoEdYm(null);
		}
		entity.setTaishoKbn(form.getTaishoKbn());
		entity.setDelFlg("0");
		zeiritsuRepository.save(entity);

		boolean isTeigaku = FukaConstants.TEIGAKU.getValue().equals(form.getFukaKbn());
		int detailSeq = 1;
		for (ZeiritsuDetailForm detail : form.getDetails()) {
			if (detail.getZeiValue() == null || detail.getZeiValue().isBlank())
				continue;
			if (isTeigaku) {
				ZeiritsuTeigaku d = new ZeiritsuTeigaku();
				d.setJichitaiCd(jichitaiCd);
				d.setSeq(nextSeq);
				d.setTeigakuSeq(BigDecimal.valueOf(detailSeq));
				d.setZeigaku(parseLong(detail.getZeiValue()));
				d.setRyokinSt(parseLong(detail.getRyokinSt()));
				d.setRyokinEd(parseLong(detail.getRyokinEd()));
				d.setDelFlg("0");
				zeiritsuTeigakuRepository.save(d);
			} else {
				ZeiritsuTeiritsu d = new ZeiritsuTeiritsu();
				d.setJichitaiCd(jichitaiCd);
				d.setSeq(nextSeq);
				d.setTeiritsuSeq(BigDecimal.valueOf(detailSeq));
				BigDecimal zeiRitsu = new BigDecimal(detail.getZeiValue());
				d.setZeiRitsu(zeiRitsu);
				d.setKbnName(detail.getKbnName());
				d.setDelFlg("0");
				zeiritsuTeiritsuRepository.save(d);
			}
			detailSeq++;
		}

		log.debug("税率管理マスタを登録しました。jichitaiCd: {}, seq: {}", jichitaiCd, nextSeq);
		redirectAttributes.addFlashAttribute("successMessage", "税率管理マスタを登録しました。");
		return "redirect:/admin/zeiritsu/list";
	}

	// ========== private ==========

	private Zeiritsu findOrThrow(String jichitaiCd, BigDecimal seq) {
		return zeiritsuRepository.findById(new ZeiritsuId(jichitaiCd, seq))
				.orElseThrow(() -> new ResourceNotFoundException("税率管理データが見つかりません"));
	}

	private boolean isLatestRecord(String jichitaiCd, Zeiritsu z) {
		BigDecimal maxSeq = zeiritsuRepository.findMaxSeqByJichitaiCdAndTaishoKbn(jichitaiCd, z.getTaishoKbn());
		return maxSeq != null && maxSeq.compareTo(z.getSeq()) == 0;
	}

	private boolean isFutureStartYm(String tekiyoStYm) {
		if (tekiyoStYm == null || tekiyoStYm.length() != 6)
			return false;
		java.time.YearMonth current = java.time.YearMonth.now();
		java.time.YearMonth startYm = java.time.YearMonth.of(
				Integer.parseInt(tekiyoStYm.substring(0, 4)),
				Integer.parseInt(tekiyoStYm.substring(4, 6)));
		return startYm.isAfter(current);
	}

	private void validateDetails(ZeiritsuForm form, BindingResult bindingResult) {
		boolean isTeigaku = FukaConstants.TEIGAKU.getValue().equals(form.getFukaKbn());
		for (int i = 0; i < form.getDetails().size(); i++) {
			ZeiritsuDetailForm d = form.getDetails().get(i);
			boolean hasZei = d.getZeiValue() != null && !d.getZeiValue().isBlank();
			if (i == 0 && !hasZei) {
				bindingResult.rejectValue("details[0].zeiValue", "NotBlank", "税額(税率)①は必須です");
			}
			if (hasZei && isTeigaku) {
				boolean stBlank = d.getRyokinSt() == null || d.getRyokinSt().isBlank();
				boolean edBlank = d.getRyokinEd() == null || d.getRyokinEd().isBlank();
				if (stBlank && edBlank) {
					bindingResult.rejectValue("details[" + i + "].ryokinSt", "RequiredEither",
							"「円以上」または「円未満」のどちらかは必須です ");
				}
			}
			if (hasZei && !isTeigaku) {
				// 定率の場合の税率範囲チェック
				try {
					BigDecimal zeiValue = new BigDecimal(d.getZeiValue());
					if (zeiValue.compareTo(new BigDecimal("0.00")) < 0
							|| zeiValue.compareTo(new BigDecimal("999.99")) > 0) {
						bindingResult.rejectValue("details[" + i + "].zeiValue", "Range",
								"税率は0.00%～999.99%の範囲で入力してください");
					}
				} catch (NumberFormatException e) {
					bindingResult.rejectValue("details[" + i + "].zeiValue", "Invalid",
							"税率は数値で入力してください");
				}
				boolean kbnBlank = d.getKbnName() == null || d.getKbnName().isBlank();
				if (kbnBlank) {
					bindingResult.rejectValue("details[" + i + "].kbnName", "NotBlank",
							"区分名は必須です");
				}
			}
		}

		// 定額の場合の金額範囲重複チェック
		if (isTeigaku) {
			validateTeigakuRangeOverlap(form, bindingResult);
		}
	}

	/**
	 * 期間の歯抜けチェック。
	 * 無期限レコードなしかつ新規開始年月の前月が既存の最大終了年月と一致しない場合にエラー。
	 */
	private void validatePeriodGap(ZeiritsuForm form, BindingResult bindingResult, String jichitaiCd,
			BigDecimal excludeSeq) {
		String taishoKbn = form.getTaishoKbn();
		String tekiyoStYm = form.getTekiyoStYm().replace("-", "");

		List<Zeiritsu> existingList = zeiritsuRepository.findActiveByJichitaiCd(jichitaiCd).stream()
				.filter(z -> z.getTaishoKbn().equals(taishoKbn))
				.filter(z -> !"1".equals(z.getDelFlg()))
				.filter(z -> excludeSeq == null || !z.getSeq().equals(excludeSeq))
				.collect(Collectors.toList());

		if (existingList.isEmpty())
			return;

		// 無期限レコードがある場合は歯抜けにならない（findAutoUpdateTargetで別途処理）
		boolean hasOpenEnd = existingList.stream()
				.anyMatch(z -> z.getTekiyoEdYm() == null || z.getTekiyoEdYm().isBlank());
		if (hasOpenEnd)
			return;

		// 既存の最大終了年月
		String maxEdYm = existingList.stream()
				.map(Zeiritsu::getTekiyoEdYm)
				.max(String::compareTo)
				.orElse(null);

		if (maxEdYm == null)
			return;

		// 新規開始年月の前月が最大終了年月と一致するかチェック
		String expectedEdYm = getPreviousMonth(tekiyoStYm);
		if (!expectedEdYm.equals(maxEdYm)) {
			bindingResult.rejectValue("tekiyoStYm", "PeriodGap",
					"期間に歯抜けが生じます。適用開始年月は " + formatYm(getNextMonth(maxEdYm)) + "で登録してください。");
		}
	}

	/**
	 * 終了年月未登録の既存レコードと重複する場合、そのレコードを返す（自動更新確認用）。
	 * 該当なしの場合は null を返す。
	 */
	private Zeiritsu findAutoUpdateTarget(ZeiritsuForm form, String jichitaiCd, BigDecimal excludeSeq) {
		String taishoKbn = form.getTaishoKbn();
		String tekiyoStYm = form.getTekiyoStYm().replace("-", "");
		return zeiritsuRepository.findActiveByJichitaiCd(jichitaiCd).stream()
				.filter(z -> z.getTaishoKbn().equals(taishoKbn))
				.filter(z -> !"1".equals(z.getDelFlg()))
				.filter(z -> excludeSeq == null || !z.getSeq().equals(excludeSeq))
				.filter(z -> z.getTekiyoEdYm() == null || z.getTekiyoEdYm().isBlank())
				.filter(z -> tekiyoStYm.compareTo(z.getTekiyoStYm()) >= 0)
				.findFirst().orElse(null);
	}

	private void validatePeriodOverlap(ZeiritsuForm form, BindingResult bindingResult, String jichitaiCd,
			BigDecimal excludeSeq) {
		String taishoKbn = form.getTaishoKbn();
		String tekiyoStYm = form.getTekiyoStYm().replace("-", "");
		String tekiyoEdYm = form.getTekiyoEdYm();
		if (tekiyoEdYm != null && !tekiyoEdYm.isBlank()) {
			tekiyoEdYm = tekiyoEdYm.replace("-", "");
		}

		// 初期登録済み期間より古い開始年月のブロック
		List<Zeiritsu> sameKbnList = zeiritsuRepository.findActiveByJichitaiCdAndTaishoKbn(jichitaiCd, taishoKbn);
		if (!sameKbnList.isEmpty()) {
			Zeiritsu oldest = sameKbnList.stream()
					.filter(z -> excludeSeq == null || !z.getSeq().equals(excludeSeq))
					.min((a, b) -> a.getSeq().compareTo(b.getSeq()))
					.orElse(null);
			if (oldest != null && tekiyoStYm.compareTo(oldest.getTekiyoStYm()) < 0) {
				String taishoName = ZeiritsuConstants.CITY.getValue().equals(taishoKbn)
						? ZeiritsuConstants.CITY.getName()
						: ZeiritsuConstants.KEN.getName();
				bindingResult.rejectValue("tekiyoStYm", "TooOldStartYm",
						"[" + taishoName + "] の初期登録済み期間（" + formatYm(oldest.getTekiyoStYm())
								+ "～）より古い開始年月は登録できません。");
				return;
			}
		}

		// 同じ対象区分の既存データを取得（終了年月あり・削除済み除外）
		List<Zeiritsu> existingList = zeiritsuRepository.findActiveByJichitaiCd(jichitaiCd).stream()
				.filter(z -> z.getTaishoKbn().equals(taishoKbn))
				.filter(z -> !"1".equals(z.getDelFlg()))
				.filter(z -> excludeSeq == null || !z.getSeq().equals(excludeSeq))
				.filter(z -> z.getTekiyoEdYm() != null && !z.getTekiyoEdYm().isBlank())
				.collect(Collectors.toList());

		for (Zeiritsu existing : existingList) {
			String existingStYm = existing.getTekiyoStYm();
			String existingEdYm = existing.getTekiyoEdYm();
			boolean isOverlap;
			if (tekiyoEdYm == null || tekiyoEdYm.isBlank()) {
				isOverlap = tekiyoStYm.compareTo(existingEdYm) <= 0;
			} else {
				isOverlap = !(tekiyoEdYm.compareTo(existingStYm) < 0 || tekiyoStYm.compareTo(existingEdYm) > 0);
			}
			if (isOverlap) {
				String fukaKbnName = FukaConstants.TEIGAKU.getValue().equals(existing.getFukaKbn())
						? FukaConstants.TEIGAKU.getName()
						: FukaConstants.TEIRITSU.getName();
				bindingResult.rejectValue("tekiyoStYm", "PeriodOverlap",
						"既存の賦課方式設定と期間が重複しています。（既存：" + fukaKbnName + " "
								+ formatYm(existingStYm) + "～" + formatYm(existingEdYm) + "）");
				return;
			}
		}
	}

	private void autoUpdateExistingPeriod(ZeiritsuForm form, String jichitaiCd, BigDecimal excludeSeq) {
		String taishoKbn = form.getTaishoKbn();
		String tekiyoStYm = form.getTekiyoStYm().replace("-", "");

		// 同じ対象区分で無期限の既存データを取得（賦課方式は区別しない、削除フラグが1のものは除外）
		List<Zeiritsu> existingList = zeiritsuRepository.findActiveByJichitaiCd(jichitaiCd)
				.stream()
				.filter(z -> z.getTaishoKbn().equals(taishoKbn))
				.filter(z -> !"1".equals(z.getDelFlg()))
				.filter(z -> z.getTekiyoEdYm() == null || z.getTekiyoEdYm().isBlank())
				.filter(z -> excludeSeq == null || !z.getSeq().equals(excludeSeq))
				.filter(z -> z.getTekiyoStYm().compareTo(tekiyoStYm) < 0)
				.collect(Collectors.toList());

		for (Zeiritsu existing : existingList) {
			// 新規の適用開始時期の前月を適用終了時期に設定
			String newEdYm = getPreviousMonth(tekiyoStYm);
			existing.setTekiyoEdYm(newEdYm);
			zeiritsuRepository.save(existing);
			String fukaKbnName = FukaConstants.TEIGAKU.getValue().equals(existing.getFukaKbn())
					? FukaConstants.TEIGAKU.getName()
					: FukaConstants.TEIRITSU.getName();
			log.debug("既存の賦課方式設定の適用終了時期を自動更新しました。seq: {}, 賦課方式: {}, 新終了時期: {}",
					existing.getSeq(), fukaKbnName, newEdYm);
		}
	}

	private String getPreviousMonth(String ym) {
		int year = Integer.parseInt(ym.substring(0, 4));
		int month = Integer.parseInt(ym.substring(4, 6));
		if (month == 1) {
			year--;
			month = 12;
		} else {
			month--;
		}
		return String.format("%04d%02d", year, month);
	}

	private String getNextMonth(String ym) {
		int year = Integer.parseInt(ym.substring(0, 4));
		int month = Integer.parseInt(ym.substring(4, 6));
		if (month == 12) {
			year++;
			month = 1;
		} else {
			month++;
		}
		return String.format("%04d%02d", year, month);
	}

	private String formatYm(String ym) {
		if (ym == null || ym.length() != 6)
			return ym;
		return ym.substring(0, 4) + "年" + ym.substring(4, 6) + "月";
	}

	private List<ZeiritsuListItem> search(String jichitaiCd, ZeiritsuSearchForm form) {
		return zeiritsuRepository.findActiveByJichitaiCd(jichitaiCd).stream()
				.filter(z -> {
					if (form.getFukaKbn() != null && !form.getFukaKbn().isBlank()
							&& !form.getFukaKbn().equals(z.getFukaKbn()))
						return false;
					if (form.getTaishoKbn() != null && !form.getTaishoKbn().isBlank()
							&& !form.getTaishoKbn().equals(z.getTaishoKbn()))
						return false;
					if (form.getTekiyoYmFrom() != null && !form.getTekiyoYmFrom().isBlank()) {
						String from = form.getTekiyoYmFrom().replace("-", "");
						if (z.getTekiyoStYm().compareTo(from) < 0)
							return false;
					}
					if (form.getTekiyoYmTo() != null && !form.getTekiyoYmTo().isBlank()) {
						String to = form.getTekiyoYmTo().replace("-", "");
						if (z.getTekiyoStYm().compareTo(to) > 0)
							return false;
					}
					return true;
				})
				.map(z -> new ZeiritsuListItem(
						z.getSeq(),
						z.getFukaKbn(),
						FukaConstants.TEIGAKU.getValue().equals(z.getFukaKbn())
								? FukaConstants.TEIGAKU.getName()
								: FukaConstants.TEIRITSU.getName(),
						z.getTekiyoStYm(),
						z.getTekiyoEdYm(),
						z.getTaishoKbn(),
						ZeiritsuConstants.CITY.getValue().equals(z.getTaishoKbn())
								? ZeiritsuConstants.CITY.getName()
								: ZeiritsuConstants.KEN.getName()))
				.collect(Collectors.toList());
	}

	private ZeiritsuForm toForm(Zeiritsu z, String jichitaiCd) {
		ZeiritsuForm form = new ZeiritsuForm();
		form.setFukaKbn(z.getFukaKbn());
		form.setTekiyoStYm(z.getTekiyoStYm());
		form.setTekiyoEdYm(z.getTekiyoEdYm());
		form.setTaishoKbn(z.getTaishoKbn());

		boolean isTeigaku = FukaConstants.TEIGAKU.getValue().equals(z.getFukaKbn());
		if (isTeigaku) {
			List<ZeiritsuTeigaku> details = zeiritsuTeigakuRepository.findActiveBySeq(jichitaiCd, z.getSeq());
			for (int i = 0; i < details.size() && i < 5; i++) {
				ZeiritsuTeigaku d = details.get(i);
				ZeiritsuDetailForm df = form.getDetails().get(i);
				df.setZeiValue(d.getZeigaku() != null ? d.getZeigaku().toString() : null);
				df.setRyokinSt(d.getRyokinSt() != null ? d.getRyokinSt().toString() : null);
				df.setRyokinEd(d.getRyokinEd() != null ? d.getRyokinEd().toString() : null);
			}
		} else {
			List<ZeiritsuTeiritsu> details = zeiritsuTeiritsuRepository.findActiveBySeq(jichitaiCd, z.getSeq());
			for (int i = 0; i < details.size() && i < 5; i++) {
				ZeiritsuTeiritsu d = details.get(i);
				ZeiritsuDetailForm df = form.getDetails().get(i);
				// データベース値をそのまま表示（200.0 → 200.0）
				df.setZeiValue(d.getZeiRitsu() != null ? d.getZeiRitsu().toPlainString() : null);
				df.setKbnName(d.getKbnName());
			}
		}
		return form;
	}

	private Long parseLong(String value) {
		if (value == null || value.isBlank())
			return null;
		return Long.parseLong(value.trim());
	}

	private void addConstants(Model model) {
		model.addAttribute("fukaTeigaku", FukaConstants.TEIGAKU);
		model.addAttribute("fukaTeiritsu", FukaConstants.TEIRITSU);
		model.addAttribute("taishoCity", ZeiritsuConstants.CITY);
		model.addAttribute("taishoKen", ZeiritsuConstants.KEN);
	}

	private void validateTeigakuRangeOverlap(ZeiritsuForm form, BindingResult bindingResult) {
		// 有効な税額設定のみを収集
		List<ZeiritsuDetailForm> validDetails = form.getDetails().stream()
				.filter(d -> d.getZeiValue() != null && !d.getZeiValue().isBlank())
				.collect(Collectors.toList());

		for (int i = 0; i < validDetails.size(); i++) {
			ZeiritsuDetailForm detail1 = validDetails.get(i);
			Long st1 = parseLong(detail1.getRyokinSt());
			Long ed1 = parseLong(detail1.getRyokinEd());

			for (int j = i + 1; j < validDetails.size(); j++) {
				ZeiritsuDetailForm detail2 = validDetails.get(j);
				Long st2 = parseLong(detail2.getRyokinSt());
				Long ed2 = parseLong(detail2.getRyokinEd());

				if (isRangeOverlap(st1, ed1, st2, ed2)) {
					int index1 = form.getDetails().indexOf(detail1);
					int index2 = form.getDetails().indexOf(detail2);
					bindingResult.rejectValue("details[" + index1 + "].ryokinSt", "RangeOverlap",
							"金額範囲が重複しています。①番目と" + (index2 + 1) + "番目の範囲を確認してください。");
					return;
				}
			}
		}
	}

	private boolean isRangeOverlap(Long st1, Long ed1, Long st2, Long ed2) {
		// 無限大の値を表現するためのLong.MAX_VALUE
		long start1 = (st1 != null) ? st1 : 0L;
		long end1 = (ed1 != null) ? ed1 - 1 : Long.MAX_VALUE; // 未満なので-1
		long start2 = (st2 != null) ? st2 : 0L;
		long end2 = (ed2 != null) ? ed2 - 1 : Long.MAX_VALUE; // 未満なので-1

		// 範囲重複チェック: 両方が重ならない場合はfelse
		return !(end1 < start2 || end2 < start1);
	}
}

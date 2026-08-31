package jp.lg.asp.accommodation.controller;

import java.math.BigDecimal;
import java.util.List;

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
import jp.lg.asp.accommodation.service.ZeiritsuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/zeiritsu")
public class ZeiritsuController {

	private final ZeiritsuService zeiritsuService;
	private final ScreenAccessChecker accessChecker;
	private final JichitaiContext jichitaiContext;

	private static final String SCREEN_ID = ScreenManagement.ZEIRITSU_DAICHO;
	private static final String SCREEN_ID_CONFIG = ScreenManagement.ZEIRITSU_CONFIG;
	private static final String LIST_VIEW = "admin/zeiritsuDaicho";
	private static final String FORM_VIEW = "admin/zeiritsuConfig";

	@GetMapping("/list")
	@OpeLog(screenId = SCREEN_ID, operation = "一覧表示")
	public String list(@ModelAttribute ZeiritsuSearchForm searchForm,
			@org.springframework.web.bind.annotation.RequestParam(required = false) String tekiyoYmFrom,
			Model model) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkAccess(SCREEN_ID);
		boolean isSearched = tekiyoYmFrom != null;
		List<ZeiritsuListItem> items = isSearched ? zeiritsuService.search(jichitaiCd, searchForm) : List.of();
		model.addAttribute("items", items);
		model.addAttribute("isSearched", isSearched);
		model.addAttribute("searchForm", searchForm);
		addConstants(model);
		return LIST_VIEW;
	}

	@GetMapping("/view/{seq}")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "照会")
	public String view(@PathVariable("seq") Long seq, Model model) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkAccess(SCREEN_ID_CONFIG);
		Zeiritsu z = zeiritsuService.findOrThrow(jichitaiCd, BigDecimal.valueOf(seq));
		model.addAttribute("zeiritsuForm", zeiritsuService.toForm(z, jichitaiCd));
		model.addAttribute("isView", true);
		model.addAttribute("isEdit", false);
		model.addAttribute("seq", seq);
		model.addAttribute("isDetailEditable", false);
		model.addAttribute("isHeaderEditable", false);
		model.addAttribute("isEdYmOnlyEditable", false);
		model.addAttribute("autoUpdateTarget", null);
		addConstants(model);
		return FORM_VIEW;
	}

	@GetMapping("/edit/{seq}")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "編集画面表示")
	public String edit(@PathVariable("seq") Long seq, Model model) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);
		Zeiritsu z = zeiritsuService.findOrThrow(jichitaiCd, BigDecimal.valueOf(seq));
		model.addAttribute("zeiritsuForm", zeiritsuService.toForm(z, jichitaiCd));
		model.addAttribute("isView", false);
		model.addAttribute("isEdit", true);
		model.addAttribute("seq", seq);
		model.addAttribute("isLatest", zeiritsuService.isLatestRecord(jichitaiCd, z));
		boolean futureStart = zeiritsuService.isFutureStartYm(z.getTekiyoStYm());
		boolean edYmOnly = !futureStart;
		model.addAttribute("isDetailEditable", futureStart);
		model.addAttribute("isHeaderEditable", futureStart);
		model.addAttribute("isEdYmOnlyEditable", edYmOnly);
		model.addAttribute("autoUpdateTarget", null);
		addConstants(model);
		return FORM_VIEW;
	}

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
		Zeiritsu entity = zeiritsuService.findOrThrow(jichitaiCd, seqDec);
		boolean latest = zeiritsuService.isLatestRecord(jichitaiCd, entity);
		boolean detailEditable = zeiritsuService.isFutureStartYm(entity.getTekiyoStYm());
		boolean headerEditable = detailEditable;
		boolean edYmOnly = !detailEditable;

		if (edYmOnly) {
			// 適用期間内：適用終了年月のみ更新・バリデーション
			validateEdYmOverlap(entity, form, bindingResult, jichitaiCd, seqDec);
			List<String> edYmErrors = bindingResult.getFieldErrors("tekiyoEdYm").stream()
					.map(org.springframework.validation.FieldError::getDefaultMessage).toList();
			if (!edYmErrors.isEmpty()) {
				ZeiritsuForm cleanForm = zeiritsuService.toForm(entity, jichitaiCd);
				cleanForm.setTekiyoEdYm(form.getTekiyoEdYm());
				model.addAttribute("zeiritsuForm", cleanForm);
				model.addAttribute("isView", false);
				model.addAttribute("isEdit", true);
				model.addAttribute("seq", seq);
				model.addAttribute("isLatest", latest);
				model.addAttribute("isDetailEditable", false);
				model.addAttribute("isHeaderEditable", false);
				model.addAttribute("isEdYmOnlyEditable", true);
				model.addAttribute("autoUpdateTarget", null);
				model.addAttribute("validationErrors", edYmErrors);
				model.addAttribute("tekiyoEdYmErrors", edYmErrors);
				addConstants(model);
				return FORM_VIEW;
			}
			zeiritsuService.update(jichitaiCd, seqDec, form, false, true);
			redirectAttributes.addFlashAttribute("successMessage", "税率管理マスタを更新しました。");
			return "redirect:/admin/zeiritsu/view/" + seq;
		}

		if (!headerEditable) {
			form.setFukaKbn(entity.getFukaKbn());
			form.setTekiyoStYm(entity.getTekiyoStYm());
			form.setTaishoKbn(entity.getTaishoKbn());
		}

		validateDetails(form, bindingResult);
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
			model.addAttribute("isHeaderEditable", headerEditable);
			model.addAttribute("isEdYmOnlyEditable", edYmOnly);
			model.addAttribute("autoUpdateTarget", null);
			addConstants(model);
			model.addAttribute("validationErrors", ZeiritsuForm.validate(form).values());
			return FORM_VIEW;
		}

		zeiritsuService.update(jichitaiCd, seqDec, form, detailEditable, false);
		redirectAttributes.addFlashAttribute("successMessage", "税率管理マスタを更新しました。");
		return "redirect:/admin/zeiritsu/view/" + seq;
	}

	@PostMapping("/delete/{seq}")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "削除")
	public String delete(@PathVariable("seq") Long seq, RedirectAttributes redirectAttributes) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);
		zeiritsuService.delete(jichitaiCd, BigDecimal.valueOf(seq));
		redirectAttributes.addFlashAttribute("successMessage", "税率管理マスタを削除しました。");
		return "redirect:/admin/zeiritsu/list";
	}

	@GetMapping("/register")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "登録画面表示")
	public String showForm(Model model) {
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);
		model.addAttribute("zeiritsuForm", new ZeiritsuForm());
		model.addAttribute("isView", false);
		model.addAttribute("isEdit", false);
		model.addAttribute("isDetailEditable", true);
		model.addAttribute("isHeaderEditable", true);
		model.addAttribute("isEdYmOnlyEditable", false);
		addConstants(model);
		return FORM_VIEW;
	}

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
			autoUpdateTarget = zeiritsuService.findAutoUpdateTarget(form, jichitaiCd, null);
			if (autoUpdateTarget != null) {
				if (!bindingResult.hasErrors()) {
					model.addAttribute("isView", false);
					model.addAttribute("isEdit", false);
					model.addAttribute("isDetailEditable", true);
					model.addAttribute("isHeaderEditable", true);
					model.addAttribute("isEdYmOnlyEditable", false);
					model.addAttribute("autoUpdateTarget", autoUpdateTarget);
					model.addAttribute("autoUpdateNewEdYm",
							zeiritsuService.formatYm(zeiritsuService.getPreviousMonth(form.getTekiyoStYm().replace("-", ""))));
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
			model.addAttribute("isDetailEditable", true);
			model.addAttribute("isHeaderEditable", true);
			model.addAttribute("isEdYmOnlyEditable", false);
			model.addAttribute("autoUpdateTarget", null);
			addConstants(model);
			model.addAttribute("validationErrors", ZeiritsuForm.validate(form).values());
			return FORM_VIEW;
		}

		zeiritsuService.save(jichitaiCd, form);
		redirectAttributes.addFlashAttribute("successMessage", "税率管理マスタを登録しました。");
		return "redirect:/admin/zeiritsu/list";
	}

	private void addConstants(Model model) {
		model.addAttribute("fukaTeigaku", FukaConstants.TEIGAKU);
		model.addAttribute("fukaTeiritsu", FukaConstants.TEIRITSU);
		model.addAttribute("taishoCity", ZeiritsuConstants.CITY);
		model.addAttribute("taishoKen", ZeiritsuConstants.KEN);
	}

	private void validateDetails(ZeiritsuForm form, BindingResult bindingResult) {
		boolean isTeigaku = FukaConstants.TEIGAKU.getValue().equals(form.getFukaKbn());
		for (int i = 0; i < form.getDetails().size(); i++) {
			var d = form.getDetails().get(i);
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
				try {
					BigDecimal zeiValue = new BigDecimal(d.getZeiValue());
					if (zeiValue.compareTo(new BigDecimal("0.00")) < 0
							|| zeiValue.compareTo(new BigDecimal("999.99")) > 0) {
						bindingResult.rejectValue("details[" + i + "].zeiValue", "Range",
								"税率は0.00%～999.99%の範囲で入力してください");
					}
				} catch (NumberFormatException e) {
					bindingResult.rejectValue("details[" + i + "].zeiValue", "Invalid", "税率は数値で入力してください");
				}
				if (d.getKbnName() == null || d.getKbnName().isBlank()) {
					bindingResult.rejectValue("details[" + i + "].kbnName", "NotBlank", "区分名は必須です");
				}
			}
		}
		if (isTeigaku) {
			validateTeigakuRangeOverlap(form, bindingResult);
		}
	}

	private void validateEdYmOverlap(Zeiritsu entity, ZeiritsuForm form, BindingResult bindingResult,
			String jichitaiCd, BigDecimal excludeSeq) {
		String tekiyoEdYm = form.getTekiyoEdYm();
		if (tekiyoEdYm == null || tekiyoEdYm.isBlank()) return;
		String edYm = tekiyoEdYm.replace("-", "");
		String taishoKbn = entity.getTaishoKbn();
		boolean overlap = zeiritsuService.findActiveByJichitaiCd(jichitaiCd).stream()
				.filter(z -> z.getTaishoKbn().equals(taishoKbn))
				.filter(z -> !"1".equals(z.getDelFlg()))
				.filter(z -> !z.getSeq().equals(excludeSeq))
				.anyMatch(z -> edYm.compareTo(z.getTekiyoStYm()) >= 0
						&& (z.getTekiyoEdYm() == null || z.getTekiyoEdYm().isBlank()
								|| edYm.compareTo(z.getTekiyoEdYm()) <= 0));
		if (overlap) {
			bindingResult.rejectValue("tekiyoEdYm", "PeriodOverlap",
					"適用終了年月が別レコードの適用期間と重複しています。");
		}
	}

	private void validatePeriodGap(ZeiritsuForm form, BindingResult bindingResult, String jichitaiCd,
			BigDecimal excludeSeq) {
		String taishoKbn = form.getTaishoKbn();
		String tekiyoStYm = form.getTekiyoStYm().replace("-", "");

		List<Zeiritsu> existingList = zeiritsuService.findActiveByJichitaiCd(jichitaiCd).stream()
				.filter(z -> z.getTaishoKbn().equals(taishoKbn))
				.filter(z -> !"1".equals(z.getDelFlg()))
				.filter(z -> excludeSeq == null || !z.getSeq().equals(excludeSeq))
				.toList();

		if (existingList.isEmpty()) return;

		boolean hasOpenEnd = existingList.stream()
				.anyMatch(z -> z.getTekiyoEdYm() == null || z.getTekiyoEdYm().isBlank());
		if (hasOpenEnd) return;

		String maxEdYm = existingList.stream()
				.map(Zeiritsu::getTekiyoEdYm)
				.max(String::compareTo)
				.orElse(null);
		if (maxEdYm == null) return;

		String expectedEdYm = zeiritsuService.getPreviousMonth(tekiyoStYm);
		if (!expectedEdYm.equals(maxEdYm)) {
			String nextMonth = zeiritsuService.formatYm(getNextMonth(maxEdYm));
			bindingResult.rejectValue("tekiyoStYm", "PeriodGap",
					"期間に歯抜けが生じます。適用開始年月は " + nextMonth + "で登録してください。");
		}
	}

	private void validatePeriodOverlap(ZeiritsuForm form, BindingResult bindingResult, String jichitaiCd,
			BigDecimal excludeSeq) {
		String taishoKbn = form.getTaishoKbn();
		String tekiyoStYm = form.getTekiyoStYm().replace("-", "");
		String tekiyoEdYm = form.getTekiyoEdYm();
		if (tekiyoEdYm != null && !tekiyoEdYm.isBlank()) {
			tekiyoEdYm = tekiyoEdYm.replace("-", "");
		}

		List<Zeiritsu> sameKbnList = zeiritsuService.findActiveByJichitaiCdAndTaishoKbn(jichitaiCd, taishoKbn);

		if (!sameKbnList.isEmpty()) {
			Zeiritsu oldest = sameKbnList.stream()
					.filter(z -> excludeSeq == null || !z.getSeq().equals(excludeSeq))
					.min((a, b) -> a.getSeq().compareTo(b.getSeq()))
					.orElse(null);
			if (oldest != null && tekiyoStYm.compareTo(oldest.getTekiyoStYm()) < 0) {
				String taishoName = ZeiritsuConstants.CITY.getValue().equals(taishoKbn)
						? ZeiritsuConstants.CITY.getName() : ZeiritsuConstants.KEN.getName();
				bindingResult.rejectValue("tekiyoStYm", "TooOldStartYm",
						"[" + taishoName + "] の初期登録済み期間（" + zeiritsuService.formatYm(oldest.getTekiyoStYm())
								+ "～）より古い開始年月は登録できません。");
				return;
			}
		}

		final String finalTekiyoEdYm = tekiyoEdYm;
		List<Zeiritsu> existingList = zeiritsuService.findActiveByJichitaiCd(jichitaiCd).stream()
				.filter(z -> z.getTaishoKbn().equals(taishoKbn))
				.filter(z -> !"1".equals(z.getDelFlg()))
				.filter(z -> excludeSeq == null || !z.getSeq().equals(excludeSeq))
				.filter(z -> z.getTekiyoEdYm() != null && !z.getTekiyoEdYm().isBlank())
				.toList();

		for (Zeiritsu existing : existingList) {
			String existingStYm = existing.getTekiyoStYm();
			String existingEdYm = existing.getTekiyoEdYm();
			boolean isOverlap;
			if (finalTekiyoEdYm == null || finalTekiyoEdYm.isBlank()) {
				isOverlap = tekiyoStYm.compareTo(existingEdYm) <= 0;
			} else {
				isOverlap = !(finalTekiyoEdYm.compareTo(existingStYm) < 0 || tekiyoStYm.compareTo(existingEdYm) > 0);
			}
			if (isOverlap) {
				String fukaKbnName = FukaConstants.TEIGAKU.getValue().equals(existing.getFukaKbn())
						? FukaConstants.TEIGAKU.getName() : FukaConstants.TEIRITSU.getName();
				bindingResult.rejectValue("tekiyoStYm", "PeriodOverlap",
						"既存の賦課方式設定と期間が重複しています。（既存：" + fukaKbnName + " "
								+ zeiritsuService.formatYm(existingStYm) + "～" + zeiritsuService.formatYm(existingEdYm) + "）");
				return;
			}
		}
	}

	private void validateTeigakuRangeOverlap(ZeiritsuForm form, BindingResult bindingResult) {
		List<jp.lg.asp.accommodation.dto.ZeiritsuDetailForm> validDetails = form.getDetails().stream()
				.filter(d -> d.getZeiValue() != null && !d.getZeiValue().isBlank())
				.toList();

		for (int i = 0; i < validDetails.size(); i++) {
			jp.lg.asp.accommodation.dto.ZeiritsuDetailForm detail1 = validDetails.get(i);
			Long st1 = parseLong(detail1.getRyokinSt());
			Long ed1 = parseLong(detail1.getRyokinEd());
			for (int j = i + 1; j < validDetails.size(); j++) {
				jp.lg.asp.accommodation.dto.ZeiritsuDetailForm detail2 = validDetails.get(j);
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
		long start1 = (st1 != null) ? st1 : 0L;
		long end1 = (ed1 != null) ? ed1 - 1 : Long.MAX_VALUE;
		long start2 = (st2 != null) ? st2 : 0L;
		long end2 = (ed2 != null) ? ed2 - 1 : Long.MAX_VALUE;
		return !(end1 < start2 || end2 < start1);
	}

	private Long parseLong(String value) {
		if (value == null || value.isBlank()) return null;
		return Long.parseLong(value.trim());
	}

	private String getNextMonth(String ym) {
		int year = Integer.parseInt(ym.substring(0, 4));
		int month = Integer.parseInt(ym.substring(4, 6));
		if (month == 12) { year++; month = 1; } else { month++; }
		return String.format("%04d%02d", year, month);
	}
}

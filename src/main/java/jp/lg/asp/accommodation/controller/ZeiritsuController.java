package jp.lg.asp.accommodation.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
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

	private static final String SCREEN_ID = ScreenManagement.ZEIRITSU_CONFIG;
	private static final String LIST_VIEW = "admin/zeiritsuDaicho";
	private static final String FORM_VIEW = "admin/zeiritsuConfig";

	// ========== 一覧 ==========

	@GetMapping("/list")
	public String list(@ModelAttribute ZeiritsuSearchForm searchForm,
			Authentication authentication, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		List<ZeiritsuListItem> items = search(authentication.getName(), searchForm);
		model.addAttribute("items", items);
		model.addAttribute("searchForm", searchForm);
		addConstants(model);
		return LIST_VIEW;
	}

	// ========== 照会 ==========

	@GetMapping("/view/{seq}")
	public String view(@PathVariable("seq") Long seq,
			Authentication authentication, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		Zeiritsu z = findOrThrow(authentication.getName(), BigDecimal.valueOf(seq));
		model.addAttribute("zeiritsuForm", toForm(z, authentication.getName()));
		model.addAttribute("isView", true);
		model.addAttribute("isEdit", false);
		model.addAttribute("seq", seq);
		addConstants(model);
		return FORM_VIEW;
	}

	// ========== 編集画面 ==========

	@GetMapping("/edit/{seq}")
	public String edit(@PathVariable("seq") Long seq,
			Authentication authentication, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		Zeiritsu z = findOrThrow(authentication.getName(), BigDecimal.valueOf(seq));
		model.addAttribute("zeiritsuForm", toForm(z, authentication.getName()));
		model.addAttribute("isView", false);
		model.addAttribute("isEdit", true);
		model.addAttribute("seq", seq);
		addConstants(model);
		return FORM_VIEW;
	}

	// ========== 更新処理 ==========

	@PostMapping("/edit/{seq}")
	public String update(@PathVariable("seq") Long seq,
			@Validated @ModelAttribute("zeiritsuForm") ZeiritsuForm form,
			BindingResult bindingResult,
			Authentication authentication,
			Model model,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID);

		validateDetails(form, bindingResult);

		if (bindingResult.hasErrors()) {
			model.addAttribute("isView", false);
			model.addAttribute("isEdit", true);
			model.addAttribute("seq", seq);
			addConstants(model);
			return FORM_VIEW;
		}

		String jichitaiCd = authentication.getName();
		String tekiyoStYm = form.getTekiyoStYm().replace("-", "");
		BigDecimal seqDec = BigDecimal.valueOf(seq);

		Zeiritsu entity = findOrThrow(jichitaiCd, seqDec);
		entity.setFukaKbn(form.getFukaKbn());
		entity.setTekiyoStYm(tekiyoStYm);
		entity.setTaishoKbn(form.getTaishoKbn());
		zeiritsuRepository.save(entity);

		boolean isTeigaku = FukaConstants.TEIGAKU.getValue().equals(form.getFukaKbn());
		if (isTeigaku) {
			zeiritsuTeigakuRepository.findActiveBySeq(jichitaiCd, seqDec)
					.forEach(d -> { d.setDelFlg("1"); zeiritsuTeigakuRepository.save(d); });
		} else {
			zeiritsuTeiritsuRepository.findActiveByTaishoKbnAndTekiyoYm(entity.getTaishoKbn(),
					entity.getTekiyoStYm(), entity.getTekiyoEdYm())
					.forEach(d -> { d.setDelFlg("1"); zeiritsuTeiritsuRepository.save(d); });
		}

		int detailSeq = 1;
		for (ZeiritsuDetailForm detail : form.getDetails()) {
			if (detail.getZeiValue() == null || detail.getZeiValue().isBlank()) continue;
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
				d.setZeiRitsu(new BigDecimal(detail.getZeiValue()));
				d.setKbnName(detail.getKbnName());
				d.setDelFlg("0");
				zeiritsuTeiritsuRepository.save(d);
			}
			detailSeq++;
		}

		log.info("税率管理マスタを更新しました。jichitaiCd: {}, seq: {}", jichitaiCd, seq);
		redirectAttributes.addFlashAttribute("successMessage", "税率管理マスタを更新しました。");
		return "redirect:/admin/zeiritsu/view/" + seq;
	}

	// ========== 新規登録画面 ==========

	@GetMapping("/register")
	public String showForm(Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		model.addAttribute("zeiritsuForm", new ZeiritsuForm());
		model.addAttribute("isView", false);
		model.addAttribute("isEdit", false);
		addConstants(model);
		return FORM_VIEW;
	}

	// ========== 登録処理 ==========

	@PostMapping("/register")
	public String save(@Validated @ModelAttribute("zeiritsuForm") ZeiritsuForm form,
			BindingResult bindingResult,
			Authentication authentication,
			Model model,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID);

		validateDetails(form, bindingResult);

		if (bindingResult.hasErrors()) {
			model.addAttribute("isView", false);
			model.addAttribute("isEdit", false);
			addConstants(model);
			return FORM_VIEW;
		}

		String jichitaiCd = authentication.getName();
		String tekiyoStYm = form.getTekiyoStYm().replace("-", "");

		List<Zeiritsu> existing = zeiritsuRepository.findActiveByJichitaiCd(jichitaiCd);
		BigDecimal nextSeq = existing.stream()
				.map(Zeiritsu::getSeq)
				.max(BigDecimal::compareTo)
				.map(s -> s.add(BigDecimal.ONE))
				.orElse(BigDecimal.ONE);

		Zeiritsu entity = new Zeiritsu();
		entity.setJichitaiCd(jichitaiCd);
		entity.setSeq(nextSeq);
		entity.setFukaKbn(form.getFukaKbn());
		entity.setTekiyoStYm(tekiyoStYm);
		entity.setTekiyoEdYm("999912");
		entity.setTaishoKbn(form.getTaishoKbn());
		entity.setDelFlg("0");
		zeiritsuRepository.save(entity);

		boolean isTeigaku = FukaConstants.TEIGAKU.getValue().equals(form.getFukaKbn());
		int detailSeq = 1;
		for (ZeiritsuDetailForm detail : form.getDetails()) {
			if (detail.getZeiValue() == null || detail.getZeiValue().isBlank()) continue;
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
				d.setZeiRitsu(new BigDecimal(detail.getZeiValue()));
				d.setKbnName(detail.getKbnName());
				d.setDelFlg("0");
				zeiritsuTeiritsuRepository.save(d);
			}
			detailSeq++;
		}

		log.info("税率管理マスタを登録しました。jichitaiCd: {}, seq: {}", jichitaiCd, nextSeq);
		redirectAttributes.addFlashAttribute("successMessage", "税率管理マスタを登録しました。");
		return "redirect:/admin/zeiritsu/list";
	}

	// ========== private ==========

	private Zeiritsu findOrThrow(String jichitaiCd, BigDecimal seq) {
		return zeiritsuRepository.findById(new ZeiritsuId(jichitaiCd, seq))
				.orElseThrow(() -> new ResourceNotFoundException("税率管理データが見つかりません"));
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
							"「円以上」または「円未満」のどちらかは必須です");
				}
			}
			if (hasZei && !isTeigaku) {
				boolean kbnBlank = d.getKbnName() == null || d.getKbnName().isBlank();
				if (kbnBlank) {
					bindingResult.rejectValue("details[" + i + "].kbnName", "NotBlank",
							"区分名は必須です");
				}
			}
		}
	}

	private List<ZeiritsuListItem> search(String jichitaiCd, ZeiritsuSearchForm form) {
		return zeiritsuRepository.findActiveByJichitaiCd(jichitaiCd).stream()
				.filter(z -> {
					if (form.getFukaKbn() != null && !form.getFukaKbn().isBlank()
							&& !form.getFukaKbn().equals(z.getFukaKbn())) return false;
					if (form.getTaishoKbn() != null && !form.getTaishoKbn().isBlank()
							&& !form.getTaishoKbn().equals(z.getTaishoKbn())) return false;
					if (form.getTekiyoYmFrom() != null && !form.getTekiyoYmFrom().isBlank()) {
						String from = form.getTekiyoYmFrom().replace("-", "");
						if (z.getTekiyoStYm().compareTo(from) < 0) return false;
					}
					if (form.getTekiyoYmTo() != null && !form.getTekiyoYmTo().isBlank()) {
						String to = form.getTekiyoYmTo().replace("-", "");
						if (z.getTekiyoStYm().compareTo(to) > 0) return false;
					}
					return true;
				})
				.map(z -> new ZeiritsuListItem(
						z.getSeq(),
						z.getFukaKbn(),
						FukaConstants.TEIGAKU.getValue().equals(z.getFukaKbn())
								? FukaConstants.TEIGAKU.getName() : FukaConstants.TEIRITSU.getName(),
						z.getTekiyoStYm(),
						z.getTekiyoEdYm(),
						z.getTaishoKbn(),
						ZeiritsuConstants.CITY.getValue().equals(z.getTaishoKbn())
								? ZeiritsuConstants.CITY.getName() : ZeiritsuConstants.KEN.getName()))
				.collect(Collectors.toList());
	}

	private ZeiritsuForm toForm(Zeiritsu z, String jichitaiCd) {
		ZeiritsuForm form = new ZeiritsuForm();
		form.setFukaKbn(z.getFukaKbn());
		form.setTekiyoStYm(z.getTekiyoStYm());
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
			List<ZeiritsuTeiritsu> details = zeiritsuTeiritsuRepository.findActiveByTaishoKbnAndTekiyoYm(z.getTaishoKbn(),
					z.getTekiyoStYm(), z.getTekiyoEdYm());
			for (int i = 0; i < details.size() && i < 5; i++) {
				ZeiritsuTeiritsu d = details.get(i);
				ZeiritsuDetailForm df = form.getDetails().get(i);
				df.setZeiValue(d.getZeiRitsu() != null ? d.getZeiRitsu().toPlainString() : null);
				df.setKbnName(d.getKbnName());
			}
		}
		return form;
	}

	private Long parseLong(String value) {
		if (value == null || value.isBlank()) return null;
		return Long.parseLong(value.trim());
	}

	private void addConstants(Model model) {
		model.addAttribute("fukaTeigaku", FukaConstants.TEIGAKU);
		model.addAttribute("fukaTeiritsu", FukaConstants.TEIRITSU);
		model.addAttribute("taishoCity", ZeiritsuConstants.CITY);
		model.addAttribute("taishoKen", ZeiritsuConstants.KEN);
	}
}

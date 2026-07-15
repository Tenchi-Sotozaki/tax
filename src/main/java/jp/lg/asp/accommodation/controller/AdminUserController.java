package jp.lg.asp.accommodation.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
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

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.UserForm;
import jp.lg.asp.accommodation.dto.UserSearchForm;
import jp.lg.asp.accommodation.entity.User;
import jp.lg.asp.accommodation.entity.UserId;
import jp.lg.asp.accommodation.repository.RoleRepository;
import jp.lg.asp.accommodation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
@Slf4j 
public class AdminUserController {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final ScreenAccessChecker accessChecker;

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	private static final String SCREEN_ID = ScreenManagement.USER_MANAGEMENT;
	private static final String LIST_VIEW = "admin/userDaicho";
	private static final String FORM_VIEW = "admin/userConfig";

	@GetMapping("/user-search")
	@OpeLog(screenId = SCREEN_ID, operation = "照会")
	public String list(@ModelAttribute UserSearchForm searchForm, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		model.addAttribute("items", userRepository.search(
				jichitaiCd,
				emptyToNull(searchForm.getId()),
				toLikePattern(searchForm.getName(), searchForm.getNameMatchType()),
				toLikePattern(searchForm.getNameKana(), searchForm.getNameKanaMatchType()),
				toLikePattern(searchForm.getBusho(), searchForm.getBushoMatchType())));
		model.addAttribute("roleMap", roleRepository.findByJichitaiCdOrderByRoleId(jichitaiCd)
				.stream().collect(java.util.stream.Collectors.toMap(
						r -> String.valueOf(r.getRoleId()), r -> r.getName())));
		return LIST_VIEW;
	}

	@GetMapping("/user-registration")
	@OpeLog(screenId = SCREEN_ID, operation = "登録画面表示")
	public String showRegistrationForm(Model model) {
	    accessChecker.checkWriteAccess(SCREEN_ID);
	    model.addAttribute("userForm", new UserForm());
	    model.addAttribute("roles", roleRepository.findByJichitaiCdOrderByRoleId(jichitaiCd));
	    model.addAttribute("isEdit", false);
	    return FORM_VIEW;
	}

	@PostMapping("/user-registration")
	@OpeLog(screenId = SCREEN_ID, operation = "登録")
	public String register(
	        @Validated(UserForm.OnCreate.class) @ModelAttribute("userForm") UserForm form,
	        BindingResult bindingResult,
	        Model model,
	        RedirectAttributes redirectAttributes) {
	    accessChecker.checkWriteAccess(SCREEN_ID);
	    if (!form.getPassword().equals(form.getPasswordConfirm())) {
	        bindingResult.rejectValue("passwordConfirm", "error.passwordConfirm", "パスワードが一致しません");
	    }
	    if (bindingResult.hasErrors()) {
	        model.addAttribute("roles", roleRepository.findByJichitaiCdOrderByRoleId(jichitaiCd));
	        model.addAttribute("isEdit", false);
	        model.addAttribute("validationErrors", UserForm.validate(form, true).values());
	        return FORM_VIEW;
	    }
	    User user = userRepository.findById(buildUserId(form.getId()))
	            .orElse(new User());
	    boolean isNew = user.getId() == null;
	    if (isNew) {
	        user.setJichitaiCd(jichitaiCd);
	        user.setId(form.getId());
	    }
	    user.setPassword(passwordEncoder.encode(form.getPassword()));
	    user.setName(form.getName());
	    user.setNameKana(form.getNameKana());
	    user.setBusho(form.getBusho());
	    user.setRoleId(form.getRoleId());
	    userRepository.save(user);
	    redirectAttributes.addFlashAttribute("successMessage", isNew ? "ユーザーを登録しました。" : "ユーザー情報を更新しました。");
	    return "redirect:/admin/user-search";
	}

	@GetMapping("/user-edit/{id}")
	@OpeLog(screenId = SCREEN_ID, operation = "編集画面表示")
	public String showEditForm(@PathVariable String id, Model model) {
		accessChecker.checkWriteAccess(SCREEN_ID);
		User user = userRepository.findById(buildUserId(id))
				.orElseThrow(() -> new RuntimeException("ユーザーが見つかりません: " + id));

		UserForm form = new UserForm();
		form.setOriginalId(user.getId());
		form.setId(user.getId());
		form.setName(user.getName());
		form.setNameKana(user.getNameKana());
		form.setBusho(user.getBusho());
		form.setRoleId(user.getRoleId());

		model.addAttribute("userForm", form);
		model.addAttribute("roles", roleRepository.findByJichitaiCdOrderByRoleId(jichitaiCd));
		model.addAttribute("isEdit", true);
		model.addAttribute("isDefaultUser", InitialPasswordController.ADMIN_ID.equals(user.getId()));
		return FORM_VIEW;
	}

	@PostMapping("/user-edit/{id}")
	@OpeLog(screenId = SCREEN_ID, operation = "編集")
	public String update(
			@PathVariable String id,
			@Validated(UserForm.OnUpdate.class) @ModelAttribute("userForm") UserForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID);

		User user = userRepository.findById(buildUserId(id))
				.orElseThrow(() -> new RuntimeException("ユーザーが見つかりません: " + id));

		// デフォルトユーザーは権限変更不可のため、送信値に関わらず現在のロールを維持する
		boolean isDefaultUser = InitialPasswordController.ADMIN_ID.equals(user.getId());
		if (isDefaultUser) {
			form.setRoleId(user.getRoleId());
		}

		if (form.getPassword() != null && !form.getPassword().isBlank()) {
			if (!form.getPassword().equals(form.getPasswordConfirm())) {
				bindingResult.rejectValue("passwordConfirm", "error.passwordConfirm", "パスワードが一致しません");
			}
		}
		if (bindingResult.hasErrors()) {
			model.addAttribute("roles", roleRepository.findByJichitaiCdOrderByRoleId(jichitaiCd));
			model.addAttribute("isEdit", true);
			model.addAttribute("isDefaultUser", isDefaultUser);
			model.addAttribute("validationErrors", UserForm.validate(form, false).values());
			return FORM_VIEW;
		}

		user.setName(form.getName());
		user.setNameKana(form.getNameKana());
		user.setBusho(form.getBusho());
		user.setRoleId(form.getRoleId());
		if (form.getPassword() != null && !form.getPassword().isBlank()) {
			user.setPassword(passwordEncoder.encode(form.getPassword()));
		}
		userRepository.save(user);

		redirectAttributes.addFlashAttribute("successMessage", "ユーザー情報を更新しました。");
		return "redirect:/admin/user-search";
	}

	@PostMapping("/user-delete/{id}")
	@OpeLog(screenId = SCREEN_ID, operation = "削除")
	public String delete(@PathVariable String id, RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID);
		userRepository.deleteById(buildUserId(id));
		redirectAttributes.addFlashAttribute("successMessage", "ユーザーを削除しました。");
		return "redirect:/admin/user-search";
	}

	@PostMapping("/user-delete-batch")
	@OpeLog(screenId = SCREEN_ID, operation = "一括削除")
	public String deleteBatch(@RequestParam List<String> ids, RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID);
		for (String id : ids) {
			User user = userRepository.findById(buildUserId(id)).orElse(null);
			if (user != null) {
				user.setDelFlg("1");
				userRepository.save(user);
			}
		}
		redirectAttributes.addFlashAttribute("successMessage", ids.size() + "件のユーザーを削除しました。");
		return "redirect:/admin/user-search";
	}

	private UserId buildUserId(String id) {
		UserId pk = new UserId();
		pk.setJichitaiCd(jichitaiCd);
		pk.setId(id);
		return pk;
	}

	private String emptyToNull(String s) {
		return (s == null || s.isBlank()) ? null : s;
	}

	private String toLikePattern(String value, String matchType) {
		if (value == null || value.isBlank()) return null;
		return switch (matchType) {
			case "prefix" -> value + "%";
			case "exact"  -> value;
			default       -> "%" + value + "%"; // partial
		};
	}
}

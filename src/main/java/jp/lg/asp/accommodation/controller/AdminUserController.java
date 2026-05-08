package jp.lg.asp.accommodation.controller;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.dto.UserForm;
import jp.lg.asp.accommodation.dto.UserSearchForm;
import jp.lg.asp.accommodation.entity.User;
import jp.lg.asp.accommodation.entity.UserId;
import jp.lg.asp.accommodation.repository.RoleRepository;
import jp.lg.asp.accommodation.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminUserController {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	private static final String LIST_VIEW = "admin/userSearch";
	private static final String FORM_VIEW = "admin/userConfig";

	@GetMapping("/user-search")
	public String list(@ModelAttribute UserSearchForm searchForm, Model model) {
		model.addAttribute("items", userRepository.search(
				jichitaiCd,
				emptyToNull(searchForm.getId()),
				emptyToNull(searchForm.getName()),
				emptyToNull(searchForm.getNameKana()),
				emptyToNull(searchForm.getBusho())));
		return LIST_VIEW;
	}

	@GetMapping("/user-registration")
	public String showRegistrationForm(Model model) {
		model.addAttribute("userForm", new UserForm());
		model.addAttribute("roles", roleRepository.findByJichitaiCdOrderByRoleId(jichitaiCd));
		model.addAttribute("isEdit", false);
		return FORM_VIEW;
	}

	@PostMapping("/user-registration")
	public String register(
			@Validated @ModelAttribute("userForm") UserForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {

		if (!form.getPassword().equals(form.getPasswordConfirm())) {
			bindingResult.rejectValue("passwordConfirm", "error.passwordConfirm", "パスワードが一致しません");
		}
		if (bindingResult.hasErrors()) {
			model.addAttribute("roles", roleRepository.findByJichitaiCdOrderByRoleId(jichitaiCd));
			model.addAttribute("isEdit", false);
			return FORM_VIEW;
		}

		User user = new User();
		user.setJichitaiCd(jichitaiCd);
		user.setId(form.getId());
		user.setPassword(passwordEncoder.encode(form.getPassword()));
		user.setName(form.getName());
		user.setNameKana(form.getNameKana());
		user.setBusho(form.getBusho());
		user.setRoleId(form.getRoleId());
		userRepository.save(user);

		redirectAttributes.addFlashAttribute("successMessage", "ユーザーを登録しました。");
		return "redirect:/admin/user-search";
	}

	@GetMapping("/user-edit/{id}")
	public String showEditForm(@PathVariable String id, Model model) {
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
		return FORM_VIEW;
	}

	@PostMapping("/user-edit/{id}")
	public String update(
			@PathVariable String id,
			@Validated @ModelAttribute("userForm") UserForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {

		User user = userRepository.findById(buildUserId(id))
				.orElseThrow(() -> new RuntimeException("ユーザーが見つかりません: " + id));

		// 新しいパスワードが入力された場合のみ現在のパスワードを検証
		if (form.getPassword() != null && !form.getPassword().isBlank()) {
			if (form.getCurrentPassword() == null || form.getCurrentPassword().isBlank()) {
				bindingResult.rejectValue("currentPassword", "error.currentPassword", "現在のパスワードを入力してください");
			} else if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPassword())) {
				bindingResult.rejectValue("currentPassword", "error.currentPassword", "現在のパスワードが正しくありません");
			}
			if (!form.getPassword().equals(form.getPasswordConfirm())) {
				bindingResult.rejectValue("passwordConfirm", "error.passwordConfirm", "パスワードが一致しません");
			}
		}
		if (bindingResult.hasErrors()) {
			model.addAttribute("roles", roleRepository.findByJichitaiCdOrderByRoleId(jichitaiCd));
			model.addAttribute("isEdit", true);
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
	public String delete(@PathVariable String id, RedirectAttributes redirectAttributes) {
		userRepository.deleteById(buildUserId(id));
		redirectAttributes.addFlashAttribute("successMessage", "ユーザーを削除しました。");
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
}

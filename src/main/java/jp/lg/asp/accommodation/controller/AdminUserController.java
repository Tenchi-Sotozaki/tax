package jp.lg.asp.accommodation.controller;

import java.util.List;

import jp.lg.asp.accommodation.config.JichitaiContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import jp.lg.asp.accommodation.config.AppUserDetails;
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
import jp.lg.asp.accommodation.entity.Role;
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

	private final JichitaiContext jichitaiContext;

	private static final String SCREEN_ID = ScreenManagement.USER_MANAGEMENT;
	private static final String SCREEN_ID_CONFIG = ScreenManagement.USER_CONFIG;
	private static final String LIST_VIEW = "admin/userDaicho";
	private static final String FORM_VIEW = "admin/userConfig";

	@GetMapping("/user-search")
	@OpeLog(screenId = SCREEN_ID, operation = "照会")
	public String list(@ModelAttribute UserSearchForm searchForm,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int pageSize,
			Model model) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkAccess(SCREEN_ID);
		searchForm.setPage(page);
		searchForm.setPageSize(pageSize);
		Page<User> items = userRepository.searchPage(
				jichitaiCd,
				emptyToNull(searchForm.getId()),
				toLikePattern(searchForm.getName(), searchForm.getNameMatchType()),
				toLikePattern(searchForm.getNameKana(), searchForm.getNameKanaMatchType()),
				toLikePattern(searchForm.getBusho(), searchForm.getBushoMatchType()),
				searchForm.getRoleId(),
				PageRequest.of(page, pageSize));
		model.addAttribute("items", items);

		// デフォルトユーザー用の権限は検索条件のプルダウンには表示しない
		List<Role> roles = selectableRoles(jichitaiCd, null);
		model.addAttribute("roles", roles);
		// 一覧の権限名解決用（キーは権限IDの数値。BigDecimal/Longの表記揺れを避けるためLongで統一）
		model.addAttribute("roleMap", roles.stream()
				.filter(r -> r.getRoleId() != null)
				.collect(java.util.stream.Collectors.toMap(Role::getRoleId, Role::getName)));
		return LIST_VIEW;
	}

	/**
	 * 画面のプルダウンに表示する権限一覧を返す。
	 * <p>
	 * システム管理用のデフォルトユーザー権限（{@link UserRepository#DEFAULT_USER_ROLE_ID}）は
	 * 選択できないよう除外する。ただし対象ユーザーが既にその権限を持っている場合は、
	 * 照会・編集時に表示が空欄にならないよう例外的に残す。
	 *
	 * @param jichitaiCd 自治体コード
	 * @param currentRoleId 対象ユーザーの現在の権限ID（新規登録・検索条件では null）
	 * @return 表示対象の権限一覧
	 */
	private List<Role> selectableRoles(String jichitaiCd, java.math.BigDecimal currentRoleId) {
		Long current = currentRoleId != null ? currentRoleId.longValue() : null;
		return roleRepository.findByJichitaiCdOrderByRoleId(jichitaiCd).stream()
				.filter(r -> r.getRoleId() == null
						|| r.getRoleId().longValue() != UserRepository.DEFAULT_USER_ROLE_ID
						|| r.getRoleId().equals(current))
				.toList();
	}

	@GetMapping("/user-registration")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "登録画面表示")
	public String showRegistrationForm(Model model) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);
		model.addAttribute("userForm", new UserForm());
		model.addAttribute("roles", selectableRoles(jichitaiCd, null));
		model.addAttribute("isEdit", false);
		model.addAttribute("isView", false);
		model.addAttribute("isDefaultUser", false);
		return FORM_VIEW;
	}

	@PostMapping("/user-registration")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "登録")
	public String register(
			@Validated(UserForm.OnCreate.class) @ModelAttribute("userForm") UserForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);
		if (!form.getPassword().equals(form.getPasswordConfirm())) {
			bindingResult.rejectValue("passwordConfirm", "error.passwordConfirm", "パスワードが一致しません");
		}
		if (bindingResult.hasErrors()) {
			model.addAttribute("roles", selectableRoles(jichitaiCd, form.getRoleId()));
			model.addAttribute("isEdit", false);
			model.addAttribute("isView", false);
			model.addAttribute("isDefaultUser", false);
			model.addAttribute("validationErrors", UserForm.validate(form, true).values());
			return FORM_VIEW;
		}
		User user = userRepository.findById(buildUserId(form.getId()))
				.orElse(null);
		if (user != null && "0".equals(user.getDelFlg())) {
			bindingResult.rejectValue("id", "error.id", "このIDは既に登録済みです");
			model.addAttribute("roles", selectableRoles(jichitaiCd, form.getRoleId()));
			model.addAttribute("isEdit", false);
			model.addAttribute("isView", false);
			model.addAttribute("isDefaultUser", false);
			return FORM_VIEW;
		}
		if (user == null) {
			user = new User();
			user.setJichitaiCd(jichitaiCd);
			user.setId(form.getId());
		} else {
			user.setDelFlg("0");
		}
		user.setPassword(passwordEncoder.encode(form.getPassword()));
		user.setName(form.getName());
		user.setNameKana(form.getNameKana());
		user.setBusho(form.getBusho());
		user.setRoleId(form.getRoleId());
		user.setInitialPasswordFlg("1");
		userRepository.save(user);
		redirectAttributes.addFlashAttribute("successMessage", "ユーザーを登録しました。");
		return "redirect:/admin/user-search";
	}

	@GetMapping("/user-edit/{id}")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "編集画面表示")
	public String showEditForm(@PathVariable String id, Model model) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);
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
		model.addAttribute("roles", selectableRoles(jichitaiCd, form.getRoleId()));
		model.addAttribute("isEdit", true);
		model.addAttribute("isView", false);
		model.addAttribute("isDefaultUser", InitialPasswordController.ADMIN_ID.equals(user.getId()));
		return FORM_VIEW;
	}

	@GetMapping("/user-view/{id}")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "照会画面表示")
	public String showViewForm(@PathVariable String id, Model model) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkAccess(SCREEN_ID_CONFIG);
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
		model.addAttribute("roles", selectableRoles(jichitaiCd, form.getRoleId()));
		model.addAttribute("isEdit", true);
		model.addAttribute("isView", true);
		model.addAttribute("isDefaultUser", InitialPasswordController.ADMIN_ID.equals(user.getId()));
		return FORM_VIEW;
	}

	@PostMapping("/user-edit/{id}")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "編集")
	public String update(
			@PathVariable String id,
			@Validated(UserForm.OnUpdate.class) @ModelAttribute("userForm") UserForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);

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
			model.addAttribute("roles", selectableRoles(jichitaiCd, form.getRoleId()));
			model.addAttribute("isEdit", true);
			model.addAttribute("isView", false);
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
			user.setInitialPasswordFlg("1");
		}
		userRepository.save(user);

		if (isLoginUser(id)) {
			updateSessionAuthentication(user);
		}

		redirectAttributes.addFlashAttribute("successMessage", "ユーザー情報を更新しました。");
		return "redirect:/admin/user-search";
	}

	@PostMapping("/user-delete/{id}")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "削除")
	public String delete(@PathVariable String id, RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);
		if (isLoginUser(id)) {
			redirectAttributes.addFlashAttribute("errorMessage", "ログイン中のユーザーは削除できません。");
			return "redirect:/admin/user-search";
		}
		// 論理削除（削除フラグを立てる。同一IDでの再登録時に復活させるため物理削除はしない）
		User user = userRepository.findById(buildUserId(id))
				.orElseThrow(() -> new RuntimeException("ユーザーが見つかりません: " + id));
		user.setDelFlg("1");
		userRepository.save(user);
		redirectAttributes.addFlashAttribute("successMessage", "ユーザーを削除しました。");
		return "redirect:/admin/user-search";
	}

	private boolean isLoginUser(String id) {
		return getLoginUserId().equals(id);
	}

	private String getLoginUserId() {
		return SecurityContextHolder.getContext().getAuthentication().getName();
	}

	private void updateSessionAuthentication(User user) {
		Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
		AppUserDetails updatedDetails = new AppUserDetails(
				user.getId(),
				user.getPassword(),
				currentAuth.getAuthorities(),
				"1".equals(user.getInitialPasswordFlg()));
		updatedDetails.setDisplayName(user.getName());
		Authentication newAuth = new UsernamePasswordAuthenticationToken(
				updatedDetails, updatedDetails.getPassword(), updatedDetails.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(newAuth);
	}

	private UserId buildUserId(String id) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		UserId pk = new UserId();
		pk.setJichitaiCd(jichitaiCd);
		pk.setId(id);
		return pk;
	}

	private String emptyToNull(String s) {
		return (s == null || s.isBlank()) ? null : s;
	}

	private String toLikePattern(String value, String matchType) {
		if (value == null || value.isBlank())
			return null;
		return switch (matchType) {
		case "prefix" -> value + "%";
		case "exact" -> value;
		default -> "%" + value + "%"; // partial
		};
	}
}

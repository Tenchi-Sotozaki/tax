package jp.lg.asp.accommodation.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.RoleForm;
import jp.lg.asp.accommodation.entity.Role;
import jp.lg.asp.accommodation.entity.Screen;
import jp.lg.asp.accommodation.entity.User;
import jp.lg.asp.accommodation.service.RoleService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/role")
@RequiredArgsConstructor
public class RoleController {

	private final RoleService roleService;
	private final ScreenAccessChecker accessChecker;

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	private static final String SCREEN_ID = ScreenManagement.ROLE_MANAGEMENT;

	@GetMapping("/management")
	public String roleManagement(Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		try {
			List<Role> roles = roleService.findAllRoles(jichitaiCd);
			List<Screen> screens = roleService.findAllScreens();
			model.addAttribute("roles", roles);
			model.addAttribute("screens", screens);
			return "admin/roleManagement";
		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("roles", java.util.Collections.emptyList());
			model.addAttribute("screens", java.util.Collections.emptyList());
			return "admin/roleManagement";
		}
	}

	@PostMapping("/save")
	@ResponseBody
	public Map<String, Object> saveRole(@RequestBody RoleForm form) {
		accessChecker.checkAccess(SCREEN_ID);
		Map<String, Object> result = new HashMap<>();
		if (form.getName() == null || form.getName().isBlank()) {
			result.put("success", false);
			result.put("errors", java.util.List.of("権限名は必須です"));
			return result;
		}
		try {
			roleService.saveRole(form, jichitaiCd, "admin");
			result.put("success", true);
		} catch (Exception e) {
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@GetMapping("/detail/{roleId}")
	@ResponseBody
	public Map<String, Object> getRoleDetail(@PathVariable Long roleId) {
		accessChecker.checkAccess(SCREEN_ID);
		Role role = roleService.findById(jichitaiCd, roleId);

		Map<String, Object> result = new HashMap<>();
		if (role == null)
			return result;

		Map<String, Object> roleMap = new HashMap<>();
		roleMap.put("roleId", role.getRoleId());
		roleMap.put("name", role.getName());
		roleMap.put("version", role.getVersion());
		result.put("role", roleMap);

		if (role.getRoleDetails() != null) {
			Map<String, String> permissions = role.getRoleDetails().stream()
					.collect(Collectors.toMap(
							rd -> rd.getScreenId(),
							rd -> rd.getPermission()));
			result.put("permissions", permissions);
		}

		return result;
	}

	@GetMapping("/users/{roleId}")
	@ResponseBody
	public Map<String, Object> getAssignedUsers(@PathVariable Long roleId) {
		accessChecker.checkAccess(SCREEN_ID);
		Role role = roleService.findById(jichitaiCd, roleId);
		List<User> allUsers = roleService.findAllUsers(jichitaiCd);

		Map<String, Object> result = new HashMap<>();
		result.put("roleName", role != null ? role.getName() : "");
		result.put("users", allUsers.stream().map(u -> {
			Map<String, Object> m = new HashMap<>();
			m.put("id", u.getId());
			m.put("name", u.getName());
			m.put("assigned", u.getRoleId() != null && u.getRoleId().longValue() == roleId);
			return m;
		}).collect(Collectors.toList()));
		return result;
	}

	@PostMapping("/users/{roleId}")
	@ResponseBody
	public Map<String, Object> updateAssignedUsers(@PathVariable Long roleId,
			@RequestBody Map<String, List<String>> body) {
		accessChecker.checkAccess(SCREEN_ID);
		Map<String, Object> result = new HashMap<>();
		try {
			roleService.updateUserRole(jichitaiCd, roleId, body.get("userIds"), "admin");
			result.put("success", true);
		} catch (Exception e) {
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@PostMapping("/delete/{roleId}")
	@ResponseBody
	public Map<String, Object> deleteRole(@PathVariable Long roleId) {
		accessChecker.checkAccess(SCREEN_ID);
		Map<String, Object> result = new HashMap<>();
		
		if (roleId == 1L) {
			result.put("success", false);
			result.put("message", "デフォルト権限のため削除できません");
			return result;
		}
		
		try {
			// 削除対象の権限が付与されているユーザーをデフォルト権限に変更
			roleService.resetUsersToDefaultRole(jichitaiCd, roleId, "admin");
			
			roleService.deleteRole(jichitaiCd, roleId);
			result.put("success", true);
		} catch (Exception e) {
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}
}

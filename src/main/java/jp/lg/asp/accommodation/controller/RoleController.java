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

import jp.lg.asp.accommodation.dto.RoleForm;
import jp.lg.asp.accommodation.entity.Role;
import jp.lg.asp.accommodation.entity.Screen;
import jp.lg.asp.accommodation.service.RoleService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/role")
@RequiredArgsConstructor
public class RoleController {

	private final RoleService roleService;

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	@GetMapping("/management")
	public String roleManagement(Model model) {
		try {
			List<Role> roles = roleService.findAllRoles(jichitaiCd);
			List<Screen> screens = roleService.findAllScreens();

			model.addAttribute("roles", roles);
			model.addAttribute("screens", screens);
			
			// デバッグ用ログ
			System.out.println("Roles count: " + (roles != null ? roles.size() : 0));
			System.out.println("Screens count: " + (screens != null ? screens.size() : 0));
			
			return "admin/roleManagement";
		} catch (Exception e) {
			e.printStackTrace();
			// エラーが発生した場合は空のリストを設定
			model.addAttribute("roles", java.util.Collections.emptyList());
			model.addAttribute("screens", java.util.Collections.emptyList());
			return "admin/roleManagement";
		}
	}

	@PostMapping("/save")
	@ResponseBody
	public Map<String, Object> saveRole(@RequestBody RoleForm form) {
		Map<String, Object> result = new HashMap<>();
		try {
			String userId = "admin"; // セッションから取得

			roleService.saveRole(form, jichitaiCd, userId);
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
		Role role = roleService.findById(jichitaiCd, roleId);
		List<Screen> screens = roleService.findAllScreens();

		Map<String, Object> result = new HashMap<>();
		result.put("role", role);
		result.put("screens", screens);

		if (role != null && role.getRoleDetails() != null) {
			Map<String, Integer> permissions = role.getRoleDetails().stream()
					.collect(Collectors.toMap(
							rd -> rd.getScreenId(),
							rd -> rd.getPermission()));
			result.put("permissions", permissions);
		}

		return result;
	}

	@PostMapping("/delete/{roleId}")
	@ResponseBody
	public Map<String, Object> deleteRole(@PathVariable Long roleId) {
		Map<String, Object> result = new HashMap<>();
		try {
			roleService.deleteRole(jichitaiCd, roleId);
			result.put("success", true);
		} catch (Exception e) {
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}
}
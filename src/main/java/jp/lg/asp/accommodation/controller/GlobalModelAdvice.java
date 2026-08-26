package jp.lg.asp.accommodation.controller;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;

import jp.lg.asp.accommodation.config.AppUserDetails;
import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.MenuDto;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Menu;
import jp.lg.asp.accommodation.exception.AccessDeniedException;
import jp.lg.asp.accommodation.service.GlobalModelService;
import jp.lg.asp.accommodation.service.JichitaiConfigService;
import jp.lg.asp.accommodation.service.MenuService;
import jp.lg.asp.accommodation.util.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

	private final GlobalModelService globalModelService;
	private final JichitaiConfigService jichitaiConfigService;
	private final MenuService menuService;
	private final JichitaiContext jichitaiContext;

	private static final String OPERATOR_JICHITAI_CD = "99999";

	@ModelAttribute("loginUserName")
	public String loginUserName() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
			return "";
		}
		if (auth.getPrincipal() instanceof AppUserDetails details) {
			return details.getDisplayName() != null ? details.getDisplayName() : auth.getName();
		}
		return auth.getName();
	}

	@ModelAttribute("currentUri")
	public String currentUri(HttpServletRequest request) {
		return request.getRequestURI();
	}

	@ModelAttribute("flashSuccessMessage")
	public String flashSuccessMessage(HttpServletRequest request) {
		var session = request.getSession(false);
		if (session == null) return null;
		String msg = (String) session.getAttribute("flashSuccessMessage");
		if (msg != null) session.removeAttribute("flashSuccessMessage");
		return msg;
	}

	@ModelAttribute("selectedShiteiGassan")
	public ShiteiGassanSearchDto selectedShiteiGassan(HttpServletRequest request) {
		return SessionHelper.getShiteiGassan(request);
	}

	@ModelAttribute("accessibleScreens")
	public Set<String> accessibleScreens() {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
			return Collections.emptySet();
		}
		return globalModelService.getAccessibleScreens(jichitaiCd, auth.getName());
	}

	@ModelAttribute("sideMenuTree")
	public List<MenuDto> sideMenuTree() {
		String jichitaiCd = jichitaiContext.getJichitaiCd();

		try {
			boolean isOperator = OPERATOR_JICHITAI_CD.equals(jichitaiCd);
			boolean isMonthly = jichitaiConfigService.findById(jichitaiCd)
					.map(Jichitai::getNozeiShuki)
					.map("1"::equals)
					.orElse(false);
			// 運用者は全画面許可として扱う
			Set<String> screens = isOperator ? Set.of("*") : accessibleScreens();
			List<Menu> menus = menuService.findAllOrderByDspOdr();
			Map<String, MenuDto> map = new LinkedHashMap<>();
			for (Menu m : menus) {
				if (!isDspKbnVisible(m.getDspKbn(), isOperator, isMonthly)) continue;
				MenuDto dto = new MenuDto();
				dto.setMenuId(m.getMenuId());
				dto.setLevel(m.getLevel());
				dto.setPMenuId(m.getPMenuId());
				dto.setName(m.getName());
				dto.setScreenId(m.getScreenId());
				dto.setIconLink(m.getIconLink());
				dto.setLink(m.getLink());
				map.put(m.getMenuId(), dto);
			}
			// ツリー構築
			List<MenuDto> roots = new java.util.ArrayList<>();
			for (MenuDto dto : map.values()) {
				if (dto.getLevel() == 1) {
					roots.add(dto);
				} else {
					MenuDto parent = map.get(dto.getPMenuId());
					if (parent != null) {
						parent.getChildren().add(dto);
					}
				}
			}
			// 権限フィルタリング：下位から上位へ不要ノードを除去
			for (MenuDto lv1 : roots) {
				for (MenuDto lv2 : lv1.getChildren()) {
					for (MenuDto lv3 : lv2.getChildren()) {
						lv3.getChildren().removeIf(lv4 -> !isAccessible(lv4, screens));
					}
					lv2.getChildren().removeIf(lv3 ->
						!isAccessible(lv3, screens) ||
						(lv3.getLink() == null && lv3.getChildren().isEmpty())
					);
				}
				lv1.getChildren().removeIf(lv2 ->
					!isAccessible(lv2, screens) ||
					(lv2.getLink() == null && lv2.getChildren().isEmpty())
				);
			}
			roots.removeIf(lv1 -> lv1.getChildren().isEmpty());
			return roots;
		} catch (Exception e) {
			log.error("sideMenuTree取得エラー: {}", e.getMessage());
			return Collections.emptyList();
		}
	}

	private boolean isAccessible(MenuDto menu, Set<String> screens) {
		return screens.contains("*") || menu.getScreenId() == null || screens.contains(menu.getScreenId().strip());
	}

	private boolean isDspKbnVisible(String dspKbn, boolean isOperator, boolean isMonthly) {
		if ("1".equals(dspKbn)) return true;
		if ("2".equals(dspKbn)) return isMonthly;
		if ("3".equals(dspKbn)) return isOperator;
		return false;
	}

	@ExceptionHandler(AccessDeniedException.class)
	public String handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		log.warn("アクセス拒否: screenId={}, userId={}", ex.getScreenId(), ex.getUserId());
		request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 403);
		request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, request.getRequestURI());
		request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, ex);
		return "forward:/error";
	}
}

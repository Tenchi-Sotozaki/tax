package jp.lg.asp.accommodation.controller;

import java.util.Collections;
import java.util.List;
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
import jp.lg.asp.accommodation.exception.AccessDeniedException;
import jp.lg.asp.accommodation.service.GlobalModelService;
import jp.lg.asp.accommodation.util.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

	private final GlobalModelService globalModelService;
	private final JichitaiContext jichitaiContext;

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
		Set<String> screens = accessibleScreens();
		return globalModelService.buildSideMenuTree(jichitaiCd, screens);
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

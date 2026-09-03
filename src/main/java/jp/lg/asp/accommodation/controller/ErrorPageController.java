package jp.lg.asp.accommodation.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jp.lg.asp.accommodation.service.ErrorPageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ErrorPageController implements ErrorController {

	private final ErrorPageService errorPageService;

	@GetMapping("/error")
	public String handleError(HttpServletRequest request) {
		Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		int statusCode = statusAttr != null ? Integer.parseInt(statusAttr.toString()) : 500;

		String uri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
		if (uri == null || !uri.endsWith(".map")) {
			errorPageService.saveLog(request, statusCode);
		}

		if (statusCode == HttpStatus.NOT_FOUND.value()) {
			return "error/404";
		}
		if (statusCode == HttpStatus.FORBIDDEN.value()) {
			return "error/403";
		}
		Throwable ex = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
		if (ex != null) {
			log.error("500エラーが発生しました URI={}", uri, ex);
		} else {
			log.error("500エラーが発生しました URI={}", uri);
		}
		return "error/500";
	}
}

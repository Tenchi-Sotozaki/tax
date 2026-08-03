package jp.lg.asp.accommodation.controller;

import java.time.LocalDateTime;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.entity.OperationLog;
import jp.lg.asp.accommodation.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ErrorPageController implements ErrorController {

	private final OperationLogRepository operationLogRepository;
	private final JichitaiContext jichitaiContext;

	@RequestMapping("/error")
	public String handleError(HttpServletRequest request) {
		Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		int statusCode = statusAttr != null ? Integer.parseInt(statusAttr.toString()) : 500;

		String uri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
		if (uri == null || !uri.endsWith(".map")) {
			saveLog(request, statusCode);
		}

		if (statusCode == HttpStatus.BAD_REQUEST.value()) {
			// 入力値の変換に失敗した場合など。コンテナ標準の英語表記が出ないよう専用画面を返す
			return "error/400";
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

	private void saveLog(HttpServletRequest request, int statusCode) {
		try {
			String jichitaiCd = jichitaiContext.getJichitaiCd();
			String uri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
			String method = request.getMethod();
			String statusStr = String.valueOf(statusCode);

			OperationLog entity = new OperationLog();
			entity.setJichitaiCd(jichitaiCd);
			entity.setSeq(operationLogRepository.findNextSeq(jichitaiCd));
			entity.setScreenId("error_" + statusStr);
			entity.setSousa(statusStr + "エラー");
			entity.setMethod(method);
			entity.setPath(uri);
			entity.setStatus(statusStr);
			entity.setOpeUser(getCurrentUserId());
			entity.setOpeDt(LocalDateTime.now());

			operationLogRepository.save(entity);
		} catch (Exception e) {
			log.warn("{}操作ログの保存に失敗しました", statusCode, e);
		}
	}

	private String getCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null ? authentication.getName() : "anonymous";
	}
}

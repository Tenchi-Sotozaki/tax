package jp.lg.asp.accommodation.aspect;
import jp.lg.asp.accommodation.config.JichitaiContext;

import java.time.LocalDateTime;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.entity.OperationLog;
import jp.lg.asp.accommodation.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 操作ログ記録AOP
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class OpeLogAspect {

	private final OperationLogRepository operationLogRepository;
	private final ObjectMapper objectMapper;

	private final JichitaiContext jichitaiContext;

	/**
	 * @OpeLogが付与されたメソッドの前後で操作ログをDBに保存
	 */
	@Around("@annotation(opeLog)")
	public Object logOperation(ProceedingJoinPoint joinPoint, OpeLog opeLog) throws Throwable {
		try {
			Object result = joinPoint.proceed();
			saveLog(opeLog);
			return result;
		} catch (Throwable throwable) {
			saveLog(opeLog);
			throw throwable;
		}
	}

	private void saveLog(OpeLog opeLog) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		try {
			HttpServletRequest request = getCurrentRequest();
			String param = request != null ? getRequestParameters(request) : null;
			if (param != null && param.length() > 2000) {
				param = param.substring(0, 2000);
			}

			OperationLog entity = new OperationLog();
			entity.setJichitaiCd(jichitaiCd);
			entity.setSeq(operationLogRepository.findNextSeq(jichitaiCd));
			entity.setScreenId(opeLog.screenId());
			entity.setSousa(opeLog.operation());
			entity.setParam(param);
			entity.setOpeUser(getCurrentUserId());
			entity.setOpeDt(LocalDateTime.now());

			operationLogRepository.save(entity);
		} catch (Exception e) {
			log.warn("操作ログの保存に失敗しました", e);
		}
	}

	private HttpServletRequest getCurrentRequest() {
		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		return attributes != null ? attributes.getRequest() : null;
	}

	private String getCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null ? authentication.getName() : "anonymous";
	}

	private String getRequestParameters(HttpServletRequest request) {
		try {
			Map<String, Object> parameterMap = new java.util.LinkedHashMap<>();
			parameterMap.put("path", request.getRequestURI());
			request.getParameterMap().entrySet().stream()
					.filter(e -> !e.getKey().equalsIgnoreCase("_csrf"))
					.forEach(e -> parameterMap.put(e.getKey(), e.getValue()));
			return objectMapper.writeValueAsString(parameterMap);
		} catch (Exception e) {
			log.warn("リクエストパラメータの取得に失敗", e);
			return null;
		}
	}
}

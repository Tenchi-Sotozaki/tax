package jp.lg.asp.accommodation.aspect;

import java.time.LocalDateTime;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jp.lg.asp.accommodation.annotation.RptLog;
import jp.lg.asp.accommodation.entity.ReportsLog;
import jp.lg.asp.accommodation.repository.ReportsLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 帳票ログ記録AOP
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RptLogAspect {

	private final ReportsLogRepository reportsLogRepository;

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	/**
	 * @RptLogが付与されたメソッドの前後で帳票ログをDBに保存
	 */
	@Around("@annotation(rptLog)")
	public Object logOperation(ProceedingJoinPoint joinPoint, RptLog rptLog) throws Throwable {
		try {
			Object result = joinPoint.proceed();
			saveLog(rptLog, joinPoint);
			return result;
		} catch (Throwable throwable) {
			saveLog(rptLog, joinPoint);
			throw throwable;
		}
	}

	private void saveLog(RptLog rptLog, ProceedingJoinPoint joinPoint) {
		try {
			ReportsLog entity = new ReportsLog();
			entity.setJichitaiCd(jichitaiCd);
			entity.setSeq(reportsLogRepository.findNextSeq(jichitaiCd));
			entity.setRptId(rptLog.rptId());
			entity.setSousa(rptLog.operation());
			entity.setShiteiNo(resolveShiteiNo(rptLog, joinPoint));
			entity.setOpeUser(getCurrentUserId());
			entity.setOpeDt(LocalDateTime.now());

			reportsLogRepository.save(entity);
		} catch (Exception e) {
			log.warn("帳票ログの保存に失敗しました", e);
		}
	}

	private String resolveShiteiNo(RptLog rptLog, ProceedingJoinPoint joinPoint) {
		String shiteiNo = rptLog.shiteiNo();
		if (shiteiNo.isEmpty()) {
			return null;
		}
		try {
			MethodSignature sig = (MethodSignature) joinPoint.getSignature();
			EvaluationContext context = new StandardEvaluationContext();
			String[] paramNames = sig.getParameterNames();
			Object[] args = joinPoint.getArgs();
			for (int i = 0; i < paramNames.length; i++) {
				context.setVariable(paramNames[i], args[i]);
			}
			return new SpelExpressionParser().parseExpression(shiteiNo).getValue(context, String.class);
		} catch (Exception e) {
			log.warn("shiteiNoのSpEL評価に失敗しました: {}", shiteiNo, e);
			return null;
		}
	}

	private String getCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null ? authentication.getName() : "anonymous";
	}
}

package jp.lg.asp.accommodation.service.impl;

import java.time.LocalDateTime;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.entity.OperationLog;
import jp.lg.asp.accommodation.repository.OperationLogRepository;
import jp.lg.asp.accommodation.service.ErrorPageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorPageServiceImpl implements ErrorPageService {

    private final OperationLogRepository operationLogRepository;
    private final JichitaiContext jichitaiContext;

    @Override
    public void saveLog(HttpServletRequest request, int statusCode) {
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

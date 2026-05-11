package jp.lg.asp.accommodation.controller;

import jakarta.servlet.http.HttpServletRequest;
import jp.lg.asp.accommodation.exception.AccessDeniedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;

@Slf4j
@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex) {
        log.warn("アクセス拒否: screenId={}, userId={}", ex.getScreenId(), ex.getUserId());
        return "error/403";
    }
}

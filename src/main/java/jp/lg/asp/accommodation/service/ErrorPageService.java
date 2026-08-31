package jp.lg.asp.accommodation.service;

import jakarta.servlet.http.HttpServletRequest;

public interface ErrorPageService {

    void saveLog(HttpServletRequest request, int statusCode);
}

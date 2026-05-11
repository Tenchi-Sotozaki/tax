package jp.lg.asp.accommodation.exception;

public class AccessDeniedException extends RuntimeException {

    private final String screenId;
    private final String userId;

    public AccessDeniedException(String screenId, String userId) {
        super("画面へのアクセス権限がありません。screenId=" + screenId + ", userId=" + userId);
        this.screenId = screenId;
        this.userId = userId;
    }

    public String getScreenId() { return screenId; }
    public String getUserId() { return userId; }
}

package jp.lg.asp.accommodation.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super("ERR_NOT_FOUND", message);
    }
}

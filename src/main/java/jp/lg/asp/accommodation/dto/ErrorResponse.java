package jp.lg.asp.accommodation.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Builder
public class ErrorResponse {

    private String code;
    private String message;
    private List<String> fieldErrors;
    private LocalDateTime timestamp;
}

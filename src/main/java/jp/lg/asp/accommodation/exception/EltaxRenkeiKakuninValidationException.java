package jp.lg.asp.accommodation.exception;

import java.util.List;

import jp.lg.asp.accommodation.dto.EltaxRenkeiKakuninDto;

public class EltaxRenkeiKakuninValidationException extends RuntimeException {

    private final List<String> errorMessages;
    private final EltaxRenkeiKakuninDto dto;

    public EltaxRenkeiKakuninValidationException(List<String> errorMessages, EltaxRenkeiKakuninDto dto) {
        super(String.join("\n", errorMessages));
        this.errorMessages = errorMessages;
        this.dto = dto;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public EltaxRenkeiKakuninDto getDto() {
        return dto;
    }
}

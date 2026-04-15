package jp.lg.asp.accommodation.exception;

public class DuplicateDeclarationException extends BusinessException {

    public DuplicateDeclarationException(String collectorId, String facilityId, String paymentYearMonth) {
        super("ERR_DUPLICATE_DECLARATION",
                String.format("納入申告が既に登録されています。[徴収義務者ID: %s, 施設ID: %s, 納入年月: %s]",
                        collectorId, facilityId, paymentYearMonth));
    }
}

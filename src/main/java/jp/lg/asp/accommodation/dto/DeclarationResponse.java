package jp.lg.asp.accommodation.dto;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.util.List;

@Getter @Builder
public class DeclarationResponse {

    private Long declarationId;
    private String collectorId;
    private String collectorName;
    private String facilityId;
    private String facilityName;
    private String paymentYearMonth;
    private Integer totalNights;
    private Integer exemptNights;
    private BigDecimal totalPaymentAmount;
    private String status;
    private List<DeclarationDetailResponse> details;

    @Getter @Builder
    public static class DeclarationDetailResponse {
        private String taxCategoryCode;
        private String categoryName;
        private Integer taxableNights;
        private BigDecimal taxAmountPerNight;
        private BigDecimal subtotalAmount;
    }
}

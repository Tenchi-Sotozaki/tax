package jp.lg.asp.accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CollectorListItem {

    private Long id;
    private String registrationNo;
    private String obligorName;
    private String facilityName;
    private String businessType;
    private String businessTypeLabel;
    private String consolidationTarget; // "target" or "non-target"
    /** ステータス: 1=営業中 / 2=休止 / 3=廃止 */
    private String status;
}

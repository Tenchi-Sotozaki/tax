package jp.lg.asp.accommodation.dto;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.Data;

@Data
public class RptStatusListItem {
    private String shiteiNo;
    private String name;
    private String shisetsuName;
    /** rptId -> createDt のマップ */
    private Map<String, LocalDateTime> rptStatusMap;
}

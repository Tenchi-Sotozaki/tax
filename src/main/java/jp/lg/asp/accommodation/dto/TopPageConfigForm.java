package jp.lg.asp.accommodation.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class TopPageConfigForm {
	
    private Integer seq;
    
    private String title;
    
    private String htmlContent;    
    
    private LocalDate postingStartDate;

    private LocalDate postingEndDate;
    
}

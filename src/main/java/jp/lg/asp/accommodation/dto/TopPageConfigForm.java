package jp.lg.asp.accommodation.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class TopPageConfigForm {
	
    private Integer seq;
    
    
    @NotBlank(message = "タイトルを入力してください")
    private String title;
    
    @NotBlank(message = "内容を入力してください")
    private String htmlContent;    
    
    private LocalDate postingStartDate;

    private LocalDate postingEndDate;
    
}

package jp.lg.asp.accommodation.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class TopPageConfigForm {
	
    private Integer seq;
    
    
    @NotBlank(message = "タイトルを入力してください")
    private String title;
    
    @NotBlank(message = "内容を入力してください")
    private String htmlContent;    
    
    @NotNull(message = "掲載開始日を入力してください")
    private LocalDate postingStartDate;

    private LocalDate postingEndDate;
    
}

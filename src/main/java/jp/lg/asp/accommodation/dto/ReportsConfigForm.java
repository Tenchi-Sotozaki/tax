package jp.lg.asp.accommodation.dto;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class ReportsConfigForm {
    
    private MultipartFile file;
    private String fileName;
    private String importDateTime;
    private String importUser;
}
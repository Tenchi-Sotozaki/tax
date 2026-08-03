package jp.lg.asp.accommodation.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jp.lg.asp.accommodation.entity.KoinTorikomi;

public interface KoinTorikomiService {
    
    List<KoinTorikomi> getImportHistory();
    
    void importReportFile(MultipartFile file, String jichitaiCd, String userId);
}
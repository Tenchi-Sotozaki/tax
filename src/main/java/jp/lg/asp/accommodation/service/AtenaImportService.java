package jp.lg.asp.accommodation.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jp.lg.asp.accommodation.entity.AtenaRenkei;

public interface AtenaImportService {
    AtenaRenkei importCsv(MultipartFile file, String jichitaiCd, String userId);
    List<AtenaRenkei> findHistory(String jichitaiCd);
}

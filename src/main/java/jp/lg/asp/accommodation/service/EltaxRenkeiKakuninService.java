package jp.lg.asp.accommodation.service;

import java.math.BigDecimal;

import org.springframework.web.multipart.MultipartFile;

import jp.lg.asp.accommodation.dto.EltaxRenkeiKakuninDto;

public interface EltaxRenkeiKakuninService {

	EltaxRenkeiKakuninDto preview(MultipartFile file);

	void commit(byte[] fileBytes, String fileName, BigDecimal atenaNoFromSession);
}

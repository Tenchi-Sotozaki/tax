package jp.lg.asp.accommodation.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jp.lg.asp.accommodation.dto.EltaxRenkeiDto;
import jp.lg.asp.accommodation.entity.EltaxRenkei;
import jp.lg.asp.accommodation.entity.EltaxRenkeiId;
import jp.lg.asp.accommodation.repository.EltaxRenkeiRepository;
import jp.lg.asp.accommodation.service.EltaxRenkeiService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EltaxRenkeiServiceImpl implements EltaxRenkeiService {

	private final EltaxRenkeiRepository eltaxRenkeiRepository;

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	@Override
	@Transactional(readOnly = true)
	public List<EltaxRenkeiDto> findAll() {
		return eltaxRenkeiRepository.findByJichitaiCd(jichitaiCd)
				.stream()
				.map(e -> new EltaxRenkeiDto(e.getSeq(), e.getFileName(), e.getShubetsu(), e.getShoriDt(),
						e.getShoriKekka()))
				.toList();
	}

	@Override
	@Transactional
	public void importFile(MultipartFile file) {
		try {
			BigDecimal nextSeq = eltaxRenkeiRepository.findNextSeq(jichitaiCd);
			LocalDateTime now = LocalDateTime.now();

			EltaxRenkei entity = new EltaxRenkei();
			entity.setJichitaiCd(jichitaiCd);
			entity.setSeq(nextSeq);
			entity.setFileName(file.getOriginalFilename());
			entity.setShoriDt(now);
			entity.setShoriKekka("1");
			entity.setLog(file.getBytes());

			eltaxRenkeiRepository.save(entity);
		} catch (Exception e) {
			throw new RuntimeException("ファイルの取込に失敗しました: " + e.getMessage(), e);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public EltaxRenkei findBySeq(BigDecimal seq) {
		return eltaxRenkeiRepository.findById(new EltaxRenkeiId(jichitaiCd, seq)).orElse(null);
	}
}

package jp.lg.asp.accommodation.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
	@Transactional(readOnly = true)
	public EltaxRenkei findBySeq(BigDecimal seq) {
		return eltaxRenkeiRepository.findById(new EltaxRenkeiId(jichitaiCd, seq)).orElse(null);
	}
}

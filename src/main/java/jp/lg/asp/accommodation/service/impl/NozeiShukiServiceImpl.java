package jp.lg.asp.accommodation.service.impl;
import jp.lg.asp.accommodation.config.JichitaiContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.NozeiShukiDto;
import jp.lg.asp.accommodation.entity.NozeiShuki;
import jp.lg.asp.accommodation.entity.NozeiShukiId;
import jp.lg.asp.accommodation.repository.NozeiShukiRepository;
import jp.lg.asp.accommodation.service.NozeiShukiService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NozeiShukiServiceImpl implements NozeiShukiService {

	private final NozeiShukiRepository nozeiShukiRepository;

	private final JichitaiContext jichitaiContext;

	@Override
	@Transactional(readOnly = true)
	public List<NozeiShukiDto> findAll() {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		return nozeiShukiRepository.findActiveByJichitaiCd(jichitaiCd)
				.stream()
				.map(n -> new NozeiShukiDto(n.getSeq(), n.getShuki()))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<NozeiShukiDto> findByShuki(Integer shuki) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		if (shuki == null) {
			return findAll();
		}
		return nozeiShukiRepository.findActiveByJichitaiCdAndShuki(jichitaiCd, BigDecimal.valueOf(shuki))
				.stream()
				.map(n -> new NozeiShukiDto(n.getSeq(), n.getShuki()))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public NozeiShuki findBySeq(BigDecimal seq) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		NozeiShukiId id = new NozeiShukiId(jichitaiCd, seq);
		return nozeiShukiRepository.findById(id).orElse(null);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByShuki(BigDecimal shuki) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		return nozeiShukiRepository.countActiveByJichitaiCdAndShuki(jichitaiCd, shuki) > 0;
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByShukiExcludeSeq(BigDecimal shuki, BigDecimal seq) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		return nozeiShukiRepository.countActiveByJichitaiCdAndShukiExcludeSeq(jichitaiCd, shuki, seq) > 0;
	}

	@Override
	@Transactional
	public NozeiShuki save(NozeiShuki nozeiShuki) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		LocalDateTime now = LocalDateTime.now();

		if (nozeiShuki.getSeq() == null) {
			// 新規登録の場合、SEQを自動採番
			BigDecimal nextSeq = nozeiShukiRepository.findNextSeq(jichitaiCd);
			nozeiShuki.setSeq(nextSeq);
			nozeiShuki.setJichitaiCd(jichitaiCd);
			nozeiShuki.setDelFlg("0");
		}

		return nozeiShukiRepository.save(nozeiShuki);
	}

	@Override
	@Transactional
	public void delete(BigDecimal seq) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		NozeiShukiId id = new NozeiShukiId(jichitaiCd, seq);
		NozeiShuki nozeiShuki = nozeiShukiRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("削除対象のデータが見つかりません"));

		// 論理削除
		nozeiShuki.setDelFlg("1");

		nozeiShukiRepository.save(nozeiShuki);
	}
}

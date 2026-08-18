package jp.lg.asp.accommodation.service.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.entity.Nokan;
import jp.lg.asp.accommodation.repository.NokanRepository;
import jp.lg.asp.accommodation.service.NokanService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NokanServiceImpl implements NokanService {
	
	private final JichitaiContext jichitaiContext;
	private final NokanRepository nokanRepository;

	/**
	 * 指定した納税管理人情報を取得
	 * @param ShiteiNo 指定番号
	 * @return 納税管理人情報
	 */
	@Override
	@Transactional(readOnly = true)
	public Optional<Nokan> findByJichitaiCdAndShiteiNo(String ShiteiNo) {
		return nokanRepository.findByJichitaiCdAndShiteiNo(jichitaiContext.getJichitaiCd(), ShiteiNo);
	}
}

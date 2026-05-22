package jp.lg.asp.accommodation.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.FurikomiKozaDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.FurikomiKoza;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.exception.ResourceNotFoundException;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.FurikomiKozaRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.FurikomiKozaService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FurikomiKozaServiceImpl implements FurikomiKozaService {

	private final FurikomiKozaRepository furikomiKozaRepository;
	private final TokugimuRepository tokugimuRepository;
	private final AtenaRepository atenaRepository;

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	@Override
	@Transactional(readOnly = true)
	public FurikomiKozaDto getFurikomiKoza(String shiteiNo) {
		// 特別徴収義務者情報を取得（del_flg='0', new_flg='1'）
		Optional<Tokugimu> tokugimuOpt = tokugimuRepository
				.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(jichitaiCd, shiteiNo, "1", "0");

		if (tokugimuOpt.isEmpty()) {
			throw new ResourceNotFoundException("指定番号に該当する特別徴収義務者が見つかりません: " + shiteiNo);
		}

		Tokugimu tokugimu = tokugimuOpt.get();

		// 宛名情報を取得
		Optional<Atena> atenaOpt = atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, tokugimu.getAtenaNo());
		String shimei = atenaOpt.map(Atena::getName).orElse("");

		// 振込先口座情報を取得
		Optional<FurikomiKoza> furikomiKozaOpt = furikomiKozaRepository
				.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);

		FurikomiKozaDto dto = new FurikomiKozaDto();
		dto.setShiteiNo(shiteiNo);
		dto.setShisetsuName(tokugimu.getShisetsuName());
		dto.setShimei(shimei);

		if (furikomiKozaOpt.isPresent()) {
			FurikomiKoza furikomiKoza = furikomiKozaOpt.get();
			dto.setBankCd(furikomiKoza.getBankCd());
			dto.setBankName(furikomiKoza.getBankName());
			dto.setBranchCd(furikomiKoza.getBranchCd());
			dto.setBranchName(furikomiKoza.getBranchName());
			dto.setShumoku(furikomiKoza.getShumoku());
			dto.setKozaNo(furikomiKoza.getKozaNo());
			dto.setMeigi(furikomiKoza.getMeigi());
			dto.setMode("view");
		} else {
			dto.setMode("register");
		}

		return dto;
	}

	@Override
	@Transactional
	public void registerFurikomiKoza(FurikomiKozaDto dto) {
		// 既存データの存在チェック
		Optional<FurikomiKoza> existingOpt = furikomiKozaRepository
				.findByJichitaiCdAndShiteiNo(jichitaiCd, dto.getShiteiNo());

		if (existingOpt.isPresent()) {
			throw new IllegalStateException("振込先口座情報は既に登録されています");
		}

		FurikomiKoza furikomiKoza = new FurikomiKoza();
		furikomiKoza.setJichitaiCd(jichitaiCd);
		furikomiKoza.setShiteiNo(dto.getShiteiNo());
		furikomiKoza.setBankCd(dto.getBankCd());
		furikomiKoza.setBankName(dto.getBankName());
		furikomiKoza.setBranchCd(dto.getBranchCd());
		furikomiKoza.setBranchName(dto.getBranchName());
		furikomiKoza.setShumoku(dto.getShumoku());
		furikomiKoza.setKozaNo(dto.getKozaNo());
		furikomiKoza.setMeigi(dto.getMeigi());
		furikomiKoza.setAddDt(LocalDateTime.now());
		furikomiKoza.setAddUser("SYSTEM"); // TODO: ログインユーザIDを設定
		furikomiKoza.setUpdDt(LocalDateTime.now());
		furikomiKoza.setUpdUser("SYSTEM"); // TODO: ログインユーザIDを設定
		furikomiKoza.setVersion(1);

		furikomiKozaRepository.save(furikomiKoza);
	}

	@Override
	@Transactional
	public void updateFurikomiKoza(FurikomiKozaDto dto) {
		Optional<FurikomiKoza> existingOpt = furikomiKozaRepository
				.findByJichitaiCdAndShiteiNo(jichitaiCd, dto.getShiteiNo());

		if (existingOpt.isEmpty()) {
			throw new ResourceNotFoundException("更新対象の振込先口座情報が見つかりません");
		}

		FurikomiKoza furikomiKoza = existingOpt.get();
		furikomiKoza.setBankCd(dto.getBankCd());
		furikomiKoza.setBankName(dto.getBankName());
		furikomiKoza.setBranchCd(dto.getBranchCd());
		furikomiKoza.setBranchName(dto.getBranchName());
		furikomiKoza.setShumoku(dto.getShumoku());
		furikomiKoza.setKozaNo(dto.getKozaNo());
		furikomiKoza.setMeigi(dto.getMeigi());
		furikomiKoza.setUpdDt(LocalDateTime.now());
		furikomiKoza.setUpdUser("SYSTEM"); // TODO: ログインユーザIDを設定
		furikomiKoza.setVersion(furikomiKoza.getVersion() + 1);

		furikomiKozaRepository.save(furikomiKoza);
	}
}
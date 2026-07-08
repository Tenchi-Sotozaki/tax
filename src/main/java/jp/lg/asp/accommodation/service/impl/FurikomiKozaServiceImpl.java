package jp.lg.asp.accommodation.service.impl;
import jp.lg.asp.accommodation.config.JichitaiContext;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.FurikomiKozaDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.FurikomiKoza;
import jp.lg.asp.accommodation.entity.FurikomiKozaId;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.FurikomiKozaRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.FurikomiKozaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 振込先口座照会／登録／編集 Service 実装クラス
 * 仕様書：振込先口座照会・登録・編集.csv に基づく実装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FurikomiKozaServiceImpl implements FurikomiKozaService {

	private final FurikomiKozaRepository furikomiKozaRepository;
	private final TokugimuRepository tokugimuRepository;
	private final AtenaRepository atenaRepository;

	private final JichitaiContext jichitaiContext;

	@Override
	@Transactional(readOnly = true)
	public FurikomiKozaDto getFurikomiKoza(String shiteiNo) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		FurikomiKozaDto dto = new FurikomiKozaDto();
		dto.setShiteiNo(shiteiNo);

		// 特別徴収義務者情報を取得（del_flg='0', new_flg='1'）
		Optional<Tokugimu> tokugimuOpt = tokugimuRepository
				.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(jichitaiCd, shiteiNo, "1", "0");

		if (tokugimuOpt.isPresent()) {
			Tokugimu tokugimu = tokugimuOpt.get();
			dto.setShisetsuName(tokugimu.getShisetsuName());

			// 宛名情報を取得
			Optional<Atena> atenaOpt = atenaRepository
					.findByJichitaiCdAndAtenaNo(jichitaiCd, tokugimu.getAtenaNo());
			if (atenaOpt.isPresent()) {
				dto.setName(atenaOpt.get().getName());
			}
		}

		// 振込先口座情報を取得
		FurikomiKozaId id = new FurikomiKozaId(jichitaiCd, shiteiNo);
		Optional<FurikomiKoza> furikomiKozaOpt = furikomiKozaRepository.findById(id);

		if (furikomiKozaOpt.isPresent()) {
			// 既存データがある場合（照会モード）
			FurikomiKoza furikomiKoza = furikomiKozaOpt.get();
			dto.setBankCd(furikomiKoza.getBankCd());
			dto.setBankName(furikomiKoza.getBankName());
			dto.setBranchCd(furikomiKoza.getBranchCd());
			dto.setBranchName(furikomiKoza.getBranchName());
			dto.setShumoku(furikomiKoza.getShumoku());
			dto.setKozaNo(furikomiKoza.getKozaNo());
			dto.setMeigi(furikomiKoza.getMeigi());
			dto.setVersion(furikomiKoza.getVersion());
			dto.setExists(true);
			dto.setMode("view");
		} else {
			// 新規登録モード
			dto.setExists(false);
			dto.setMode("create");
		}

		return dto;
	}

	@Override
	@Transactional
	public FurikomiKozaDto createFurikomiKoza(FurikomiKozaDto dto) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		// 振込先口座情報を新規登録
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

		furikomiKozaRepository.save(furikomiKoza);

		// DTOを更新
		dto.setExists(true);
		dto.setVersion(1);
		dto.setMode("view");

		return dto;
	}

	@Override
	@Transactional
	public FurikomiKozaDto updateFurikomiKoza(FurikomiKozaDto dto) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		FurikomiKozaId id = new FurikomiKozaId(jichitaiCd, dto.getShiteiNo());
		Optional<FurikomiKoza> furikomiKozaOpt = furikomiKozaRepository.findById(id);

		if (furikomiKozaOpt.isEmpty()) {
			throw new RuntimeException("更新対象の振込先口座情報が見つかりません");
		}

		FurikomiKoza furikomiKoza = furikomiKozaOpt.get();

		// 楽観的排他制御
		if (!furikomiKoza.getVersion().equals(dto.getVersion())) {
			throw new RuntimeException("他のユーザーによって更新されています。画面を再表示してください。");
		}

		// 振込先口座情報を更新
		furikomiKoza.setBankCd(dto.getBankCd());
		furikomiKoza.setBankName(dto.getBankName());
		furikomiKoza.setBranchCd(dto.getBranchCd());
		furikomiKoza.setBranchName(dto.getBranchName());
		furikomiKoza.setShumoku(dto.getShumoku());
		furikomiKoza.setKozaNo(dto.getKozaNo());
		furikomiKoza.setMeigi(dto.getMeigi());

		furikomiKozaRepository.save(furikomiKoza);

		// DTOを更新
		dto.setVersion(furikomiKoza.getVersion());
		dto.setMode("view");

		return dto;
	}
}
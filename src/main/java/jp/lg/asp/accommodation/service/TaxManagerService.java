package jp.lg.asp.accommodation.service;

import java.time.LocalDate;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.TaxManagerForm;
import jp.lg.asp.accommodation.entity.TaxManager;
import jp.lg.asp.accommodation.entity.TaxManagerId;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.TaxManagerRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaxManagerService {

	private final TaxManagerRepository taxManagerRepository;
	private final TokugimuRepository tokugimuRepository;

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	// ========== 規約に基づく定数定義 ==========
	private static final int DEFAULT_RNO = 1;
	private static final String FLG_ON = "1";
	private static final String FLG_OFF = "0";
	// ==========================================

	/**
	 * 納税管理人登録時の特別徴収義務者との同一人物チェック
	 * @param taxManagerAtenaNo 納税管理人の宛名番号
	 * @param obligorAtenaNo 特別徴収義務者の宛名番号
	 * @return 同一人物の場合true
	 */
	public boolean isSamePerson(String taxManagerAtenaNo, String obligorAtenaNo) {
		if (taxManagerAtenaNo == null || taxManagerAtenaNo.trim().isEmpty() ||
			obligorAtenaNo == null || obligorAtenaNo.trim().isEmpty()) {
			return false;
		}
		
		boolean isSame = Objects.equals(taxManagerAtenaNo.trim(), obligorAtenaNo.trim());
		log.debug("同一人物チェック: 納税管理人={}, 特徴={}, 結果={}", 
			taxManagerAtenaNo, obligorAtenaNo, isSame);
		return isSame;
	}

	/**
	 * 指定番号（shiteiNo）からデータを取得し、画面表示用のFormを作成する
	 */
	@Transactional(readOnly = true)
	public TaxManagerForm getByShiteiNo(String shiteiNo) {
		TaxManagerForm form = new TaxManagerForm();
		form.setCollectorId(null);
		form.setShiteiNo(shiteiNo);
		form.setRegistrationDate(LocalDate.now());

		try {
			// 1. 特別徴収義務者の取得
			tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
			        .stream()
		            .findFirst()
					.ifPresent(tokugimu -> {
						form.setObligorName(tokugimu.getKyokaName());
						form.setFacilityName(tokugimu.getShisetsuName());
						form.setObligorAtenaNo(tokugimu.getAtenaNo().toString()); // 特徴の宛名番号を設定
					});

			// 2. 納税管理人の取得 (定数 DEFAULT_RNO を使用)
			TaxManagerId nokanId = new TaxManagerId(jichitaiCd, shiteiNo, DEFAULT_RNO);
			taxManagerRepository.findById(nokanId).ifPresent(nokan -> {
				form.setEdit(true);
				form.setRegistrationDate(nokan.getTorokuYmd());
				form.setAtenaNo(nokan.getAtenaNo());
				form.setManagerName(nokan.getName());
				form.setManagerNameKana(nokan.getNameKana());
				form.setManagerYubinNo(nokan.getYubinNo());
				form.setManagerAddress(nokan.getJusho());
				form.setManagerPhone(nokan.getTel());

				// 定数 FLG_ON を使用
				form.setExemptionFlag(FLG_ON.equals(nokan.getMenjoKbn()));
				form.setExemptionReason(nokan.getMenjoRiyu());
			});
		} catch (Exception e) {
			log.warn("データの取得中にエラーが発生しました。新規登録として処理します: {}", e.getMessage());
		}

		return form;
	}

	/**
	 * 指定番号（shiteiNo）ベースでの保存処理
	 */
	@Transactional
	public void saveByShiteiNo(String shiteiNo, TaxManagerForm form) {
		log.info("納税管理人保存処理開始: shiteiNo={}, atenaNo={}", shiteiNo, form.getAtenaNo());
		
		// 特別徴収義務者との同一人物チェック
		if (form.getAtenaNo() != null && !form.getAtenaNo().trim().isEmpty()) {
			if (isSamePerson(form.getAtenaNo(), form.getObligorAtenaNo())) {
				log.warn("特別徴収義務者と同一人物のため登録拒否: 納税管理人宛名番号={}, 特徴宛名番号={}", 
					form.getAtenaNo(), form.getObligorAtenaNo());
				throw new IllegalArgumentException("特別徴収義務者と同一人物のため、納税管理人として登録できません。");
			}
		} else {
			log.warn("宛名番号が未入力です。shiteiNo={}", shiteiNo);
			throw new IllegalArgumentException("宛名番号は必須です。");
		}

		// 1. 既存データを取得
		TaxManagerId nokanId = new TaxManagerId(jichitaiCd, shiteiNo, DEFAULT_RNO);
		TaxManager entity = taxManagerRepository.findById(nokanId)
				.orElse(new TaxManager());

		// 2.値をマッピング
		entity.setJichitaiCd(jichitaiCd);
		entity.setShiteiNo(shiteiNo);
		entity.setRno(DEFAULT_RNO); 
		
		entity.setMenjoKbn(form.isExemptionFlag() ? FLG_ON : FLG_OFF);
		entity.setTorokuYmd(form.getRegistrationDate());
		entity.setShinkokuYmd(form.getRegistrationDate());

		entity.setAtenaNo(form.getAtenaNo());
		entity.setName(form.getManagerName());
		entity.setNameKana(form.getManagerNameKana());
		entity.setYubinNo(form.getManagerYubinNo());
		entity.setJusho(form.getManagerAddress());
		entity.setTel(form.getManagerPhone());
		entity.setMenjoRiyu(form.getExemptionReason());

		entity.setNewFlg(FLG_ON);
		entity.setDelFlg(FLG_OFF);

		taxManagerRepository.save(entity);
	}
}
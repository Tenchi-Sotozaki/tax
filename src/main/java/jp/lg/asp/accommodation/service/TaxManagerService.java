package jp.lg.asp.accommodation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.TaxManagerForm;
import jp.lg.asp.accommodation.entity.TaxManager;
import jp.lg.asp.accommodation.entity.TaxManagerId;
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
	private final TokugimuService tokugimuService;

	// application.yml (app.jichitai.code) から自治体コードを注入
	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	// ========== 規約に基づく定数定義 ==========
	/** 履歴番号（デフォルト） */
	private static final int DEFAULT_RNO = 1;
	
	/** フラグON / 有効 */
	private static final String FLG_ON = "1";
	
	/** フラグOFF / 無効 */
	private static final String FLG_OFF = "0";
	
	/** システム更新ユーザー名 */
	private static final String SYSTEM_USER = "system";
	
	/** 初期バージョン番号 */
	private static final int INITIAL_VERSION = 1;
	// ==========================================

	/**
	 * IDからデータを取得し、画面表示用のFormを作成する
	 */
	@Transactional(readOnly = true)
	public TaxManagerForm getById(Long id) {
		TaxManagerForm form = new TaxManagerForm();
		form.setCollectorId(id);
		form.setRegistrationDate(LocalDate.now());

		try {
			String shiteiNo = tokugimuService.getShiteiNoById(id);

			// 1. 特別徴収義務者の取得
			tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
			        .stream()
		            .findFirst()
					.ifPresent(tokugimu -> {
						form.setObligorName(tokugimu.getKyokaName());
						form.setFacilityName(tokugimu.getShisetsuName());
					});

			// 2. 納税管理人の取得 (定数 DEFAULT_RNO を使用)
			TaxManagerId nokanId = new TaxManagerId(jichitaiCd, shiteiNo, DEFAULT_RNO);
			taxManagerRepository.findById(nokanId).ifPresent(nokan -> {
				form.setEdit(true);
				form.setRegistrationDate(nokan.getTorokuYmd());
				form.setManagerName(nokan.getName());
				form.setManagerNameKana(nokan.getNameKana());
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
	 * 保存処理
	 */
	@Transactional
	public void save(Long id, TaxManagerForm form) {
		String shiteiNo = tokugimuService.getShiteiNoById(id);
		LocalDateTime now = LocalDateTime.now();

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

		entity.setName(form.getManagerName());
		entity.setNameKana(form.getManagerNameKana());
		entity.setJusho(form.getManagerAddress());
		entity.setTel(form.getManagerPhone());
		entity.setMenjoRiyu(form.getExemptionReason());


		entity.setNewFlg(FLG_ON);
		entity.setDelFlg(FLG_OFF);

		// 3. 共通項目の手動セット
		if (entity.getAddDt() == null) {
			entity.setAddDt(now);
			entity.setAddUser(SYSTEM_USER);
		}
		entity.setUpdDt(now);
		entity.setUpdUser(SYSTEM_USER);
		entity.setVersion(INITIAL_VERSION);

		taxManagerRepository.save(entity);
	}
}
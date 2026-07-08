package jp.lg.asp.accommodation.service;
import jp.lg.asp.accommodation.config.JichitaiContext;

import java.time.LocalDate;
import java.util.Objects;

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

	private final JichitaiContext jichitaiContext;

	// ========== 規約に基づく定数定義 ==========
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
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		TaxManagerForm form = new TaxManagerForm();
		form.setCollectorId(null);
		form.setShiteiNo(shiteiNo);
		form.setRegistrationDate(LocalDate.now());
		form.setDeclarationDate(LocalDate.now()); // 申告日のデフォルト値

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

			// 2. 最新の納税管理人情報を取得（newFlg = '1'）
			taxManagerRepository.findLatestByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo).ifPresent(nokan -> {
				form.setEdit(true);
				form.setRegistrationDate(nokan.getTorokuYmd());
				form.setDeclarationDate(nokan.getShinkokuYmd());
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
	 * 指定番号（shiteiNo）ベースでの保存処理（履歴管理方式）
	 */
	@Transactional
	public void saveByShiteiNo(String shiteiNo, TaxManagerForm form) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		log.info("納税管理人保存処理開始: shiteiNo={}, atenaNo={}", shiteiNo, form.getAtenaNo());
		
		// 選任免除が有効でない場合のみ特別徴収義務者との同一人物チェック
		if (!form.isExemptionFlag() && form.getAtenaNo() != null && !form.getAtenaNo().trim().isEmpty()) {
			if (isSamePerson(form.getAtenaNo(), form.getObligorAtenaNo())) {
				log.warn("特別徴収義務者と同一人物のため登録拒否: 納税管理人宛名番号={}, 特徴宛名番号={}", 
					form.getAtenaNo(), form.getObligorAtenaNo());
				throw new IllegalArgumentException("特別徴収義務者と同一人物のため、納税管理人として登録できません。");
			}
		}

		// 1. 新しい履歴番号を算出
		Integer maxRno = taxManagerRepository.findMaxRnoByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
		Integer newRno = maxRno + 1;
		
		// 2. 既存の最新フラグを0に更新（更新時のみ）
		if (maxRno > 0) {
			taxManagerRepository.updateNewFlgToZero(jichitaiCd, shiteiNo);
			log.info("既存レコードの最新フラグを0に更新: shiteiNo={}", shiteiNo);
		}

		// 3. 新しいレコードを作成（必ずインサート）
		TaxManager newEntity = new TaxManager();
		newEntity.setJichitaiCd(jichitaiCd);
		newEntity.setShiteiNo(shiteiNo);
		newEntity.setRno(newRno);
		
		newEntity.setMenjoKbn(form.isExemptionFlag() ? FLG_ON : FLG_OFF);
		newEntity.setTorokuYmd(form.getRegistrationDate());
		newEntity.setShinkokuYmd(form.getDeclarationDate()); // フォームからの申告日を使用

		// 選任免除が有効でない場合のみ納税管理人情報を設定
		if (!form.isExemptionFlag()) {
			newEntity.setAtenaNo(form.getAtenaNo());
			newEntity.setName(form.getManagerName());
			newEntity.setNameKana(form.getManagerNameKana());
			newEntity.setYubinNo(form.getManagerYubinNo());
			newEntity.setJusho(form.getManagerAddress());
			newEntity.setTel(form.getManagerPhone());
		} else {
			// 選任免除の場合は納税管理人情報をnullに設定
			newEntity.setAtenaNo(null);
			newEntity.setName(null);
			newEntity.setNameKana(null);
			newEntity.setYubinNo(null);
			newEntity.setJusho(null);
			newEntity.setTel(null);
		}
		newEntity.setMenjoRiyu(form.getExemptionReason());

		newEntity.setNewFlg(FLG_ON); // 新しいレコードは必ず最新
		newEntity.setDelFlg(FLG_OFF);

		taxManagerRepository.save(newEntity);
		log.info("納税管理人履歴保存完了: shiteiNo={}, rno={}, 種別={}", 
				shiteiNo, newRno, maxRno > 0 ? "更新" : "新規登録");
	}

	/**
	 * 納税管理人の削除処理（履歴管理方式）
	 */
	@Transactional
	public void deleteByShiteiNo(String shiteiNo) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		log.info("納税管理人削除処理開始: shiteiNo={}", shiteiNo);
		
		// 1. 最新の納税管理人情報を取得
		TaxManager currentRecord = taxManagerRepository.findLatestByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
				.orElseThrow(() -> new IllegalArgumentException("削除対象の納税管理人が見つかりません: " + shiteiNo));
		
		Integer currentRno = currentRecord.getRno();
		
		// 2. 現在のレコードの削除フラグを1に更新
		taxManagerRepository.updateDelFlgToOne(jichitaiCd, shiteiNo, currentRno);
		log.info("現在レコードの削除フラグを更新: shiteiNo={}, rno={}", shiteiNo, currentRno);
		
		// 3. 履歴番号-1のレコードが存在する場合、最新フラグを1に変更
		if (currentRno > 1) {
			Integer previousRno = currentRno - 1;
			taxManagerRepository.updateNewFlgToOneByRno(jichitaiCd, shiteiNo, previousRno);
			log.info("前履歴レコードの最新フラグを更新: shiteiNo={}, rno={}", shiteiNo, previousRno);
		}
		
		log.info("納税管理人削除完了: shiteiNo={}", shiteiNo);
	}
}
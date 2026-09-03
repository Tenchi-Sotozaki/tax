package jp.lg.asp.accommodation.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.TaxManagerForm;
import jp.lg.asp.accommodation.entity.TaxManager;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.TaxManagerRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.TaxManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaxManagerServiceImpl implements TaxManagerService {

	private final TaxManagerRepository taxManagerRepository;
	private final TokugimuRepository tokugimuRepository;
	private final JichitaiContext jichitaiContext;

	private static final String FLG_ON = "1";
	private static final String FLG_OFF = "0";

	@Override
	public boolean isSamePerson(String taxManagerAtenaNo, String obligorAtenaNo) {
		if (taxManagerAtenaNo == null || taxManagerAtenaNo.trim().isEmpty() ||
			obligorAtenaNo == null || obligorAtenaNo.trim().isEmpty()) {
			return false;
		}
		boolean isSame = Objects.equals(taxManagerAtenaNo.trim(), obligorAtenaNo.trim());
		log.debug("同一人物チェック: 納税管理人={}, 特徴={}, 結果={}", taxManagerAtenaNo, obligorAtenaNo, isSame);
		return isSame;
	}

	@Override
	@Transactional(readOnly = true)
	public TaxManagerForm getByShiteiNoAndRno(String shiteiNo, Integer rno) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		TaxManagerForm form = new TaxManagerForm();
		form.setCollectorId(null);
		form.setShiteiNo(shiteiNo);
		form.setRegistrationDate(LocalDate.now());
		form.setDeclarationDate(LocalDate.now());

		List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
		if (tokugimuList.isEmpty()) {
			throw new IllegalArgumentException("特別徴収義務者が設定されていません。");
		}
		form.setObligorAtenaNo(tokugimuList.get(0).getAtenaNo().toString());

		taxManagerRepository.findByJichitaiCdAndShiteiNoAndRno(jichitaiCd, shiteiNo, rno).ifPresent(nokan -> {
			form.setEdit(true);
			form.setRno(nokan.getRno());
			form.setRegistrationDate(nokan.getTorokuYmd());
			form.setDeclarationDate(nokan.getShinkokuYmd());
			form.setAtenaNo(nokan.getAtenaNo());
			form.setManagerName(nokan.getName());
			form.setManagerNameKana(nokan.getNameKana());
			form.setManagerYubinNo(nokan.getYubinNo());
			form.setManagerAddress(nokan.getJusho());
			form.setManagerPhone(nokan.getTel());
			form.setKbn(nokan.getKbn());
			form.setReason(nokan.getRiyu());
		});

		form.setMaxRno(taxManagerRepository.findMaxRnoByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo));
		form.setMinRno(taxManagerRepository.findMinRnoByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo));

		return form;
	}

	@Override
	@Transactional(readOnly = true)
	public TaxManagerForm getByShiteiNo(String shiteiNo) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		TaxManagerForm form = new TaxManagerForm();
		form.setCollectorId(null);
		form.setShiteiNo(shiteiNo);
		form.setRegistrationDate(LocalDate.now());
		form.setDeclarationDate(LocalDate.now());

		List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
		if (tokugimuList.isEmpty()) {
			throw new IllegalArgumentException("特別徴収義務者が設定されていません。");
		}
		form.setObligorAtenaNo(tokugimuList.get(0).getAtenaNo().toString());

		taxManagerRepository.findLatestByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo).ifPresent(nokan -> {
			form.setEdit(true);
			form.setRno(nokan.getRno());
			form.setRegistrationDate(nokan.getTorokuYmd());
			form.setDeclarationDate(nokan.getShinkokuYmd());
			form.setAtenaNo(nokan.getAtenaNo());
			form.setManagerName(nokan.getName());
			form.setManagerNameKana(nokan.getNameKana());
			form.setManagerYubinNo(nokan.getYubinNo());
			form.setManagerAddress(nokan.getJusho());
			form.setManagerPhone(nokan.getTel());
			form.setKbn(nokan.getKbn());
			form.setReason(nokan.getRiyu());
		});

		form.setMaxRno(taxManagerRepository.findMaxRnoByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo));
		form.setMinRno(taxManagerRepository.findMinRnoByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo));

		return form;
	}

	@Override
	@Transactional
	public void saveByShiteiNo(String shiteiNo, TaxManagerForm form) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		log.debug("納税管理人保存処理開始: shiteiNo={}, atenaNo={}", shiteiNo, form.getAtenaNo());

		boolean isExemption = "3".equals(form.getKbn());
		if (isExemption && form.getAtenaNo() != null && !form.getAtenaNo().trim().isEmpty()) {
			throw new IllegalArgumentException("免除時は納税管理人選択不可です。");
		}
		if ("1".equals(form.getKbn()) && (form.getAtenaNo() == null || form.getAtenaNo().trim().isEmpty())) {
			throw new IllegalArgumentException("宛名番号は必須です。");
		}
		if (!isExemption && form.getAtenaNo() != null && !form.getAtenaNo().trim().isEmpty()) {
			if (isSamePerson(form.getAtenaNo(), form.getObligorAtenaNo())) {
				log.warn("特別徴収義務者と同一人物のため登録拒否: 納税管理人宛名番号={}, 特徴宛名番号={}",
					form.getAtenaNo(), form.getObligorAtenaNo());
				throw new IllegalArgumentException("特別徴収義務者と同一人物のため、納税管理人として登録できません。");
			}
		}

		Integer maxRno = taxManagerRepository.findMaxRnoByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
		Integer newRno = maxRno + 1;

		if (maxRno > 0) {
			taxManagerRepository.updateNewFlgToZero(jichitaiCd, shiteiNo);
			log.debug("既存レコードの最新フラグを0に更新: shiteiNo={}", shiteiNo);
		}

		TaxManager newEntity = new TaxManager();
		newEntity.setJichitaiCd(jichitaiCd);
		newEntity.setShiteiNo(shiteiNo);
		newEntity.setRno(newRno);
		newEntity.setKbn(form.getKbn());
		newEntity.setTorokuYmd(form.getRegistrationDate());
		newEntity.setShinkokuYmd(form.getDeclarationDate());

		if (!isExemption) {
			newEntity.setAtenaNo(form.getAtenaNo());
			newEntity.setName(form.getManagerName());
			newEntity.setNameKana(form.getManagerNameKana());
			newEntity.setYubinNo(form.getManagerYubinNo());
			newEntity.setJusho(form.getManagerAddress());
			newEntity.setTel(form.getManagerPhone());
		} else {
			newEntity.setAtenaNo(null);
			newEntity.setName(null);
			newEntity.setNameKana(null);
			newEntity.setYubinNo(null);
			newEntity.setJusho(null);
			newEntity.setTel(null);
		}
		newEntity.setRiyu(form.getReason());
		newEntity.setNewFlg(FLG_ON);
		newEntity.setDelFlg(FLG_OFF);

		taxManagerRepository.save(newEntity);
		log.debug("納税管理人履歴保存完了: shiteiNo={}, rno={}, 種別={}", shiteiNo, newRno, maxRno > 0 ? "更新" : "新規登録");
	}

	@Override
	@Transactional
	public boolean deleteByShiteiNo(String shiteiNo) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		log.debug("納税管理人削除処理開始: shiteiNo={}", shiteiNo);

		TaxManager currentRecord = taxManagerRepository.findLatestByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
				.orElseThrow(() -> new IllegalArgumentException("削除対象の納税管理人が見つかりません: " + shiteiNo));

		Integer currentRno = currentRecord.getRno();
		taxManagerRepository.updateDelFlgToOne(jichitaiCd, shiteiNo, currentRno);
		log.debug("現在レコードの削除フラグを更新: shiteiNo={}, rno={}", shiteiNo, currentRno);

		if (currentRno > 1) {
			taxManagerRepository.updateNewFlgToOneByRno(jichitaiCd, shiteiNo, currentRno - 1);
			log.debug("前履歴レコードの最新フラグを更新: shiteiNo={}, rno={}", shiteiNo, currentRno - 1);
		}

		log.debug("納税管理人削除完了: shiteiNo={}", shiteiNo);
		return currentRno > 1;
	}
}

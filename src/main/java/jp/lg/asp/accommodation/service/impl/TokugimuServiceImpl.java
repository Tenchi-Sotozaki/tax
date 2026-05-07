package jp.lg.asp.accommodation.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.TaxManagerForm;
import jp.lg.asp.accommodation.dto.TokugimuForm;
import jp.lg.asp.accommodation.dto.TokugimuListItem;
import jp.lg.asp.accommodation.dto.TokugimuSearchForm;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.GassanUchi;
import jp.lg.asp.accommodation.entity.Shoyusha;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.GassanUchiRepository;
import jp.lg.asp.accommodation.repository.ShoyushaRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.TokugimuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokugimuServiceImpl implements TokugimuService {

	private final TokugimuRepository tokugimuRepository;
	private final AtenaRepository atenaRepository;
	private final GassanUchiRepository gassanUchiRepository;
	private final ShoyushaRepository shoyushaRepository;

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	private String getCurrentUser() {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		return (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
	}

	@Override
	@Transactional(readOnly = true)
	public List<TokugimuListItem> search(TokugimuSearchForm form) {
		List<Tokugimu> tokugimuList = tokugimuRepository.findBySearchConditions(
				form.getShiteiNo(),
				form.getName(),
				form.getShisetsuName(),
				form.getKyokaShu(),
				form.getKojinNo(),
				form.getHojinNo());

		if (tokugimuList.isEmpty()) {
			return List.of();
		}

		List<BigDecimal> atenaNos = tokugimuList.stream().map(Tokugimu::getAtenaNo).toList();
		List<String> shiteiNos = tokugimuList.stream().map(Tokugimu::getShiteiNo).toList();

		Map<BigDecimal, Atena> atenaMap = atenaRepository.findByJichitaiCdAndAtenaNoIn(jichitaiCd, atenaNos)
				.stream().collect(Collectors.toMap(Atena::getAtenaNo, a -> a));

		Map<String, Boolean> gassanMap = gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(jichitaiCd, shiteiNos)
				.stream().collect(Collectors.toMap(GassanUchi::getShiteiNo, g -> true, (a, b) -> a));

		return tokugimuList.stream()
				.map(t -> {
					Atena atena = atenaMap.get(t.getAtenaNo());
					boolean isGassanTarget = gassanMap.containsKey(t.getShiteiNo());
					String status = t.getStatus();

					if (form.getStatus() != null && !"4".equals(form.getStatus()) && !form.getStatus().equals(status)) {
						return null;
					}
					if (form.getGasanTaisho() != null && !"999".equals(form.getGasanTaisho())) {
						boolean shouldBeTarget = "2".equals(form.getGasanTaisho());
						if (shouldBeTarget != isGassanTarget)
							return null;
					}

					return new TokugimuListItem(
							t.getAtenaNo().longValue(),
							t.getShiteiNo(),
							atena != null ? atena.getName() : t.getKyokaName(),
							t.getShisetsuName(),
							t.getKyokaShu(),
							getBusinessTypeLabel(t.getKyokaShu()),
							isGassanTarget ? "target" : "non-target",
							status,
							atena != null ? atena.getKojinNo() : null,
							atena != null ? atena.getHojinNo() : null);
				})
				.filter(item -> item != null)
				.toList();
	}

	private String getBusinessTypeLabel(String kyokaShu) {
		return switch (kyokaShu != null ? kyokaShu : "") {
		case "1" -> "ホテル";
		case "2" -> "旅館";
		case "3" -> "簡易宿所";
		case "4" -> "民泊";
		default -> "";
		};
	}

	/**
	 * 指定番号（shiteiNo）で1件取得してフォームに変換する
	 */
	@Override
	@Transactional(readOnly = true)
	public TokugimuForm getTokugimuByShiteiNo(String shiteiNo) {
		// 1. 指定番号から Tokugimu（施設情報）を取得
		Tokugimu t = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
				.stream().findFirst()
				.orElseThrow(() -> new RuntimeException("宿泊施設が見つかりません: " + shiteiNo));

		// 2. 宛名番号から Atena（事業者情報）を取得
		Atena atena = atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, t.getAtenaNo())
				.orElseThrow(() -> new RuntimeException("特別徴収義務者が見つかりません: " + t.getAtenaNo()));

		TokugimuForm form = new TokugimuForm();
		form.setAtenaNo(t.getAtenaNo().longValue());
		form.setShiteiNo(shiteiNo);
		form.setRegistrationDate(t.getTorokuYmd());

		// EntityからFormへのマッピングロジック
		mapEntityToForm(form, t, atena);

		return form;
	}

	@Override
	public String getTokugimuName(String obligorId) {
		try {
			BigDecimal atenaNo = BigDecimal.valueOf(Long.parseLong(obligorId));
			Atena atena = atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, atenaNo).orElse(null);
			return atena != null ? atena.getName() : "不明";
		} catch (NumberFormatException e) {
			log.warn("無効なID形式: {}", obligorId);
			return "不明";
		}
	}

	/**
	 * 納税管理人フォームの初期値を生成（指定番号ベース）
	 */
	@Override
	@Transactional(readOnly = true)
	public TaxManagerForm buildTaxManagerFormByShiteiNo(String shiteiNo) {
		TokugimuForm tokugimu = getTokugimuByShiteiNo(shiteiNo);
		TaxManagerForm form = new TaxManagerForm();
		// 指定番号ベースで初期値をセット
		form.setObligorName(tokugimu.getName());
		return form;
	}

	private String generateShiteiNo() {
		int max = tokugimuRepository.findMaxShiteiNoByJichitaiCd(jichitaiCd).orElse(0);
		return String.format("%08d", max + 1);
	}

	@Override
	@Transactional
	public void register(TokugimuForm form) {
		LocalDateTime now = LocalDateTime.now();
		String systemUser = getCurrentUser();

		if (form.getAtenaNo() == null) {
			throw new IllegalArgumentException("宛名番号が指定されていません。");
		}
		BigDecimal atenaNo = BigDecimal.valueOf(form.getAtenaNo());
		atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, atenaNo)
				.orElseThrow(() -> new IllegalArgumentException("指定された宛名番号が見つかりません: " + form.getAtenaNo()));

		String shiteiNo = generateShiteiNo();

		Tokugimu t = new Tokugimu();
		t.setJichitaiCd(jichitaiCd);
		t.setShiteiNo(shiteiNo);
		t.setAtenaNo(atenaNo);
		t.setRno(BigDecimal.ONE);
		t.setTorokuYmd(form.getRegistrationDate());
		t.setShinkokuYmd(form.getRegistrationDate());
		t.setHenkoYmd(form.getRegistrationDate());
		applyFormToTokugimu(t, form);
		t.setNewFlg("1");
		t.setDelFlg("0");
		t.setAddDt(now);
		t.setAddUser(systemUser);
		t.setUpdDt(now);
		t.setUpdUser(systemUser);
		t.setVersion(BigDecimal.ONE);
		tokugimuRepository.save(t);

		saveShoyusha(shiteiNo, BigDecimal.ONE, form, now, systemUser);

		log.info("特別徴収義務者登録完了: shiteiNo={}", shiteiNo);
	}

	/**
	 * 指定番号（shiteiNo）をキーに更新処理を行う
	 */
	@Override
	@Transactional
	public void updateByShiteiNo(String shiteiNo, TokugimuForm form) {
		LocalDateTime now = LocalDateTime.now();
		String systemUser = getCurrentUser();

		// 1. Tokugimuを取得
		Tokugimu t = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
				.stream().findFirst()
				.orElseThrow(() -> new RuntimeException("特別徴収義務者が見つかりません: " + shiteiNo));

		// 2. Atena（事業者情報）を更新
		Atena atena = atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, t.getAtenaNo())
				.orElseThrow(() -> new RuntimeException("宛名情報が見つかりません"));
		atena.setName(form.getName());
		atena.setJusho(form.getTokugimuAddress());
		atena.setTel1(form.getTokugimuPhone());
		atena.setUpdDt(now);
		atena.setUpdUser(systemUser);
		atenaRepository.save(atena);

		// 3. Tokugimu（宿泊施設情報）を更新
		t.setHenkoYmd(form.getRegistrationDate());
		applyFormToTokugimu(t, form);
		t.setUpdDt(now);
		t.setUpdUser(systemUser);
		tokugimuRepository.save(t);

		// 4. 所有者情報の更新
		shoyushaRepository.deleteByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
		saveShoyusha(shiteiNo, t.getRno(), form, now, systemUser);

		log.info("特別徴収義務者更新完了: shiteiNo={}, name={}", shiteiNo, form.getName());
	}

	/**
	 * 指定番号（shiteiNo）をキーに論理削除を行う
	 */
	@Override
	@Transactional
	public void deleteByShiteiNo(String shiteiNo) {
		Tokugimu t = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
				.stream().findFirst()
				.orElseThrow(() -> new RuntimeException("削除対象が見つかりません: " + shiteiNo));

		t.setDelFlg("1");
		t.setUpdDt(LocalDateTime.now());
		t.setUpdUser(getCurrentUser());
		tokugimuRepository.save(t);
		log.info("特別徴収義務者論理削除完了: shiteiNo={}", shiteiNo);
	}

	/**
	 * 納税管理人の保存（指定番号ベース）
	 */
	@Override
	@Transactional
	public void saveTaxManagerByShiteiNo(String shiteiNo, TaxManagerForm form) {
		// 指定番号から宛名IDを特定し、会社単位で管理人を保存するロジック（後ほど実装）
		log.info("納税管理人保存（未実装）: shiteiNo={}, manager={}", shiteiNo, form.getManagerName());
	}

	// ========== ヘルパーメソッド ==========

	private void mapEntityToForm(TokugimuForm form, Tokugimu t, Atena atena) {
		// 事業者情報
		form.setName(atena.getName());
		form.setTokugimuAddress(atena.getJusho());
		form.setTokugimuPhone(atena.getTel1());
		form.setPersonalNumber(atena.getKojinNo());
		form.setCorporateNumber(atena.getHojinNo());

		// 施設情報
		form.setFacilityAddressNo(t.getShisetsuYubinNo());
		form.setFacilityAddress(t.getShisetsuJusho());
		form.setFacilityName(t.getShisetsuName());
		form.setFacilityNameKana(t.getShisetsuNameKana());
		form.setFacilityPhone(t.getShisetsuTel());
		form.setFloorArea(t.getYukaMenseki());
		form.setAboveGroundFloor(t.getChijoKai() != null ? t.getChijoKai().toPlainString() : null);
		form.setBasementFloor(t.getChikaKai() != null ? t.getChikaKai().toPlainString() : null);
		form.setRoomCount(t.getKyakushitsuSu() != null ? t.getKyakushitsuSu().intValue() : null);
		form.setCapacity(t.getShuyoSu() != null ? t.getShuyoSu().intValue() : null);
		form.setBusinessStartDate(t.getEigyoStYmd());

		// 営業許可・送付先・その他
		form.setLicenseAddressNo(t.getKyokaYubinNo());
		form.setLicenseAddress(t.getKyokaJusho());
		form.setLicenseName(t.getKyokaName());
		form.setLicenseNameKana(t.getKyokaNameKana());
		form.setLicensePhone(t.getKyokaTel());
		form.setBusinessType(t.getKyokaShu());
		form.setLicenseNumber(t.getKyokaNo());
		form.setMailAddressNo(t.getSoufusakiYubinNo());
		form.setMailAddress(t.getSoufusakiJusho());
		form.setMailName(t.getSoufusakiName());
		form.setMailNameKana(t.getSoufusakiNameKana());
		form.setMailPhone(t.getSoufusakiTel());
		form.setEltaxApplication(t.getEltaxUmu());
		form.setTaxCycle(t.getNokigen());
		form.setRemarks(t.getBiko());

		// 所有者情報
		shoyushaRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, t.getShiteiNo())
				.stream().findFirst().ifPresent(s -> {
					form.setOwnerName(s.getShoyushaName());
					form.setOwnerNameKana(s.getShoyushaNameKana());
					form.setOwnerAddressNo(s.getShoyushaYubinNo());
					form.setOwnerAddress(s.getShoyushaJusho());
					form.setOwnerPhone(s.getShoyushaTel());
				});

		// 状態判定
		form.setSuspensionStartDate(t.getKyushiStYmd());
		form.setSuspensionEndDate(t.getKyushiEdYmd());
		form.setResumptionOrAbolitionDate(t.getEigyoEdYmd());
		form.setSuspensionOrAbolitionReason(t.getKyuhaishiRiyu());
	}

	private void applyFormToTokugimu(Tokugimu t, TokugimuForm form) {
		t.setShisetsuYubinNo(form.getFacilityAddressNo());
		t.setShisetsuJusho(form.getFacilityAddress());
		t.setShisetsuName(form.getFacilityName());
		t.setShisetsuNameKana(form.getFacilityNameKana());
		t.setShisetsuTel(form.getFacilityPhone());
		t.setYukaMenseki(form.getFloorArea());
		t.setChijoKai(form.getAboveGroundFloor() != null && !form.getAboveGroundFloor().isBlank()
				? new BigDecimal(form.getAboveGroundFloor()) : null);
		t.setChikaKai(form.getBasementFloor() != null && !form.getBasementFloor().isBlank()
				? new BigDecimal(form.getBasementFloor()) : null);
		t.setKyakushitsuSu(form.getRoomCount() != null ? BigDecimal.valueOf(form.getRoomCount()) : null);
		t.setShuyoSu(form.getCapacity() != null ? BigDecimal.valueOf(form.getCapacity()) : null);
		t.setEigyoStYmd(form.getBusinessStartDate());
		t.setKyokaYubinNo(form.getLicenseAddressNo());
		t.setKyokaJusho(form.getLicenseAddress());
		t.setKyokaName(form.getLicenseName());
		t.setKyokaNameKana(form.getLicenseNameKana());
		t.setKyokaTel(form.getLicensePhone());
		t.setKyokaShu(form.getBusinessType());
		t.setKyokaNo(form.getLicenseNumber());
		t.setSoufusakiYubinNo(form.getMailAddressNo());
		t.setSoufusakiJusho(form.getMailAddress());
		t.setSoufusakiName(form.getMailName());
		t.setSoufusakiNameKana(form.getMailNameKana());
		t.setSoufusakiTel(form.getMailPhone());
		t.setEltaxUmu(form.getEltaxApplication());
		t.setNokigen(form.getTaxCycle());
		t.setBiko(form.getRemarks());
		t.setKyushiStYmd(form.getSuspensionStartDate());
		t.setKyushiEdYmd(form.getSuspensionEndDate());
		t.setEigyoEdYmd(form.getResumptionOrAbolitionDate());
		t.setKyuhaishiRiyu(form.getSuspensionOrAbolitionReason());
	}

	private void saveShoyusha(String shiteiNo, BigDecimal rno, TokugimuForm form, LocalDateTime now, String user) {
		Shoyusha s = new Shoyusha();
		s.setJichitaiCd(jichitaiCd);
		s.setShiteiNo(shiteiNo);
		s.setRno(rno);
		s.setIdx(BigDecimal.ONE);
		s.setShoyushaName(form.getOwnerName());
		s.setShoyushaNameKana(form.getOwnerNameKana());
		s.setShoyushaYubinNo(form.getOwnerAddressNo());
		s.setShoyushaJusho(form.getOwnerAddress());
		s.setShoyushaTel(form.getOwnerPhone());
		s.setAddDt(now);
		s.setAssUser(user);
		s.setUpdDt(now);
		s.setUpdUser(user);
		s.setVersion(BigDecimal.ONE);
		shoyushaRepository.save(s);
	}
	
	@Override
	@Transactional(readOnly = true)
	public String getShiteiNoById(Long id) {
		BigDecimal atenaNo = BigDecimal.valueOf(id);
		return tokugimuRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, atenaNo)
				.stream().findFirst()
				.map(Tokugimu::getShiteiNo)
				.orElseThrow(() -> new RuntimeException("指定番号が見つかりません: " + id));
	}
}
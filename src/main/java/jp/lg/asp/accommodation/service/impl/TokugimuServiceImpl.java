package jp.lg.asp.accommodation.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.KyodoJigyoshaDto;
import jp.lg.asp.accommodation.dto.TokugimuForm;
import jp.lg.asp.accommodation.dto.TokugimuListItem;
import jp.lg.asp.accommodation.dto.TokugimuSearchForm;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.Gassan;
import jp.lg.asp.accommodation.entity.GassanUchi;
import jp.lg.asp.accommodation.entity.KyodoJigyosha;
import jp.lg.asp.accommodation.entity.Shoyusha;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.GassanRepository;
import jp.lg.asp.accommodation.repository.GassanUchiRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.KyodoJigyoshaRepository;
import jp.lg.asp.accommodation.repository.ShoyushaRepository;
import jp.lg.asp.accommodation.repository.ShunoRirekiRepository;
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
	private final GassanRepository gassanRepository;
	private final GassanUchiRepository gassanUchiRepository;
	private final ShoyushaRepository shoyushaRepository;
	private final KyodoJigyoshaRepository kyodoJigyoshaRepository;
	private final JichitaiRepository jichitaiRepository;
	private final FukaRepository fukaRepository;
	private final ShunoRirekiRepository shunoRirekiRepository;

	private final JichitaiContext jichitaiContext;

	private String getCurrentUser() {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		return (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
	}

	@Override
	@Transactional(readOnly = true)
	public List<TokugimuListItem> searchAll(TokugimuSearchForm form) {
		// page/pageSizeを無視して全件取得するためformを複製
		TokugimuSearchForm all = new TokugimuSearchForm();
		all.setShiteiNo(form.getShiteiNo());
		all.setName(form.getName());
		all.setNameMatchType(form.getNameMatchType());
		all.setShisetsuName(form.getShisetsuName());
		all.setShisetsuNameMatchType(form.getShisetsuNameMatchType());
		all.setKyokaShu(form.getKyokaShu());
		all.setGasanTaisho(form.getGasanTaisho());
		all.setStatus(form.getStatus());
		all.setKojinNo(form.getKojinNo());
		all.setHojinNo(form.getHojinNo());
		all.setPage(0);
		all.setPageSize(Integer.MAX_VALUE);
		return search(all).getContent();
	}

	@Override
	@Transactional(readOnly = true)
	public Page<TokugimuListItem> search(TokugimuSearchForm form) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();

		// 指定した件数ごとにページを切り替える
		PageRequest pageable = PageRequest.of(form.getPage(), form.getPageSize());

		// 初期遷移時（検索条件がすべて空）は全件取得
		List<Tokugimu> tokugimuList;
		if (isEmptySearchForm(form)) {
			tokugimuList = tokugimuRepository.findAllByJichitaiCd(jichitaiCd);
		} else if (form.getShiteiNo() != null && isGassanShiteiNo(form.getShiteiNo())) {
			// 合算指定番号プレフィックスで始まる場合、t_gassanから検索
			tokugimuList = findTokugimuByGassanShiteiNo(form.getShiteiNo());
		} else {
			tokugimuList = tokugimuRepository.findBySearchConditions(
					jichitaiCd,
					form.getShiteiNo(),
					form.getName(),
					toLikePattern(form.getName(), form.getNameMatchType()),
					form.getShisetsuName(),
					toLikePattern(form.getShisetsuName(), form.getShisetsuNameMatchType()),
					form.getKyokaShu(),
					form.getKojinNo(),
					form.getHojinNo());
		}

		if (tokugimuList.isEmpty()) {
			return Page.empty(pageable);
		}

		List<BigDecimal> atenaNos = tokugimuList.stream().map(Tokugimu::getAtenaNo).toList();
		List<String> shiteiNos = tokugimuList.stream().map(Tokugimu::getShiteiNo).toList();

		Map<BigDecimal, Atena> atenaMap = atenaRepository.findByJichitaiCdAndAtenaNoIn(jichitaiCd, atenaNos)
				.stream().collect(Collectors.toMap(Atena::getAtenaNo, a -> a));

		// 指定番号 -> 合算指定番号。合算対象かどうかの判定にも利用する
		Map<String, String> gassanMap = gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(jichitaiCd, shiteiNos, null)
				.stream().collect(Collectors.toMap(GassanUchi::getShiteiNo,
						GassanUchi::getGassanShiteiNo, (a, b) -> a));

		List<TokugimuListItem> allItems = tokugimuList.stream()
				.map(t -> {
					Atena atena = atenaMap.get(t.getAtenaNo());
					boolean isGassanTarget = gassanMap.containsKey(t.getShiteiNo());
					String status = determineStatus(t);

					// ステータスフィルタリング（初期遷移時はフィルタリングなし）
					if (form.getStatus() != null && !form.getStatus().isEmpty() && !"999".equals(form.getStatus())
							&& !form.getStatus().equals(status)) {
						return null;
					}

					// 合算対象フィルタリング（初期遷移時はフィルタリングなし）
					if (form.getGasanTaisho() != null && !form.getGasanTaisho().isEmpty()
							&& !"999".equals(form.getGasanTaisho())) {
						boolean shouldBeTarget = "2".equals(form.getGasanTaisho());
						if (shouldBeTarget != isGassanTarget)
							return null;
					}

					TokugimuListItem item = new TokugimuListItem(
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
					item.setGassanShiteiNo(gassanMap.get(t.getShiteiNo()));
					return item;
				})
				.filter(item -> item != null)
				.toList();

		int start = (int) pageable.getOffset();
		int end = Math.min(start + pageable.getPageSize(), allItems.size());
		List<TokugimuListItem> pageContent = start >= allItems.size() ? List.of() : allItems.subList(start, end);

		// 最終申告日・納付状況は表示するページ分だけ問い合わせる
		applyLastDeclarationInfo(jichitaiCd, pageContent);

		return new PageImpl<>(pageContent, pageable, allItems.size());
	}

	/**
	 * 一覧に表示する行へ、最終申告日と最終申告分の納付状況を設定する。
	 * <p>
	 * 「最終申告」は申告日（shinkoku_ymd）が最も新しいレコードとする。
	 * 納付状況はその年度・期別の納入額合計と税額を突き合わせて判定し、
	 * 申告実績が無い場合はいずれも未設定（画面では空欄と「-」）とする。
	 * 行数分の問い合わせが発生しないよう、指定番号をまとめて取得する。
	 *
	 * @param jichitaiCd 自治体コード
	 * @param items 対象行（表示するページ分）
	 */
	private void applyLastDeclarationInfo(String jichitaiCd, List<TokugimuListItem> items) {
		if (items.isEmpty()) {
			return;
		}
		List<String> shiteiNos = items.stream().map(TokugimuListItem::getShiteiNo).distinct().toList();

		// 指定番号ごとの最終申告レコード（申告日の降順で先に来たものを採用）
		Map<String, Fuka> lastFukaMap = new HashMap<>();
		for (Fuka fuka : fukaRepository.findDeclaredByShiteiNoInOrderByShinkokuYmdDesc(jichitaiCd, shiteiNos)) {
			lastFukaMap.putIfAbsent(fuka.getShiteiNo(), fuka);
		}
		if (lastFukaMap.isEmpty()) {
			return;
		}

		// 指定番号・年度・期別ごとの納入額合計
		Map<String, Long> nonyuMap = new HashMap<>();
		for (Object[] row : shunoRirekiRepository.sumNonyugakuByShiteiNoIn(jichitaiCd, shiteiNos)) {
			nonyuMap.put(nonyuKey((String) row[0], (String) row[1], (Integer) row[2]),
					((Number) row[3]).longValue());
		}

		for (TokugimuListItem item : items) {
			Fuka fuka = lastFukaMap.get(item.getShiteiNo());
			if (fuka == null) {
				continue;
			}
			item.setLastShinkokuYmd(fuka.getShinkokuYmd());
			long zeigaku = fuka.getTotalZeigaku() != null ? fuka.getTotalZeigaku() : 0L;
			long nonyu = nonyuMap.getOrDefault(
					nonyuKey(fuka.getShiteiNo(), fuka.getNendo(), fuka.getKibetsu()), 0L);
			item.setLastNonyuStatus(determineNonyuStatus(zeigaku, nonyu));
		}
	}

	/** 納入額集計マップのキーを組み立てる。 */
	private String nonyuKey(String shiteiNo, String nendo, Integer kibetsu) {
		return shiteiNo + "|" + nendo + "|" + kibetsu;
	}

	/**
	 * 税額と納入額合計から納付状況を判定する。
	 *
	 * @param zeigaku 税額
	 * @param nonyugaku 納入額合計
	 * @return paid=完納 / partial=一部納付 / unpaid=未納
	 */
	private String determineNonyuStatus(long zeigaku, long nonyugaku) {
		long remaining = zeigaku - nonyugaku;
		if (remaining <= 0) {
			return "paid";
		}
		return remaining < zeigaku ? "partial" : "unpaid";
	}

	private List<Tokugimu> findTokugimuByGassanShiteiNo(String gassanShiteiNo) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		List<Gassan> gassanList = gassanRepository.findByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo);
		if (gassanList.isEmpty()) {
			return List.of();
		}
		List<String> shiteiNos = gassanUchiRepository
				.findByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo)
				.stream().map(GassanUchi::getShiteiNo).toList();
		if (shiteiNos.isEmpty()) {
			return List.of();
		}
		return shiteiNos.stream()
				.flatMap(sn -> tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, sn).stream())
				.toList();
	}

	private boolean isGassanShiteiNo(String shiteiNo) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		String gassanPrefix = jichitaiRepository.findById(jichitaiCd)
				.map(j -> j.getGassanStChar() != null ? j.getGassanStChar() : "900")
				.orElse("900");
		return shiteiNo.startsWith(gassanPrefix);
	}

	private String toLikePattern(String value, String matchType) {
		if (value == null || value.isBlank())
			return null;
		return switch (matchType) {
		case "prefix" -> value + "%";
		case "exact" -> value;
		default -> "%" + value + "%"; // partial
		};
	}

	private boolean isEmptySearchForm(TokugimuSearchForm form) {
		if (form == null) {
			return true;
		}
		return (form.getShiteiNo() == null || form.getShiteiNo().isEmpty()) &&
				(form.getName() == null || form.getName().isEmpty()) &&
				(form.getShisetsuName() == null || form.getShisetsuName().isEmpty()) &&
				(form.getKyokaShu() == null || form.getKyokaShu().isEmpty() || "999".equals(form.getKyokaShu())) &&
				(form.getGasanTaisho() == null || form.getGasanTaisho().isEmpty()
						|| "999".equals(form.getGasanTaisho()))
				&&
				(form.getStatus() == null || form.getStatus().isEmpty() || "999".equals(form.getStatus())) &&
				(form.getKojinNo() == null || form.getKojinNo().isEmpty()) &&
				(form.getHojinNo() == null || form.getHojinNo().isEmpty());
	}

	private String determineStatus(Tokugimu t) {
		// 廃止日が設定されている場合は廃止
		if (t.getEigyoEdYmd() != null) {
			return "3"; // 廃止
		}
		// 休止期間が設定されている場合は休止
		if (t.getKyushiStYmd() != null && t.getKyushiEdYmd() != null) {
			return "2"; // 休止
		}
		// それ以外は営業中
		return "1"; // 営業中
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
		String jichitaiCd = jichitaiContext.getJichitaiCd();
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
		form.setShinseiDate(t.getShinkokuYmd());
		form.setHenkoDate(t.getHenkoYmd());
		form.setRno(t.getRno().intValue());
		form.setMaxRno(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo).orElse(1));
		form.setMinRno(tokugimuRepository.findMinRnoByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo).orElse(1));

		// EntityからFormへのマッピングロジック
		mapEntityToForm(form, t, atena);

		return form;
	}

	@Override
	@Transactional(readOnly = true)
	public TokugimuForm getTokugimuByShiteiNoAndRno(String shiteiNo, int rno) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		Tokugimu t = tokugimuRepository.findByJichitaiCdAndShiteiNoAndRno(
				jichitaiCd, shiteiNo, BigDecimal.valueOf(rno))
				.orElseThrow(() -> new RuntimeException("宿泊施設が見つかりません: " + shiteiNo + "/rno=" + rno));

		Atena atena = atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, t.getAtenaNo())
				.orElseThrow(() -> new RuntimeException("特別徴収義務者が見つかりません: " + t.getAtenaNo()));

		TokugimuForm form = new TokugimuForm();
		form.setAtenaNo(t.getAtenaNo().longValue());
		form.setShiteiNo(shiteiNo);
		form.setRegistrationDate(t.getTorokuYmd());
		form.setShinseiDate(t.getShinkokuYmd());
		form.setHenkoDate(t.getHenkoYmd());
		form.setRno(t.getRno().intValue());
		form.setMaxRno(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo).orElse(1));
		form.setMinRno(tokugimuRepository.findMinRnoByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo).orElse(1));

		mapEntityToForm(form, t, atena);

		return form;
	}

	private String generateShiteiNo() {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		String prefix = jichitaiRepository.findById(jichitaiCd)
				.map(j -> j.getShiteiStChar() != null ? j.getShiteiStChar() : "000")
				.orElse("000");
		int max = tokugimuRepository.findMaxShiteiNoByJichitaiCdAndPrefix(jichitaiCd, prefix).orElse(0);
		return prefix + String.format("%05d", max + 1);
	}

	@Override
	@Transactional
	public String register(TokugimuForm form) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
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
		tokugimuRepository.save(t);

		saveShoyusha(shiteiNo, BigDecimal.ONE, form, now, systemUser);
		saveKyodoJigyosha(shiteiNo, BigDecimal.ONE, form);

		log.debug("特別徴収義務者登録完了: shiteiNo={}", shiteiNo);
		return shiteiNo;
	}

	/**
	 * 指定番号（shiteiNo）をキーに更新処理を行う
	 */
	@Override
	@Transactional
	public void updateByShiteiNo(String shiteiNo, TokugimuForm form) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		LocalDateTime now = LocalDateTime.now();
		String systemUser = getCurrentUser();

		// 1. 既存レコードを取得し、new_flgを0にする
		Tokugimu old = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
				.stream().findFirst()
				.orElseThrow(() -> new RuntimeException("特別徴収義務者が見つかりません: " + shiteiNo));
		old.setNewFlg("0");
		tokugimuRepository.save(old);

		// 2. rno+1で新レコードをINSERT
		BigDecimal newRno = BigDecimal.valueOf(
				tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo).orElse(0) + 1);

		Tokugimu t = new Tokugimu();
		t.setJichitaiCd(jichitaiCd);
		t.setShiteiNo(shiteiNo);
		t.setRno(newRno);
		t.setAtenaNo(BigDecimal.valueOf(form.getAtenaNo()));
		t.setTorokuYmd(old.getTorokuYmd());
		t.setShinkokuYmd(old.getShinkokuYmd());
		t.setHenkoYmd(form.getRegistrationDate());
		applyFormToTokugimu(t, form);
		t.setNewFlg("1");
		t.setDelFlg("0");
		tokugimuRepository.save(t);

		// 4. 所有者情報の追加
		saveShoyusha(shiteiNo, newRno, form, now, systemUser);

		// 5. 共同事業者情報の追加
		saveKyodoJigyosha(shiteiNo, newRno, form);

		log.debug("特別徴収義務者更新完了: shiteiNo={}, rno={}", shiteiNo, newRno);
	}

	/**
	 * 指定番号（shiteiNo）をキーに論理削除を行う
	 */
	@Override
	@Transactional
	public boolean deleteByShiteiNo(String shiteiNo) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		Tokugimu t = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
				.stream().findFirst()
				.orElseThrow(() -> new RuntimeException("削除対象が見つかりません: " + shiteiNo));

		t.setDelFlg("1");
		t.setNewFlg("0");
		tokugimuRepository.save(t);

		// 履歴が残っている場合は、残っている中で最も新しいものを最新版に戻す。
		// これをしないと new_flg='1' のレコードが無くなり、一覧・照会のいずれからも参照できなくなる。
		Tokugimu latest = tokugimuRepository
				.findActiveHistoryByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
				.stream().findFirst().orElse(null);
		if (latest == null) {
			log.debug("特別徴収義務者論理削除完了（履歴なし）: shiteiNo={}", shiteiNo);
			return false;
		}
		latest.setNewFlg("1");
		tokugimuRepository.save(latest);
		log.debug("特別徴収義務者論理削除完了（最新履歴を rno={} に戻す）: shiteiNo={}", latest.getRno(), shiteiNo);
		return true;
	}

	// ========== ヘルパーメソッド ==========

	private void mapEntityToForm(TokugimuForm form, Tokugimu t, Atena atena) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		// 事業者情報
		form.setTokugimuAddressNo(atena.getYubinNo());
		form.setTokugimuAddress(atena.getJusho());
		form.setName(atena.getName());
		form.setNameKana(atena.getNameKana());
		form.setTokugimuPhone(atena.getTel1());
		form.setPersonalNumber(atena.getKojinNo());
		form.setCorporateNumber(atena.getHojinNo());

		// 施設情報
		form.setFacilityAddressNo(t.getShisetsuYubinNo());
		form.setFacilityAddress(t.getShisetsuJusho());
		form.setFacilityName(t.getShisetsuName());
		form.setFacilityNameKana(t.getShisetsuNameKana());
		form.setFacilityPhone(t.getShisetsuTel());
		form.setFloorArea(t.getYukaMenseki() != null ? t.getYukaMenseki().toPlainString() : null);
		form.setAboveGroundFloor(t.getChijoKai() != null ? t.getChijoKai().toPlainString() : null);
		form.setBasementFloor(t.getChikaKai() != null ? t.getChikaKai().toPlainString() : null);
		form.setRoomCount(t.getKyakushitsuSu() != null ? t.getKyakushitsuSu().toPlainString() : null);
		form.setCapacity(t.getShuyoSu() != null ? t.getShuyoSu().toPlainString() : null);
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
		form.setEltaxUmu(t.getEltaxUmu());
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

		// 共同事業者情報
		List<KyodoJigyosha> kyodoList = kyodoJigyoshaRepository.findByJichitaiCdAndShiteiNoAndRno(jichitaiCd,
				t.getShiteiNo(), t.getRno());
		if (!kyodoList.isEmpty()) {
			form.setKyodoList(kyodoList.stream().map(k -> {
				KyodoJigyoshaDto dto = new KyodoJigyoshaDto();
				dto.setKyodoName(k.getKyodoJigyoshaName());
				dto.setKyodoNameKana(k.getKyodoJigyoshaNameKana());
				dto.setKyodoAddressNo(k.getKyodoJigyoshaYubinNo());
				dto.setKyodoAddress(k.getKyodoJigyoshaJusho());
				dto.setKyodoPhone(k.getKyodoJigyoshaTel());
				return dto;
			}).toList());
		}

		// 状態判定
		form.setSuspensionStartDate(t.getKyushiStYmd());
		form.setSuspensionEndDate(t.getKyushiEdYmd());
		form.setResumptionOrAbolitionDate(t.getEigyoEdYmd());
		form.setSuspensionOrAbolitionReason(t.getKyuhaishiRiyu());
		form.setBusinessStatusFlg(
				t.getKyushiStYmd() != null || t.getKyushiEdYmd() != null
						|| t.getEigyoEdYmd() != null || t.getKyuhaishiRiyu() != null);
	}

	private void applyFormToTokugimu(Tokugimu t, TokugimuForm form) {
		t.setShisetsuYubinNo(form.getFacilityAddressNo());
		t.setShisetsuJusho(form.getFacilityAddress());
		t.setShisetsuName(form.getFacilityName());
		t.setShisetsuNameKana(form.getFacilityNameKana());
		t.setShisetsuTel(form.getFacilityPhone());
		t.setYukaMenseki(toDecimal(form.getFloorArea()));
		t.setChijoKai(toDecimal(form.getAboveGroundFloor()));
		t.setChikaKai(toDecimal(form.getBasementFloor()));
		t.setKyakushitsuSu(toDecimal(form.getRoomCount()));
		t.setShuyoSu(toDecimal(form.getCapacity()));
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
		t.setEltaxUmu(form.getEltaxUmu());
		t.setBiko(form.getRemarks());
		if (form.isBusinessStatusFlg()) {
			t.setKyushiStYmd(form.getSuspensionStartDate());
			t.setKyushiEdYmd(form.getSuspensionEndDate());
			t.setEigyoEdYmd(form.getResumptionOrAbolitionDate());
			t.setKyuhaishiRiyu(form.getSuspensionOrAbolitionReason());
		} else {
			t.setKyushiStYmd(null);
			t.setKyushiEdYmd(null);
			t.setEigyoEdYmd(null);
			t.setKyuhaishiRiyu(null);
		}
	}

	private void saveKyodoJigyosha(String shiteiNo, BigDecimal rno, TokugimuForm form) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		if (form.getKyodoList() == null)
			return;
		for (int i = 0; i < form.getKyodoList().size(); i++) {
			KyodoJigyoshaDto dto = form.getKyodoList().get(i);
			if (dto.getKyodoName() == null || dto.getKyodoName().isBlank())
				continue;
			KyodoJigyosha k = new KyodoJigyosha();
			k.setJichitaiCd(jichitaiCd);
			k.setShiteiNo(shiteiNo);
			k.setRno(rno);
			k.setIdx(BigDecimal.valueOf(i + 1));
			k.setKyodoJigyoshaName(dto.getKyodoName());
			k.setKyodoJigyoshaNameKana(dto.getKyodoNameKana());
			k.setKyodoJigyoshaYubinNo(dto.getKyodoAddressNo());
			k.setKyodoJigyoshaJusho(dto.getKyodoAddress());
			k.setKyodoJigyoshaTel(dto.getKyodoPhone());
			kyodoJigyoshaRepository.save(k);
		}
	}

	private void saveShoyusha(String shiteiNo, BigDecimal rno, TokugimuForm form, LocalDateTime now, String user) {
		if (!org.springframework.util.StringUtils.hasText(form.getOwnerName())
				&& !org.springframework.util.StringUtils.hasText(form.getOwnerNameKana())
				&& !org.springframework.util.StringUtils.hasText(form.getOwnerAddressNo())
				&& !org.springframework.util.StringUtils.hasText(form.getOwnerAddress())
				&& !org.springframework.util.StringUtils.hasText(form.getOwnerPhone()))
			return;
		String jichitaiCd = jichitaiContext.getJichitaiCd();
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
		shoyushaRepository.save(s);
	}

	@Override
	@Transactional(readOnly = true)
	public String getShiteiNoById(Long id) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		BigDecimal atenaNo = BigDecimal.valueOf(id);
		return tokugimuRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, atenaNo)
				.stream().findFirst()
				.map(Tokugimu::getShiteiNo)
				.orElseThrow(() -> new RuntimeException("指定番号が見つかりません: " + id));
	}

	/**
	 * 画面から受け取った数値項目を BigDecimal に変換する。
	 * 未入力は null。書式は Form 側の @Pattern で担保しているため、ここでは検査しない。
	 */
	private BigDecimal toDecimal(String value) {
		return value != null && !value.isBlank() ? new BigDecimal(value) : null;
	}
}
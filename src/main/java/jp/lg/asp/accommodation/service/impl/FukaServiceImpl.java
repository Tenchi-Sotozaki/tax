package jp.lg.asp.accommodation.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jp.lg.asp.accommodation.constant.FukaConstants;
import jp.lg.asp.accommodation.constant.ZeiritsuConstants;
import jp.lg.asp.accommodation.dto.FukaDaichoForm;
import jp.lg.asp.accommodation.dto.FukaDaichoListItem;
import jp.lg.asp.accommodation.dto.FukaDeclarationForm;
import jp.lg.asp.accommodation.dto.FukaMonthlyDeclarationDto;
import jp.lg.asp.accommodation.dto.FukaMonthlyTallyDto;
import jp.lg.asp.accommodation.dto.FukaMonthlyTallyDto.DailyItem;
import jp.lg.asp.accommodation.dto.FukaTaxDetailDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.ChoshuGenbo;
import jp.lg.asp.accommodation.entity.ChoshuGenboId;
import jp.lg.asp.accommodation.entity.ChoshuGenboUchi;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.FukaUchi;
import jp.lg.asp.accommodation.entity.Nokigen;
import jp.lg.asp.accommodation.entity.NokigenId;
import jp.lg.asp.accommodation.entity.NozeiShuki;
import jp.lg.asp.accommodation.entity.NozeiShukiId;
import jp.lg.asp.accommodation.entity.ShunoRireki;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.entity.Zeiritsu;
import jp.lg.asp.accommodation.entity.ZeiritsuTeigaku;
import jp.lg.asp.accommodation.entity.ZeiritsuTeiritsu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.ChoshuGenboRepository;
import jp.lg.asp.accommodation.repository.ChoshuGenboUchiRepository;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.FukaUchiRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.NokigenRepository;
import jp.lg.asp.accommodation.repository.NozeiShukiRepository;
import jp.lg.asp.accommodation.repository.ShunoRirekiRepository;
import jp.lg.asp.accommodation.repository.TekiyoNozeiShukiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeigakuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeiritsuRepository;
import jp.lg.asp.accommodation.service.FukaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 宿泊税納入（賦課）に関するビジネスロジックを担当するサービスクラス。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FukaServiceImpl implements FukaService {

	private final FukaRepository fukaRepository;
	private final TokugimuRepository tokugimuRepository;
	private final ZeiritsuRepository zeiritsuRepository;
	private final ZeiritsuTeigakuRepository zeiritsuTeigakuRepository;
	private final ZeiritsuTeiritsuRepository zeiritsuTeiritsuRepository;
	private final FukaUchiRepository fukaUchiRepository;
	private final ChoshuGenboRepository choshuGenboRepository;
	private final ChoshuGenboUchiRepository choshuGenboUchiRepository;
	private final AtenaRepository atenaRepository;
	private final NozeiShukiRepository nozeiShukiRepository;
	private final NokigenRepository nokigenRepository;
	private final JichitaiRepository jichitaiRepository;
	private final ShunoRirekiRepository shunoRirekiRepository;
	private final TekiyoNozeiShukiRepository tekiyoNozeiShukiRepository;

	// 定数定義（マジックナンバーの排除）
	private static final String STATUS_ALL = "999";
	private static final String STATUS_ZUMI = "1";
	private static final String STATUS_MI = "2";
	private static final int MAX_KIBETSU = 12;
	private static final int MAX_DAYS = 31;
	private static final String DEFAULT_NEW_FLG = "1";
	private static final String DEFAULT_DEL_FLG = "0";

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	/**
	 * 納入金額管理台帳のデータを取得する。
	 */
	@Transactional(readOnly = true)
	public FukaDaichoForm getDaichoData(String shiteiNo, String nendo, String status) {
		FukaDaichoForm form = new FukaDaichoForm();
		form.setShiteiNo(shiteiNo);
		form.setNendo(nendo);
		form.setStatus(status != null ? status : STATUS_ALL);

		List<Fuka> fukaList = fukaRepository
				.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(jichitaiCd, shiteiNo, nendo);
		Map<Integer, Fuka> fukaMap = createFukaMap(fukaList);

		int shuki = tekiyoNozeiShukiRepository.findLatestByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
				.stream().findFirst()
				.flatMap(t -> nozeiShukiRepository.findById(new NozeiShukiId(jichitaiCd, t.getSeq())))
				.map(n -> n.getShuki().intValue())
				.orElse(3);
		form.setShuki(shuki);
		int nendoStMonth = jichitaiRepository.findById(jichitaiCd)
				.map(j -> Integer.parseInt(j.getNendoStMonth().trim()))
				.orElse(3);
		Nokigen nokigen = nokigenRepository.findById(new NokigenId(jichitaiCd, nendo)).orElse(null);
		form.setItems(createDaichoItems(nendo, fukaMap, form.getStatus(), form.getShuki(), nokigen, nendoStMonth, shiteiNo));
		return form;
	}

	/**
	 * 賦課エンティティのリストを期別をキーにしたマップに変換する。
	 */
	private Map<Integer, Fuka> createFukaMap(List<Fuka> fukaList) {
		return fukaList.stream()
				.collect(Collectors.toMap(
						Fuka::getKibetsu,
						f -> f,
						(existing, replacement) -> existing.getRno() > replacement.getRno() ? existing : replacement));
	}

	/**
	 * 期別ごとの台帳明細行リストを作成する。
	 */
	private List<FukaDaichoListItem> createDaichoItems(String nendo, Map<Integer, Fuka> fukaMap, String filterStatus,
			Integer shuki, Nokigen nokigen, int nendoStMonth, String shiteiNo) {
		List<FukaDaichoListItem> items = new ArrayList<>();
		for (int i = 1; i <= MAX_KIBETSU; i++) {
			FukaDaichoListItem item = buildDaichoItem(nendo, i, fukaMap, shuki, nokigen, nendoStMonth, shiteiNo);

			if (STATUS_ZUMI.equals(filterStatus) && !item.isShinkokuZumi()) {
				continue;
			}
			if (STATUS_MI.equals(filterStatus) && item.isShinkokuZumi()) {
				continue;
			}
			item.setNendo(nendo); // 年度をセット
			item.setKibetsu(i); // 期別（月）をセット
			items.add(item);
		}
		return items;
	}

	/**
	 * 単一の台帳明細行を組み立てる。
	 */
	private FukaDaichoListItem buildDaichoItem(String nendo, int kibetsu, Map<Integer, Fuka> fukaMap, Integer shuki, Nokigen nokigen, int nendoStMonth, String shiteiNo) {
		FukaDaichoListItem item = new FukaDaichoListItem();
		item.setNendo(nendo);
		item.setKibetsu(kibetsu);

		item.setDisplayNengetsu(createTaishoYmLabel(nendo, kibetsu));
		item.setDisplayShinkokuKigen(createShinkokuKigenString(nendo, kibetsu, shuki));
		item.setDisplayNonyuKigen(createNonyuKigenString(nendo, kibetsu, shuki, nokigen, nendoStMonth));
		item.setTargetYearMonth(createTaishoYmString(nendo, kibetsu));

		if (fukaMap.containsKey(kibetsu)) {
			Fuka dbData = fukaMap.get(kibetsu);
			item.setAmount(dbData.getTotalZeigaku());
			item.setTotalZeigaku(dbData.getTotalZeigaku());
			item.setCityZeigaku(dbData.getCityZeigaku());
			item.setKenZeigaku(dbData.getKenZeigaku());
			item.setShinkokuYmd(dbData.getShinkokuYmd());
			item.setShinkokuZumi(true);
			// 納入状況判定
			long totalZeigaku = dbData.getTotalZeigaku() != null ? dbData.getTotalZeigaku() : 0L;
			long totalNonyu = shunoRirekiRepository.sumNonyugaku(jichitaiCd, shiteiNo, nendo, kibetsu);
			long remaining = totalZeigaku - totalNonyu;
			if (remaining <= 0) {
				item.setNonyuStatus("paid");
			} else if (remaining < totalZeigaku) {
				item.setNonyuStatus("partial");
			} else {
				item.setNonyuStatus("unpaid");
			}
		} else {
			item.setAmount(0L);
			item.setTotalZeigaku(0L);
			item.setCityZeigaku(0L);
			item.setKenZeigaku(0L);
			item.setShinkokuZumi(false);
			item.setNonyuStatus("unpaid");
		}
		return item;
	}

	/** 
	 * 年度と期別から対象年月のラベルを作成
	 * @param  nendo 年度
	 * @param kibetsu 期別
	 * @return 対象年月ラベル（例：2026年05月）
	 */
	private String createTaishoYmLabel(String nendo, int kibetsu) {
		int year = (kibetsu + 2) > MAX_KIBETSU ? Integer.parseInt(nendo) + 1 : Integer.parseInt(nendo);
		int month = (kibetsu + 2) > MAX_KIBETSU ? (kibetsu + 2) - MAX_KIBETSU : (kibetsu + 2);
		return String.valueOf(year) + "年" + String.valueOf(month) + "月";
	}

	/** 
	 * 年度と期別から対象年月を作成
	 * @param  nendo 年度
	 * @param kibetsu 期別
	 * @return 対象年月文字列（例：202605）
	 */
	private String createTaishoYmString(String nendo, int kibetsu) {
		int year = (kibetsu + 2) > MAX_KIBETSU ? Integer.parseInt(nendo) + 1 : Integer.parseInt(nendo);
		int month = (kibetsu + 2) > MAX_KIBETSU ? (kibetsu + 2) - MAX_KIBETSU : (kibetsu + 2);
		return String.valueOf(year) + String.format("%02d", month);
	}

	/** 
	 * 年度、期別、納税周期から申告期限を作成
	 * @param  nendo 年度
	 * @param kibetsu 期別
	 * @param shuki
	 * @return 申告期限（例：2026年6月末）
	 */
	private String createShinkokuKigenString(String nendo, int kibetsu, int shuki) {
		int year = (kibetsu + 2) > MAX_KIBETSU ? Integer.parseInt(nendo) + 1 : Integer.parseInt(nendo);
		int month = (kibetsu + 2) > MAX_KIBETSU ? (kibetsu + 2) - MAX_KIBETSU : (kibetsu + 2);
		// 申告期限は対象月の翌月末
		month += 1;
		if (month > 12) {
			year++;
			month -= 12;
		}
		return String.valueOf(year) + "年" + String.valueOf(month) + "月末";
	}

	/** 
	 * 年度、期別、納税周期、納期限マスタから納入期限を作成
	 * 申告期限の翌月のm_nokigenの納期限を表示する
	 */
	private String createNonyuKigenString(String nendo, int kibetsu, int shuki, Nokigen nokigen, int nendoStMonth) {
		// 申告期限月を算出（対象月+1）
		int year = (kibetsu + 2) > MAX_KIBETSU ? Integer.parseInt(nendo) + 1 : Integer.parseInt(nendo);
		int month = (kibetsu + 2) > MAX_KIBETSU ? (kibetsu + 2) - MAX_KIBETSU : (kibetsu + 2);
		int shinkokuMonth = month + 1;
		int shinkokuYear = year;
		if (shinkokuMonth > 12) {
			shinkokuYear++;
			shinkokuMonth -= 12;
		}
		// 納入期限月 = 申告期限の翌月
		int nonyuMonth = shinkokuMonth + 1;
		int nonyuYear = shinkokuYear;
		if (nonyuMonth > 12) {
			nonyuYear++;
			nonyuMonth -= 12;
		}

		if (nokigen != null) {
			// nendoStMonthを基準に何番目のフィールドかを算出
			int ordinal = (nonyuMonth - nendoStMonth + 12) % 12 + 1;
			String nokigenValue = getNokigenByOrdinal(nokigen, ordinal);
			if (nokigenValue != null && nokigenValue.length() == 8) {
				int y = Integer.parseInt(nokigenValue.substring(0, 4));
				int m = Integer.parseInt(nokigenValue.substring(4, 6));
				int d = Integer.parseInt(nokigenValue.substring(6, 8));
				return y + "年" + m + "月" + d + "日";
			}
		}
		// フォールバック
		return nonyuYear + "年" + nonyuMonth + "月末";
	}

	/**
	 * 年度開始月からの順番（1～12）に対応するNokigenの値を取得する
	 */
	private String getNokigenByOrdinal(Nokigen nokigen, int ordinal) {
		return switch (ordinal) {
			case 1 -> nokigen.getNokigen1st();
			case 2 -> nokigen.getNokigen2nd();
			case 3 -> nokigen.getNokigen3rd();
			case 4 -> nokigen.getNokigen4th();
			case 5 -> nokigen.getNokigen5th();
			case 6 -> nokigen.getNokigen6th();
			case 7 -> nokigen.getNokigen7th();
			case 8 -> nokigen.getNokigen8th();
			case 9 -> nokigen.getNokigen9th();
			case 10 -> nokigen.getNokigen10th();
			case 11 -> nokigen.getNokigen11th();
			case 12 -> nokigen.getNokigen12th();
			default -> null;
		};
	}

	/** 
	 * 対象年月から年度を取得
	 * @param  taishoYm 対象年月
	 * @return 年度
	 */
	private String calculateNendo(String taishoYm) {
		if (taishoYm.length() != 6) {
			return "";
		}
		int year = Integer.parseInt(taishoYm.substring(0, 4));
		int month = Integer.parseInt(taishoYm.substring(4, 6));
		return String.valueOf(month >= 3 ? year : year - 1);
	}

	/** 
	 * 対象年月から期別を取得
	 * @param  taishoYm 対象年月
	 * @return 期別
	 */
	private Integer calculateKibetsu(String taishoYm) {
		if (taishoYm.length() != 6) {
			return 0;
		}
		int month = Integer.parseInt(taishoYm.substring(4, 6));
		return month >= 3 ? month - 2 : month + 10;
	}

	/**
	 * 新規登録用の初期表示データを取得する。
	 */
	@Transactional(readOnly = true)
	public FukaDeclarationForm getDeclarationFormForRegister(String shiteiNo, String taishoYm) {
		FukaDeclarationForm form = new FukaDeclarationForm();
		form.setShiteiNo(shiteiNo);
		form.setTorokuDate(LocalDate.now());
		form.setShinkokuDate(LocalDate.now());

		form.setNendo(calculateNendo(taishoYm));
		form.setKibetsu(calculateKibetsu(taishoYm));
		form.setModificationCategory(FukaConstants.SHINKOKU.getValue());

		setupObligorInfo(form, shiteiNo);

		// =========================================================
		// 💡 【仕様書準拠】適用時期ベースの税体系判定ロジック
		// =========================================================

		// 対象年月に合致する親マスタをDBから取得し、市区町村用(taishoKbn="1")を抽出
		List<Zeiritsu> appliedList = zeiritsuRepository
				.findActiveByJichitaiCdAndTargetYm(jichitaiCd, ZeiritsuConstants.CITY.getValue(), taishoYm);
		Zeiritsu appliedZeiritsu = appliedList.stream()
				.findFirst()
				.orElse(null);

		List<ZeiritsuTeiritsu> teiritsuRates = new ArrayList<>();
		List<ZeiritsuTeigaku> teigakuRates = new ArrayList<>();
		List<ZeiritsuTeigaku> kenTeigakuRates = new ArrayList<>();

		if (appliedZeiritsu != null) {
			// 【第2段階】 特定されたマスタの設定値（fuka_kbn）に従って、厳密に処理を分岐する
			String currentFukaKbn = appliedZeiritsu.getFukaKbn();
			form.setFukaKbn(currentFukaKbn);

			if (FukaConstants.TEIRITSU.getValue().equals(currentFukaKbn)) {
				// --- 当月は「定率制」が適用される ---
				log.debug("対象年月 {} は【定率制】が適用されます (親Seq: {})", taishoYm, appliedZeiritsu.getSeq());
				teiritsuRates = zeiritsuTeiritsuRepository
						.findActiveBySeq(jichitaiCd, appliedZeiritsu.getSeq());
			} else {
				// --- 当月は「定額制」が適用される ---
				log.debug("対象年月 {} は【定額制】が適用されます (親Seq: {})", taishoYm, appliedZeiritsu.getSeq());
				teigakuRates = zeiritsuTeigakuRepository
						.findActiveBySeq(jichitaiCd, appliedZeiritsu.getSeq());
				kenTeigakuRates = zeiritsuTeigakuRepository
						.findActiveByTaishoKbnAndTekiyoYm(jichitaiCd, ZeiritsuConstants.KEN.getValue(), taishoYm);
			}
		} else {
			log.error("対象年月 {} に適用される税率マスタが存在しません。", taishoYm);
			throw new RuntimeException("対象年月（" + taishoYm + "）に適用される税率マスタが登録されていません。");
		}

		// 2. 読み込んだマスタを使って初期化
		setupMonthlyDetail(form, teigakuRates, teiritsuRates, kenTeigakuRates, taishoYm);

		FukaMonthlyTallyDto tallyDto = new FukaMonthlyTallyDto();
		// 💡 カラム数を動的に確保（定額・定率で取得できた方のサイズを使う）
		int categorySize = FukaConstants.TEIRITSU.getValue().equals(form.getFukaKbn())
				? teiritsuRates.size()
				: teigakuRates.size();
		tallyDto.initialize(categorySize);
		form.setMonthlyTally(tallyDto);

		return form;
	}

	/**
	 * 特別徴収義務者情報を取得しフォームにセットする。
	 */
	private void setupObligorInfo(FukaDeclarationForm form, String shiteiNo) {
		List<Tokugimu> result = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
		result.stream()
				.findFirst()
				.ifPresent(tokugimu -> {
					// 宛名情報を取得
					Optional<Atena> atenaOpt = atenaRepository
							.findByJichitaiCdAndAtenaNo(jichitaiCd, tokugimu.getAtenaNo());
					form.setObligorName(atenaOpt.map(atena -> atena.getName()).orElse(""));
					form.setFacilityName(tokugimu.getShisetsuName());
				});
	}

	/**
	 * 💡 【定額・定率両対応版】月計明細初期表示データの組み立て
	 * マスタから税率を読み込み、画面用フォームクラスのリスト構造を初期化します。
	 */
	private void setupMonthlyDetail(FukaDeclarationForm form, List<ZeiritsuTeigaku> teigakuRates,
			List<ZeiritsuTeiritsu> teiritsuRates, List<ZeiritsuTeigaku> kenTeigakuRates, String taishoYm) {
		FukaMonthlyDeclarationDto monthlyDetail = form.getMonthlyDetail();
		String taishoYmLabel = taishoYm.substring(0, 4) + "年" + taishoYm.substring(4) + "月";
		monthlyDetail.setPaymentYearMonth(taishoYmLabel);

		// 画面のリスト（taxDetails）をクリアして初期化
		monthlyDetail.setTaxDetails(new ArrayList<>());

		// 定率制
		if (FukaConstants.TEIRITSU.getValue().equals(form.getFukaKbn())) {
			if (teiritsuRates != null) {
				for (ZeiritsuTeiritsu rate : teiritsuRates) {
					FukaTaxDetailDto teiritsuDetail = new FukaTaxDetailDto();

					teiritsuDetail.setZeiritsuSeq(rate.getTeiritsuSeq());
					// 💡 マスタの区分名（例：「一般宿泊」）をセット
					teiritsuDetail.setLabel(rate.getKbnName());

					// 💡 マスタの税率をそのまま（BigDecimal型で）セット。nullの場合は0として扱う。
					teiritsuDetail.setTaxRate(rate.getZeiRitsu() != null ? rate.getZeiRitsu() : BigDecimal.ZERO);

					// 初期表示時は数量・金額は null (または0)
					teiritsuDetail.setHakusu(null);
					teiritsuDetail.setZeigaku(null);
					teiritsuDetail.setCityZeigaku(null);
					teiritsuDetail.setKenZeigaku(null);

					// リストへ追加
					monthlyDetail.getTaxDetails().add(teiritsuDetail);
				}
			}
			return;
		}

		// 定額制
		if (teigakuRates != null) {
			for (ZeiritsuTeigaku rate : teigakuRates) {
				ZeiritsuTeigaku kenRate = kenTeigakuRates.stream()
						.filter(z -> z.getRyokinSt() <= rate.getRyokinSt()
								&& (z.getRyokinEd() == null
										|| (rate.getRyokinEd() != null && rate.getRyokinEd() <= z.getRyokinEd())))
						.findFirst().orElse(null);
				FukaTaxDetailDto detail = new FukaTaxDetailDto();
				detail.setZeiritsuSeq(rate.getTeigakuSeq());
				detail.setTaxRate(rate.getZeigaku() != null ? BigDecimal.valueOf(rate.getZeigaku()) : BigDecimal.ZERO);
				detail.setTaxKenRate(
						kenRate != null && kenRate.getZeigaku() != null ? BigDecimal.valueOf(kenRate.getZeigaku()) : BigDecimal.ZERO);
				detail.setLabel(rate.getRyokinSt() + "円以上");
				detail.setHakusu(null);
				detail.setZeigaku(null);
				detail.setCityZeigaku(null);
				detail.setKenZeigaku(null);

				monthlyDetail.getTaxDetails().add(detail);
			}
		}
	}

	/**
	 * 賦課内訳情報を設定する
	 */
	private void setFukaUchiDataToForm(List<FukaUchi> uchiList, FukaMonthlyDeclarationDto dto) {
		if (uchiList == null || uchiList.isEmpty() || dto.getTaxDetails() == null) {
			return;
		}

		Map<BigDecimal, FukaUchi> uchiMap = uchiList.stream()
				.collect(Collectors.toMap(FukaUchi::getZeiritsuSeq, u -> u,
						(existing, replacement) -> existing.getRno() > replacement.getRno() ? existing : replacement));

		log.debug("[syncUchiDataToForm] uchiMap keys={}, dto.taxDetails size={}", uchiMap.keySet(),
				dto.getTaxDetails().size());
		for (FukaTaxDetailDto detail : dto.getTaxDetails()) {
			FukaUchi matched = uchiMap.get(detail.getZeiritsuSeq());

			if (matched != null) {
				if (FukaConstants.TEIRITSU.getValue().equals(matched.getFukaKbn())) {
					// 定率制
					detail.setRyokinSogaku(matched.getRyokinSogaku());
					detail.setRyokin(matched.getRyokin());
				} else {
					// 定額制
					detail.setCityZeigaku(matched.getCityZeigaku());
					detail.setKenZeigaku(matched.getKenZeigaku());
				}
				detail.setHakusu(matched.getHakusu());
				detail.setZeigaku(matched.getZeigaku());
				detail.setCityZeigaku(matched.getCityZeigaku());
				detail.setKenZeigaku(matched.getKenZeigaku());
			}
			if (detail.getTaxKenRate() == null) {
				detail.setTaxKenRate(BigDecimal.ZERO);
			}
		}
	}

	/**
	 * 編集・照会用の表示データを取得する。
	 */
	@Transactional(readOnly = true)
	public FukaDeclarationForm getDeclarationFormForEdit(String shiteiNo, String nendo, Integer kibetsu) {

		String taishoYm = createTaishoYmString(nendo, kibetsu);
		FukaDeclarationForm form = getDeclarationFormForRegister(shiteiNo, taishoYm);

		fukaRepository
				.findLatestByNendoAndKibetsu(jichitaiCd, shiteiNo, nendo, kibetsu)
				.stream()
				.findFirst()
				.ifPresent(entity -> {
					log.debug(
							"[getDeclarationFormForEdit] entity found: rno={}, fukaKbn={}, taishoYm={}, totalHakusu={}, totalZeigaku={}, kazeiRyokin={}",
							entity.getRno(), entity.getFukaKbn(), entity.getTaishoYm(), entity.getTotalHakusu(),
							entity.getTotalZeigaku(), entity.getKazeiRyokin());
					form.setTorokuDate(entity.getTorokuYmd());
					form.setShinkokuDate(entity.getShinkokuYmd());
					form.setNendo(entity.getNendo());
					form.setKibetsu(entity.getKibetsu());
					form.setFukaKbn(entity.getFukaKbn());
					form.setModificationCategory(entity.getHenkoKbn());
					form.setModificationReason(entity.getHenkoRiyu());

					// 加算金
					form.setAdditionalCategory1(entity.getKasanKbn1());
					if (entity.getKasanRitsu1() != null) {
						form.setAdditionalRate1(entity.getKasanRitsu1().toString());
					}
					form.setAdditionalAmount1(entity.getKasanGaku1());
					form.setAdditionalDueDate1(entity.getNokigen1());
					form.setAdditionalCategory2(entity.getKasanKbn2());
					if (entity.getKasanRitsu2() != null) {
						form.setAdditionalRate2(entity.getKasanRitsu2().toString());
					}
					form.setAdditionalAmount2(entity.getKasanGaku2());
					form.setAdditionalDueDate2(entity.getNokigen2());
					form.setAdditionalCategory3(entity.getKasanKbn3());
					if (entity.getKasanRitsu3() != null) {
						form.setAdditionalRate3(entity.getKasanRitsu3().toString());
					}
					form.setAdditionalAmount3(entity.getKasanGaku3());
					form.setAdditionalDueDate3(entity.getNokigen3());

					// 賦課情報設定
					setMonthlyDetail(entity, form);
					// 徴収原簿設定
					setMonthlyTally(form, entity);
				});

		// 納入情報の取得
		shunoRirekiRepository.findLatest(jichitaiCd, shiteiNo, nendo, kibetsu)
				.ifPresent(shuno -> {
					form.setShunoFlg(true);
					form.setShunoYmd(shuno.getNonyuYmd());
					form.setShunoKingaku(shuno.getNonyugaku() != null ? shuno.getNonyugaku().longValue() : null);
				});

		return form;
	}

	/**
	 * 照会用の表示データを取得する。
	 */
	@Transactional(readOnly = true)
	public FukaDeclarationForm getDeclarationFormForView(String shiteiNo, String nendo, Integer kibetsu) {
		FukaDeclarationForm form = getDeclarationFormForEdit(shiteiNo, nendo, kibetsu);
		form.setView(true);
		return form;
	}

	/**
	 * 宿泊税情報の保存処理を実行する。
	 */
	@Transactional
	public void saveDeclaration(FukaDeclarationForm form) {

		// 既存履歴の最新フラグをクリアする
		List<Fuka> oldEntityList = fukaRepository
				.findByJichitaiCdAndShiteiNoAndNendoAndKibetsuAndNewFlgAndDelFlg(jichitaiCd, form.getShiteiNo(),
						form.getNendo(), form.getKibetsu(), DEFAULT_NEW_FLG, DEFAULT_DEL_FLG);
		oldEntityList.stream().forEach(e -> {
			e.setNewFlg("0");
			fukaRepository.save(e);
		});

		Integer targetRno = determineNextRno(form.getShiteiNo(), form.getNendo(), form.getKibetsu());

		Fuka parentFuka = createParentFuka(form);
		parentFuka.setRno(targetRno);

		List<FukaUchi> uchiList = createFukaUchiList(form, parentFuka);

		// 定額の場合は、市区町村税額、都道府県税額に賦課内訳情報の合計を設定
		if (FukaConstants.TEIGAKU.getValue().equals(parentFuka.getFukaKbn())) {
			long cityZeigakuSum = uchiList.stream()
					.mapToLong(u -> u.getCityZeigaku() != null ? u.getCityZeigaku() : 0L)
					.sum();
			long kenZeigakuSum = uchiList.stream()
					.mapToLong(u -> u.getKenZeigaku() != null ? u.getKenZeigaku() : 0L)
					.sum();
			parentFuka.setCityZeigaku(cityZeigakuSum);
			parentFuka.setKenZeigaku(kenZeigakuSum);
		}

		fukaRepository.save(parentFuka);
		if (!uchiList.isEmpty()) {
			fukaUchiRepository.saveAll(uchiList);
		}

		if (form.getMonthlyTally() != null) {
			saveChoshuGenboDataWithRno(form, parentFuka, jichitaiCd, targetRno);
		}

		// 納入情報の保存
		if (form.isShunoFlg()) {
			Integer shunoRno = shunoRirekiRepository.findMaxRno(jichitaiCd, form.getShiteiNo(), form.getNendo(), form.getKibetsu())
					.map(r -> r + 1).orElse(1);
			ShunoRireki shuno = new ShunoRireki();
			shuno.setJichitaiCd(jichitaiCd);
			shuno.setShiteiNo(form.getShiteiNo());
			shuno.setNendo(form.getNendo());
			shuno.setKibetsu(form.getKibetsu());
			shuno.setRno(shunoRno);
			shuno.setNonyuYmd(form.getShunoYmd());
			shuno.setNonyugaku(form.getShunoKingaku() != null ? form.getShunoKingaku().intValue() : null);
			shunoRirekiRepository.save(shuno);
		}
	}

	/**
	 * 次の履歴番号を決定する。
	 */
	private Integer determineNextRno(String shiteiNo, String nendo, Integer kibetsu) {
		return fukaRepository
				.findMaxRno(jichitaiCd, shiteiNo, nendo, kibetsu)
				.map(r -> r + 1)
				.orElse(1);
	}

	/**
	 * 内訳エンティティのリストを生成する。
	 */
	private List<FukaUchi> createFukaUchiList(FukaDeclarationForm form, Fuka parentFuka) {
		List<FukaUchi> uchiList = new ArrayList<>();
		FukaMonthlyDeclarationDto dto = form.getMonthlyDetail();

		for (int i = 0; i < dto.getTaxDetails().size(); i++) {
			FukaTaxDetailDto detail = dto.getTaxDetails().get(i);

			FukaUchi uchi = new FukaUchi();
			uchi.setJichitaiCd(parentFuka.getJichitaiCd());
			uchi.setShiteiNo(parentFuka.getShiteiNo());
			uchi.setNendo(parentFuka.getNendo());
			uchi.setKibetsu(parentFuka.getKibetsu());
			uchi.setRno(parentFuka.getRno());
			uchi.setKazeiKbn(i + 1);
			uchi.setFukaKbn(parentFuka.getFukaKbn());
			uchi.setZeiritsuSeq(detail.getZeiritsuSeq());

			if (FukaConstants.TEIRITSU.getValue().equals(form.getFukaKbn())) {
				// 定率制: 都道府県税額、市区町村税額算出なし
				uchi.setZeigaku(getLongValue(detail.getZeigaku()));
				uchi.setCityZeigaku(null);
				uchi.setKenZeigaku(null);
				uchi.setRyokin(getLongValue(detail.getRyokin()));
				uchi.setRyokinSogaku(getLongValue(detail.getRyokinSogaku()));
			} else {
				// 定額制: 都道府県税額、市区町村税額を算出
				long hakusu = getLongValue(detail.getHakusu());
				long kenZeigaku = detail.getTaxKenRate().longValue() * hakusu;
				long zeigaku = (detail.getZeigaku() != null) ? detail.getZeigaku() : 0L;
				long cityZeigaku = zeigaku - kenZeigaku >= 0L ? zeigaku - kenZeigaku : 0L; // 差引き
				kenZeigaku = zeigaku - cityZeigaku;
				uchi.setZeigaku(zeigaku);
				uchi.setKenZeigaku(kenZeigaku);
				uchi.setCityZeigaku(cityZeigaku);
				uchi.setRyokin(null);
				uchi.setRyokinSogaku(null);
			}
			uchi.setHakusu(getLongValue(detail.getHakusu()));
			uchi.setZeiRitsu(detail.getTaxRate());

			uchiList.add(uchi);
		}
		return uchiList;
	}

	/**
	 * 指定された対象年月に該当する申告データが存在するか判定する。
	 */
	public boolean isAlreadyRegistered(String shiteiNo, String taishoYm) {
		String nendo = calculateNendo(taishoYm);
		Integer kibetsu = calculateKibetsu(taishoYm);
		return isAlreadyRegisteredByKibetsu(shiteiNo, nendo, kibetsu);

	}

	/**
	 * 指定された年度・期別に該当する申告データが存在するか判定する。
	 */
	public boolean isAlreadyRegisteredByKibetsu(String shiteiNo, String nendo, Integer kibetsu) {
		return !fukaRepository.findLatestByNendoAndKibetsu(jichitaiCd, shiteiNo, nendo, kibetsu).isEmpty();
	}

	/**
	 * 親エンティティを生成する。
	 */
	private Fuka createParentFuka(FukaDeclarationForm form) {
		FukaMonthlyDeclarationDto dto = form.getMonthlyDetail();
		String taishoYm = dto.getPaymentYearMonth().replace("年", "").replace("月", "");
		Fuka parentFuka = new Fuka();
		parentFuka.setJichitaiCd(jichitaiCd);
		parentFuka.setShiteiNo(form.getShiteiNo());

		parentFuka.setNendo(calculateNendo(taishoYm));
		parentFuka.setKibetsu(calculateKibetsu(taishoYm));
		parentFuka.setTorokuYmd(form.getTorokuDate() != null ? form.getTorokuDate() : LocalDate.now());
		parentFuka.setShinkokuYmd(form.getShinkokuDate() != null ? form.getShinkokuDate() : LocalDate.now());
		parentFuka.setFukaKbn(
				StringUtils.hasText(form.getFukaKbn()) ? form.getFukaKbn() : FukaConstants.TEIGAKU.getValue());
		String henkoKbn = FukaConstants.HENKO_KUBUN_LIST
				.stream()
				.anyMatch(kbn -> kbn.getValue().equals(form.getModificationCategory()))
						? form.getModificationCategory()
						: FukaConstants.SHINKOKU.getValue();
		parentFuka.setHenkoKbn(henkoKbn);
		parentFuka.setHenkoRiyu(form.getModificationReason());
		parentFuka.setNewFlg(DEFAULT_NEW_FLG);
		parentFuka.setDelFlg(DEFAULT_DEL_FLG);
		parentFuka.setTaishoYm(taishoYm);
		parentFuka.setTotalHakusu(getLongValue(dto.getTotalStayCount()));
		parentFuka.setKazeiHakusu(getLongValue(dto.getTotalStayCount()) - getLongValue(dto.getExemptStayCount()));
		parentFuka.setTotalZeigaku(getLongValue(dto.getTotalPaymentAmount()));
		parentFuka.setZeigaku(getLongValue(dto.getTotalPaymentAmount()));
		parentFuka.setMenjoHakusu(getLongValue(dto.getExemptStayCount()));

		if (FukaConstants.TEIRITSU.getValue().equals(form.getFukaKbn())) {
			// 定率制
			long totalRyokin = getLongValue(dto.getKazeiRyokin());
			parentFuka.setKazeiRyokin(totalRyokin);
			parentFuka.setSogaku(getLongValue(dto.getTotalSogaku()));
			parentFuka.setMenjoRyokin(getLongValue(dto.getExemptRyokin()));
			// 市区町村税額、都道府県税額は賦課内訳を算出
			long totalShukuhakushaSu = parentFuka.getKazeiHakusu();
			long ryokin = totalShukuhakushaSu > 0 ? totalRyokin / totalShukuhakushaSu : 0;
			long kenZeigaku = getKenZeigaku(ryokin, taishoYm) * totalShukuhakushaSu;
			long cityZeigaku = parentFuka.getTotalZeigaku() - kenZeigaku >= 0
					? parentFuka.getTotalZeigaku() - kenZeigaku
					: 0L;
			kenZeigaku = parentFuka.getTotalZeigaku() - cityZeigaku;
			parentFuka.setCityZeigaku(cityZeigaku);
			parentFuka.setKenZeigaku(kenZeigaku);
		} else {
			// 定額制
			// 市区町村税額、都道府県税額は賦課内訳の算出後に合計を設定
			parentFuka.setCityZeigaku(0L);
			parentFuka.setKenZeigaku(0L);
		}

		if (!form.getAdditionalCategory1().isEmpty()) {
			parentFuka.setKasanKbn1(form.getAdditionalCategory1());
			if (StringUtils.hasText(form.getAdditionalRate1())) {
				try {
					parentFuka.setKasanRitsu1(new BigDecimal(form.getAdditionalRate1()));
				} catch (NumberFormatException e) {
					log.warn("加算割合の数値変換に失敗しました: {}", form.getAdditionalRate1());
					parentFuka.setKasanRitsu1(null);
				}
			} else {
				parentFuka.setKasanRitsu1(null);
			}
			parentFuka.setKasanGaku1(form.getAdditionalAmount1());
			parentFuka.setNokigen1(form.getAdditionalDueDate1());
		}

		if (!form.getAdditionalCategory2().isEmpty()) {
			parentFuka.setKasanKbn2(form.getAdditionalCategory2());
			if (StringUtils.hasText(form.getAdditionalRate2())) {
				try {
					parentFuka.setKasanRitsu2(new BigDecimal(form.getAdditionalRate2()));
				} catch (NumberFormatException e) {
					log.warn("加算割合の数値変換に失敗しました: {}", form.getAdditionalRate2());
					parentFuka.setKasanRitsu2(null);
				}
			} else {
				parentFuka.setKasanRitsu2(null);
			}
			parentFuka.setKasanGaku2(form.getAdditionalAmount2());
			parentFuka.setNokigen2(form.getAdditionalDueDate2());
		}

		if (!form.getAdditionalCategory3().isEmpty()) {
			parentFuka.setKasanKbn3(form.getAdditionalCategory3());
			if (StringUtils.hasText(form.getAdditionalRate3())) {
				try {
					parentFuka.setKasanRitsu3(new BigDecimal(form.getAdditionalRate3()));
				} catch (NumberFormatException e) {
					log.warn("加算割合の数値変換に失敗しました: {}", form.getAdditionalRate3());
					parentFuka.setKasanRitsu3(null);
				}
			} else {
				parentFuka.setKasanRitsu3(null);
			}
			parentFuka.setKasanGaku3(form.getAdditionalAmount3());
			parentFuka.setNokigen3(form.getAdditionalDueDate3());
		}
		return parentFuka;
	}

	/**
	 * 都道府県税額を取得する
	 */
	private long getKenZeigaku(Long shukuhakuRyokin, String taishoYm) {
		Optional<ZeiritsuTeigaku> teigakuOp = zeiritsuTeigakuRepository
				.findActiveByTaishoKbnAndTekiyoYmAndRyokin(jichitaiCd, ZeiritsuConstants.KEN.getValue(), taishoYm,
						shukuhakuRyokin);
		return teigakuOp.map(ZeiritsuTeigaku::getZeigaku).orElse(0L);
	}

	/**
	 * 賦課情報を設定する（照会・編集画面用）
	 */
	private void setMonthlyDetail(Fuka entity, FukaDeclarationForm form) {
		FukaMonthlyDeclarationDto monthDto = form.getMonthlyDetail();
		monthDto.setExemptStayCount(entity.getMenjoHakusu());
		monthDto.setExemptRyokin(entity.getMenjoRyokin());
		monthDto.setTotalSogaku(entity.getSogaku());
		monthDto.setTotalStayCount(entity.getTotalHakusu());
		monthDto.setTotalPaymentAmount(entity.getTotalZeigaku());
		monthDto.setTotalCityZeigaku(entity.getCityZeigaku());
		monthDto.setTotalKenZeigaku(entity.getKenZeigaku());
		monthDto.setKazeiRyokin(entity.getKazeiRyokin());

		// 賦課内訳情報を設定する
		List<FukaUchi> uchiList = fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
				jichitaiCd, form.getShiteiNo(), entity.getRno(), entity.getNendo(), entity.getKibetsu());

		log.debug("[setMonthlyDetail] uchiList size={}, taxDetails size={}, rno={}, nendo={}, kibetsu={}",
				uchiList.size(), monthDto.getTaxDetails().size(), entity.getRno(), entity.getNendo(),
				entity.getKibetsu());
		setFukaUchiDataToForm(uchiList, monthDto);
	}

	// =========================================================
	// 月計表（徴収原簿）保存・復元処理
	// =========================================================

	private List<Long> collectUchiIndices(ChoshuGenbo genbo) {
		List<Long> indices = new ArrayList<>();
		indices.add(genbo.getUchiIdx1());
		indices.add(genbo.getUchiIdx2());
		indices.add(genbo.getUchiIdx3());
		indices.add(genbo.getUchiIdx4());
		indices.add(genbo.getUchiIdx5());
		indices.add(genbo.getUchiIdx6());
		indices.add(genbo.getUchiIdx7());
		indices.add(genbo.getUchiIdx8());
		indices.add(genbo.getUchiIdx9());
		indices.add(genbo.getUchiIdx10());
		indices.add(genbo.getUchiIdx11());
		indices.add(genbo.getUchiIdx12());
		indices.add(genbo.getUchiIdx13());
		indices.add(genbo.getUchiIdx14());
		indices.add(genbo.getUchiIdx15());
		indices.add(genbo.getUchiIdx16());
		indices.add(genbo.getUchiIdx17());
		indices.add(genbo.getUchiIdx18());
		indices.add(genbo.getUchiIdx19());
		indices.add(genbo.getUchiIdx20());
		indices.add(genbo.getUchiIdx21());
		indices.add(genbo.getUchiIdx22());
		indices.add(genbo.getUchiIdx23());
		indices.add(genbo.getUchiIdx24());
		indices.add(genbo.getUchiIdx25());
		indices.add(genbo.getUchiIdx26());
		indices.add(genbo.getUchiIdx27());
		indices.add(genbo.getUchiIdx28());
		indices.add(genbo.getUchiIdx29());
		indices.add(genbo.getUchiIdx30());
		indices.add(genbo.getUchiIdx31());
		return indices;
	}

	private void setUchiIndicesToGenbo(ChoshuGenbo genbo, Long[] indices) {
		genbo.setUchiIdx1(indices[0]);
		genbo.setUchiIdx2(indices[1]);
		genbo.setUchiIdx3(indices[2]);
		genbo.setUchiIdx4(indices[3]);
		genbo.setUchiIdx5(indices[4]);
		genbo.setUchiIdx6(indices[5]);
		genbo.setUchiIdx7(indices[6]);
		genbo.setUchiIdx8(indices[7]);
		genbo.setUchiIdx9(indices[8]);
		genbo.setUchiIdx10(indices[9]);
		genbo.setUchiIdx11(indices[10]);
		genbo.setUchiIdx12(indices[11]);
		genbo.setUchiIdx13(indices[12]);
		genbo.setUchiIdx14(indices[13]);
		genbo.setUchiIdx15(indices[14]);
		genbo.setUchiIdx16(indices[15]);
		genbo.setUchiIdx17(indices[16]);
		genbo.setUchiIdx18(indices[17]);
		genbo.setUchiIdx19(indices[18]);
		genbo.setUchiIdx20(indices[19]);
		genbo.setUchiIdx21(indices[20]);
		genbo.setUchiIdx22(indices[21]);
		genbo.setUchiIdx23(indices[22]);
		genbo.setUchiIdx24(indices[23]);
		genbo.setUchiIdx25(indices[24]);
		genbo.setUchiIdx26(indices[25]);
		genbo.setUchiIdx27(indices[26]);
		genbo.setUchiIdx28(indices[27]);
		genbo.setUchiIdx29(indices[28]);
		genbo.setUchiIdx30(indices[29]);
		genbo.setUchiIdx31(indices[30]);
	}

	private boolean isDailyDataPresent(DailyItem item) {
		if (item == null)
			return false;
		boolean hasSogaku = item.getSogaku() != null
				&& item.getSogaku().stream().anyMatch(v -> v != null && v > 0);
		boolean hasCount = item.getHakusu() != null
				&& item.getHakusu().stream().anyMatch(v -> v != null && v > 0);
		boolean hasAmount = item.getRyokin() != null
				&& item.getRyokin().stream().anyMatch(v -> v != null && v > 0);
		boolean hasMenjoHakusu = item.getMenjoHakusu() != null && item.getMenjoHakusu() > 0;
		boolean hasMenjoRyokin = item.getMenjoRyokin() != null && item.getMenjoRyokin() > 0;
		boolean hasZeigaku = item.getZeigaku() != null && item.getZeigaku() > 0;
		return hasSogaku || hasCount || hasAmount || hasMenjoHakusu || hasMenjoRyokin || hasZeigaku;
	}

	private void setSogakuByIndex(ChoshuGenboUchi uchi, int index, Long value) {
		try {
			String methodName = "setSogaku" + index;
			uchi.getClass().getMethod(methodName, Long.class).invoke(uchi, value);
		} catch (Exception e) {
			log.warn("setSogaku{} failed: {}", index, e.getMessage());
		}
	}

	private Long getSogakuValue(ChoshuGenboUchi uchi, int index) {
		try {
			String methodName = "getSogaku" + index;
			Object val = uchi.getClass().getMethod(methodName).invoke(uchi);
			return (val != null) ? (Long) val : 0L;
		} catch (Exception e) {
			return 0L;
		}
	}

	private void setHakusuByIndex(ChoshuGenboUchi uchi, int index, Integer value) {
		try {
			String methodName = "setHakusu" + index;
			uchi.getClass().getMethod(methodName, Integer.class).invoke(uchi, value);
		} catch (Exception e) {
			log.warn("setHakusu{} failed: {}", index, e.getMessage());
		}
	}

	private Integer getHakusuValue(ChoshuGenboUchi uchi, int index) {
		try {
			String methodName = "getHakusu" + index;
			Object val = uchi.getClass().getMethod(methodName).invoke(uchi);
			return (val != null) ? (Integer) val : 0;
		} catch (Exception e) {
			return 0;
		}
	}

	private void setRyokinByIndex(ChoshuGenboUchi uchi, int index, Long value) {
		try {
			String methodName = "setRyokin" + index;
			uchi.getClass().getMethod(methodName, Long.class).invoke(uchi, value);
		} catch (Exception e) {
			log.warn("setRyokin{} failed: {}", index, e.getMessage());
		}
	}

	private Long getRyokinValue(ChoshuGenboUchi uchi, int index) {
		try {
			String methodName = "getRyokin" + index;
			Object val = uchi.getClass().getMethod(methodName).invoke(uchi);
			return (val != null) ? (Long) val : 0L;
		} catch (Exception e) {
			return 0L;
		}
	}

	private Long getLongValue(Long value) {
		return value == null ? 0L : value;
	}

	/**
	 * 月計表データを保存する。
	 */
	private void saveChoshuGenboDataWithRno(FukaDeclarationForm form, Fuka parentFuka, String jichitaiCd,
			int targetRno) {
		ChoshuGenboId genboId = new ChoshuGenboId(
				jichitaiCd, parentFuka.getShiteiNo(), targetRno, parentFuka.getNendo(), parentFuka.getKibetsu());

		Optional<ChoshuGenbo> existingGenboOpt = choshuGenboRepository.findById(genboId);
		Long[] uchiIndices = new Long[MAX_DAYS];
		if (existingGenboOpt.isPresent()) {
			List<Long> currentIndices = collectUchiIndices(existingGenboOpt.get());
			for (int i = 0; i < MAX_DAYS; i++)
				uchiIndices[i] = currentIndices.get(i);
		}

		List<DailyItem> dailyItems = form.getMonthlyTally().getDailyItems();
		Long currentMaxIdx = choshuGenboUchiRepository.getMaxUchiIdx(jichitaiCd);

		for (int i = 0; i < dailyItems.size() && i < MAX_DAYS; i++) {
			DailyItem item = dailyItems.get(i);
			if (isDailyDataPresent(item)) {
				Long targetIdx = uchiIndices[i];
				if (targetIdx == null) {
					currentMaxIdx++;
					targetIdx = currentMaxIdx;
				}
				uchiIndices[i] = targetIdx;

				ChoshuGenboUchi uchi = new ChoshuGenboUchi();
				uchi.setJichitaiCd(jichitaiCd);
				uchi.setUchiIdx(targetIdx);

				List<Long> sogaku = item.getSogaku();
				if (sogaku != null) {
					for (int k = 0; k < sogaku.size(); k++) {
						Long aVal = sogaku.get(k);
						if (aVal != null && aVal > 0) {
							setSogakuByIndex(uchi, k + 1, aVal);
						}
					}
				}

				List<Integer> hakusu = item.getHakusu();
				for (int k = 0; k < hakusu.size(); k++) {
					Integer cVal = hakusu.get(k);
					if (cVal != null && cVal > 0) {
						setHakusuByIndex(uchi, k + 1, cVal.intValue());
					}
				}

				List<Long> ryokin = item.getRyokin();
				if (ryokin != null) {
					for (int k = 0; k < ryokin.size(); k++) {
						Long aVal = ryokin.get(k);
						if (aVal != null && aVal > 0) {
							setRyokinByIndex(uchi, k + 1, aVal);
						}
					}
				}

				uchi.setMenjoHakusu(item.getMenjoHakusu());
				uchi.setMenjoRyokin(item.getMenjoRyokin());
				uchi.setZeigaku(item.getZeigaku());
				choshuGenboUchiRepository.save(uchi);
			} else {
				uchiIndices[i] = null;
			}
		}

		ChoshuGenbo genbo = existingGenboOpt.orElse(new ChoshuGenbo());
		genbo.setJichitaiCd(jichitaiCd);
		genbo.setShiteiNo(parentFuka.getShiteiNo());
		genbo.setNendo(parentFuka.getNendo());
		genbo.setKibetsu(parentFuka.getKibetsu());
		genbo.setRno(targetRno);
		setUchiIndicesToGenbo(genbo, uchiIndices);
		choshuGenboRepository.save(genbo);
	}

	/**
	 * 徴収原簿を設定する
	 */
	private void setMonthlyTally(FukaDeclarationForm form, Fuka parentFuka) {
		ChoshuGenboId genboId = new ChoshuGenboId(jichitaiCd, parentFuka.getShiteiNo(), parentFuka.getRno(),
				parentFuka.getNendo(), parentFuka.getKibetsu());

		Optional<ChoshuGenbo> genboOpt = choshuGenboRepository.findById(genboId);
		if (genboOpt.isEmpty()) {
			return;
		}

		ChoshuGenbo genbo = genboOpt.get();
		List<Long> uchiIndices = collectUchiIndices(genbo);

		List<ChoshuGenboUchi> uchiList = choshuGenboUchiRepository.findByJichitaiCdAndUchiIdxIn(jichitaiCd,
				uchiIndices);

		Map<Long, ChoshuGenboUchi> uchiMap = uchiList.stream()
				.collect(java.util.stream.Collectors.toMap(ChoshuGenboUchi::getUchiIdx, u -> u));

		int categoryCount = form.getMonthlyDetail().getTaxDetails().size();
		form.getMonthlyTally().initialize(categoryCount);

		for (int i = 0; i < MAX_DAYS; i++) {
			Long idx = uchiIndices.get(i);
			if (idx != null && uchiMap.containsKey(idx)) {
				ChoshuGenboUchi uchi = uchiMap.get(idx);
				DailyItem dDto = form.getMonthlyTally().getDailyItems().get(i);
				for (int j = 1; j <= categoryCount; j++) {
					dDto.getHakusu().set(j - 1, getHakusuValue(uchi, j));
					dDto.getRyokin().set(j - 1, getRyokinValue(uchi, j));
					dDto.getSogaku().set(j - 1, getSogakuValue(uchi, j));
				}
				dDto.setMenjoHakusu(uchi.getMenjoHakusu());
				dDto.setMenjoRyokin(uchi.getMenjoRyokin());
				dDto.setZeigaku(getLongValue(uchi.getZeigaku()));
			}
		}
	}

	/**
	 * 賦課区分に応じた税額計算を行う。
	 * @param fukaKbn 賦課区分コード（"1"=定額, "2"=定率）
	 * @param baseValue 基準値（定額なら宿泊数、定率なら課税対象料金）
	 * @param cityRate 市区町村税率（定額なら円、定率ならパーセント）
	 * @param kenRate 都道府県税率（定額なら円、定率はnull）
	 * @return 計算後の税額
	 */
	public long calculateTax(String fukaKbn, long baseValue, BigDecimal cityRate, BigDecimal kenRate) {

		BigDecimal city = (cityRate != null) ? cityRate : BigDecimal.ZERO;
		BigDecimal ken = (kenRate != null) ? kenRate : BigDecimal.ZERO;

		if (FukaConstants.TEIRITSU.getValue().equals(fukaKbn)) {
			// 定率制：宿泊料金 × 税率(%) / 100（端数切り捨て）
			return BigDecimal.valueOf(baseValue)
					.multiply(city)
					.divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.DOWN)
					.longValue();
		} else {
			// 定額制：宿泊数 × 税率
			long rate = cityRate.longValue() + kenRate.longValue();
			return baseValue * rate;
		}
	}
}

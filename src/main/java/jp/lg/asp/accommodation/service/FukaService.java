package jp.lg.asp.accommodation.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jp.lg.asp.accommodation.dto.FukaDaichoForm;
import jp.lg.asp.accommodation.dto.FukaDaichoListItem;
import jp.lg.asp.accommodation.dto.FukaDeclarationForm;
import jp.lg.asp.accommodation.dto.FukaMonthlyDeclarationDto;
import jp.lg.asp.accommodation.dto.FukaMonthlyTallyDto;
import jp.lg.asp.accommodation.dto.FukaTaxDetailDto;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.FukaId;
import jp.lg.asp.accommodation.entity.FukaUchi;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.entity.Zeiritsu;
import jp.lg.asp.accommodation.entity.ZeiritsuTeigaku;
import jp.lg.asp.accommodation.entity.ZeiritsuTeiritsu;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.FukaUchiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeigakuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeiritsuRepository;
import jp.lg.asp.accommodation.enums.FukaKbn;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 宿泊税納入（賦課）に関するビジネスロジックを担当するサービスクラス。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FukaService {

	private final FukaRepository fukaRepository;
	private final TokugimuRepository tokugimuRepository;
	private final ZeiritsuRepository zeiritsuRepository;
	private final ZeiritsuTeigakuRepository zeiritsuTeigakuRepository;
	private final ZeiritsuTeiritsuRepository zeiritsuTeiritsuRepository;
	private final FukaUchiRepository fukaUchiRepository;

	// 定数定義（マジックナンバーの排除）
	private static final String STATUS_ALL = "999";
	private static final String STATUS_ZUMI = "1";
	private static final String STATUS_MI = "2";
	private static final int MAX_KIBETSU = 12;
	private static final int FISCAL_START_MONTH = 4;
	private static final String DEFAULT_NEW_FLG = "1";
	private static final String DEFAULT_DEL_FLG = "0";
	private static final int INITIAL_VERSION = 1;

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	@Value("${app.jichitai.code}")
	private String configJichitaiCd;

	/**
	 * 納入金額管理台帳のデータを取得する。
	 */
	@Transactional(readOnly = true)
	public FukaDaichoForm getDaichoData(String shiteiNo, String nendo, String status) {
		FukaDaichoForm form = new FukaDaichoForm();
		form.setShiteiNo(shiteiNo);
		form.setNendo(nendo);
		form.setStatus(status != null ? status : STATUS_ALL);

		tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
				.stream()
				.findFirst()
				.ifPresent(tokugimu -> form.setObligorName(tokugimu.getKyokaName()));

		List<Fuka> fukaList = fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(jichitaiCd, shiteiNo,
				nendo);
		Map<Integer, Fuka> fukaMap = createFukaMap(fukaList);

		form.setItems(createDaichoItems(nendo, fukaMap, form.getStatus()));
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
	private List<FukaDaichoListItem> createDaichoItems(String nendo, Map<Integer, Fuka> fukaMap, String filterStatus) {
		List<FukaDaichoListItem> items = new ArrayList<>();
		for (int i = 1; i <= MAX_KIBETSU; i++) {
			FukaDaichoListItem item = buildDaichoItem(nendo, i, fukaMap);

			if (STATUS_ZUMI.equals(filterStatus) && !item.isShinkokuZumi()) {
				continue;
			}
			if (STATUS_MI.equals(filterStatus) && item.isShinkokuZumi()) {
				continue;
			}
			item.setNendo(nendo);       // 年度をセット
	        item.setKibetsu(i);        // 期別（月）をセット
			items.add(item);
		}
		return items;
	}

	/**
	 * 単一の台帳明細行を組み立てる。
	 */
	private FukaDaichoListItem buildDaichoItem(String nendo, int kibetsu, Map<Integer, Fuka> fukaMap) {
		FukaDaichoListItem item = new FukaDaichoListItem();
		item.setNendo(nendo);
		item.setKibetsu(kibetsu);

		int displayMonth = (kibetsu + 3) > MAX_KIBETSU ? (kibetsu + 3) - MAX_KIBETSU : (kibetsu + 3);
		item.setDisplayNengetsu(displayMonth + "月");

		int nokiMonth = displayMonth == MAX_KIBETSU ? 1 : displayMonth + 1;
		item.setDisplayNoki(nokiMonth + "月末");

		int year = Integer.parseInt(nendo);
		if (displayMonth < FISCAL_START_MONTH) {
			year++;
		}
		item.setTargetYearMonth(LocalDate.of(year, displayMonth, 1));

		if (fukaMap.containsKey(kibetsu)) {
			Fuka dbData = fukaMap.get(kibetsu);
			item.setAmount(dbData.getTotalZeigaku());
			item.setTotalZeigaku(dbData.getTotalZeigaku());
			item.setStatus("済");
			item.setShinkokuYmd(dbData.getShinkokuYmd());
			item.setShinkokuZumi(true);
		} else {
			item.setAmount(0L);
			item.setTotalZeigaku(0L);
			item.setStatus("未");
			item.setShinkokuZumi(false);
		}
		return item;
	}

	/**
	 * 操作対象の自治体コードを取得する。
	 */
	private String getCurrentJichitaiCd() {
		return this.configJichitaiCd;
	}

	/**
	 * 新規登録用の初期表示データを取得する。
	 */
	@Transactional(readOnly = true)
	public FukaDeclarationForm getDeclarationFormForRegister(String shiteiNo, String paymentMonth) {
		FukaDeclarationForm form = new FukaDeclarationForm();
		form.setShiteiNo(shiteiNo);
		form.setRegistrationDate(LocalDate.now());

		if (StringUtils.hasText(paymentMonth)) {
			form.setNendo(calculateNendo(paymentMonth));
			form.setKibetsu(calculateKibetsu(paymentMonth));
		}

		try {
			setupObligorInfo(form, shiteiNo);

			// 対象年月の生成（例："202605"）
			String targetYm = "";
			if (StringUtils.hasText(paymentMonth)) {
				targetYm = paymentMonth.replace("-", "");
			} else {
				targetYm = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
			}

			// =========================================================
			// 💡 【仕様書準拠】適用時期ベースの税体系判定ロジック
			// =========================================================

			// 【第1段階】 賦課区分（定額/定率）を問わず、現在の自治体の有効な税率マスタをすべて取得する
			// ※ZeiritsuRepository に定義済みの findActiveByJichitaiCd を利用
			List<Zeiritsu> allZeiritsu = zeiritsuRepository.findActiveByJichitaiCd(jichitaiCd);

			Zeiritsu appliedZeiritsu = null;
			int targetYmInt = parseYmToInt(targetYm);

			// 対象年月に合致する「唯一の適用マスタ」を特定する
			for (Zeiritsu z : allZeiritsu) {

				int stYmInt = parseYmToInt(z.getTekiyoStYm());
				int edYmInt = parseYmToInt(z.getTekiyoEdYm());

				boolean isAfterStart = (stYmInt == 0 || stYmInt <= targetYmInt);
				boolean isBeforeEnd = (edYmInt == 0 || targetYmInt <= edYmInt);

				if (isAfterStart && isBeforeEnd) {
					appliedZeiritsu = z; // 期間が一致したマスタを「当月のルール」として確定！
					break;
				}
			}

			List<ZeiritsuTeiritsu> teiritsuRates = new ArrayList<>();
			List<ZeiritsuTeigaku> teigakuRates = new ArrayList<>();

			if (appliedZeiritsu != null) {
				// 【第2段階】 特定されたマスタの設定値（fuka_kbn）に従って、厳密に処理を分岐する
				String currentFukaKbn = appliedZeiritsu.getFukaKbn();
				form.setFukaKbn(currentFukaKbn);

				if ("2".equals(currentFukaKbn)) {
					// --- 当月は「定率制」が適用される ---
					log.info("対象年月 {} は【定率制】が適用されます (親Seq: {})", targetYm, appliedZeiritsu.getSeq());
					teiritsuRates = zeiritsuTeiritsuRepository
							.findByJichitaiCdAndSeqAndDelFlgOrderByTeiritsuSeqAsc(jichitaiCd, appliedZeiritsu.getSeq(),
									"0");
				} else {
					// --- 当月は「定額制」が適用される ---
					log.info("対象年月 {} は【定額制】が適用されます (親Seq: {})", targetYm, appliedZeiritsu.getSeq());
					teigakuRates = zeiritsuTeigakuRepository.findByJichitaiCdOrderByRyokinStAsc(jichitaiCd);
				}
			} else {
				// 【フォールバック】適用期間に合致するマスタがない場合、有効マスタの先頭のfukaKbnを採用する
				if (!allZeiritsu.isEmpty()) {
					Zeiritsu fallback = allZeiritsu.get(0);
					String fallbackKbn = fallback.getFukaKbn();
					log.warn("対象年月 {} に適用される税率マスタが見つかりません。有効マスタ(seq={}, fukaKbn={})を採用します", targetYm, fallback.getSeq(), fallbackKbn);
					form.setFukaKbn(fallbackKbn);
					if ("2".equals(fallbackKbn)) {
						teiritsuRates = zeiritsuTeiritsuRepository
								.findByJichitaiCdAndSeqAndDelFlgOrderByTeiritsuSeqAsc(jichitaiCd, fallback.getSeq(), "0");
					} else {
						teigakuRates = zeiritsuTeigakuRepository.findByJichitaiCdOrderByRyokinStAsc(jichitaiCd);
					}
				} else {
					log.error("有効な税率マスタが1件も存在しません。デフォルト定額制で初期化します");
					form.setFukaKbn("1");
					teigakuRates = zeiritsuTeigakuRepository.findByJichitaiCdOrderByRyokinStAsc(jichitaiCd);
				}
			}

			// 2. 読み込んだマスタを使って初期化
			setupMonthlyDetail(form, teigakuRates, teiritsuRates, paymentMonth);

			FukaMonthlyTallyDto tallyDto = new FukaMonthlyTallyDto();
			// 💡 カラム数を動的に確保（定額・定率で取得できた方のサイズを使う）
			int categorySize = "2".equals(form.getFukaKbn()) ? teiritsuRates.size() : teigakuRates.size();
			tallyDto.initialize(categorySize);
			form.setMonthlyTally(tallyDto);

			if (StringUtils.hasText(paymentMonth)) {
				restoreExistingDeclaration(form, shiteiNo, paymentMonth);
			}

		} catch (Exception e) {
			log.error("登録用データの取得中に致命的なエラーが発生しました。スタックトレースを確認せよ！", e);
		}
		return form;
	}

	/**
	 * 義務者情報を取得しフォームにセットする。
	 */
	private void setupObligorInfo(FukaDeclarationForm form, String shiteiNo) {
		List<Tokugimu> result = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
		result.stream()
				.findFirst()
				.ifPresent(tokugimu -> {
					tokugimu.getShisetsuName();
					form.setObligorName(tokugimu.getKyokaName());
					form.setFacilityName(tokugimu.getShisetsuName());
				});
	}

	/**
	 * 💡 【定額・定率両対応版】月計明細初期表示データの組み立て
	 * マスタから税率を読み込み、画面用フォームクラスのリスト構造を初期化します。
	 */
	private void setupMonthlyDetail(FukaDeclarationForm form, List<ZeiritsuTeigaku> teigakuRates,
			List<ZeiritsuTeiritsu> teiritsuRates, String paymentMonth) {
		FukaMonthlyDeclarationDto monthlyDetail = form.getMonthlyDetail();
		monthlyDetail.setPaymentYearMonth(paymentMonth);

		// 画面のリスト（taxDetails）をクリアして初期化
		monthlyDetail.setTaxDetails(new ArrayList<>());

		// ---------------------------------------------------------
		// 💡 【分岐追加】定率制（fukaKbn == "2"）の場合の動的初期化処理
		// ---------------------------------------------------------
		if ("2".equals(form.getFukaKbn())) {
			if (teiritsuRates != null) {
				for (ZeiritsuTeiritsu rate : teiritsuRates) {
					FukaTaxDetailDto teiritsuDetail = new FukaTaxDetailDto();

					teiritsuDetail.setZeiritsuSeq(rate.getSeq());
					// 💡 マスタの区分名（例：「一般宿泊」）をセット
					teiritsuDetail.setLabel(rate.getKbnName());

					// 💡 マスタの税率をそのまま（BigDecimal型で）セット。nullの場合は0として扱う。
					teiritsuDetail.setTaxRate(rate.getZeiRitsu() != null ? rate.getZeiRitsu() : BigDecimal.ZERO);

					// 初期表示時は数量・金額は null (または0)
					teiritsuDetail.setStayCount(null);
					teiritsuDetail.setTaxAmount(null);

					// リストへ追加
					monthlyDetail.getTaxDetails().add(teiritsuDetail);
				}
			}
			return; // 早期リターン（Early Return：条件を満たした時点で直ちにメソッドの処理を終了させる設計手法）
		}

		// ---------------------------------------------------------
		// 💡 【既存ロジック】定額制（fukaKbn == "1"）の場合のループ初期化
		// ---------------------------------------------------------
		if (teigakuRates != null) {
			for (ZeiritsuTeigaku rate : teigakuRates) {
				FukaTaxDetailDto detail = new FukaTaxDetailDto();
				detail.setZeiritsuSeq(rate.getSeq());
				detail.setTeigakuSeq(rate.getTeigakuSeq());
				detail.setTaxRate(rate.getZeigaku() != null ? BigDecimal.valueOf(rate.getZeigaku()) : BigDecimal.ZERO);
				detail.setLabel(rate.getRyokinSt() + "円以上");
				detail.setStayCount(null);
				detail.setTaxAmount(null);

				monthlyDetail.getTaxDetails().add(detail);
			}
		}
	}

	/**
	 * 既存の申告データを復元する。
	 */
	private void restoreExistingDeclaration(FukaDeclarationForm form, String shiteiNo, String paymentMonth) {
		String targetYm = paymentMonth.replace("-", "");
		fukaRepository.findFirstByJichitaiCdAndShiteiNoAndTaishoYmOrderByRnoDesc(jichitaiCd, shiteiNo, targetYm)
				.ifPresent(latestFuka -> {
					form.setModificationCategory("修正");
					FukaMonthlyDeclarationDto monthlyDetail = form.getMonthlyDetail();
					monthlyDetail.setExemptStayCount(latestFuka.getMenjoHakusu());
					monthlyDetail.setTotalStayCount(latestFuka.getTotalHakusu());
					monthlyDetail.setTotalPaymentAmount(latestFuka.getTotalZeigaku());

					// =========================================================
					// 💡 【追加】定率制（料金ベース）専用項目の復元（順マッピング）
					// =========================================================
					if ("2".equals(latestFuka.getFukaKbn())) {
						// 念のためフォーム側にも fukaKbn を明示的にセットして状態を安定させる
						form.setFukaKbn(latestFuka.getFukaKbn());
						form.setKazeiRyokin(latestFuka.getKazeiRyokin());

						// DB(Entity)側のカラム名「zeigaku」を、Form側の「teiritsuZeigaku」へマッピング
						form.setTeiritsuZeigaku(latestFuka.getZeigaku());

						form.setMenjoRyokin(latestFuka.getMenjoRyokin());
						form.setKazeiHakusu(latestFuka.getKazeiHakusu());
					}

					List<FukaUchi> uchiList = fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
							latestFuka.getJichitaiCd(), latestFuka.getShiteiNo(), latestFuka.getRno(),
							latestFuka.getNendo(), latestFuka.getKibetsu());

					syncUchiDataToForm(uchiList, monthlyDetail);
					// TODO: hydrateMonthlyTallyは現在のEntity構造に合わせて再設計が必要
					// hydrateMonthlyTally(form, latestFuka.getJichitaiCd(), latestFuka);
				});
	}

	/**
	 * 内訳データをフォームに同期する。
	 */
	private void syncUchiDataToForm(List<FukaUchi> uchiList, FukaMonthlyDeclarationDto dto) {
		if (uchiList == null || uchiList.isEmpty() || dto.getTaxDetails() == null) {
			return;
		}

		Map<Integer, FukaUchi> uchiMap = uchiList.stream()
				.filter(u -> u.getKazeiKbn() != null)
				.collect(Collectors.toMap(FukaUchi::getKazeiKbn, u -> u, (existing, replacement) -> existing));

		log.info("[syncUchiDataToForm] uchiMap keys={}, dto.taxDetails size={}", uchiMap.keySet(), dto.getTaxDetails().size());
		for (int i = 0; i < dto.getTaxDetails().size(); i++) {
			FukaTaxDetailDto formDetail = dto.getTaxDetails().get(i);
			FukaUchi matched = uchiMap.get(i + 1);

			log.info("[syncUchiDataToForm] i={}, matched={}, fukaKbn={}, hakusu={}, ryokin={}, zeigaku={}", i, (matched != null), (matched != null ? matched.getFukaKbn() : null), (matched != null ? matched.getHakusu() : null), (matched != null ? matched.getRyokin() : null), (matched != null ? matched.getZeigaku() : null));
				if (matched != null) {
				if ("2".equals(matched.getFukaKbn())) {
					// 定率制: hakusu=宿泊数, ryokin=課税対象宿泊料金
					formDetail.setStayCount(matched.getHakusu());
					formDetail.setKazeiRyokin(matched.getRyokin() != null ? matched.getRyokin().intValue() : null);
				} else {
					// 定額制: hakusu=宿泊数
					formDetail.setStayCount(matched.getHakusu());
				}
				formDetail.setTaxAmount(matched.getZeigaku());
			} else {
				formDetail.setStayCount(null);
				formDetail.setKazeiRyokin(null);
				formDetail.setTaxAmount(null);
			}
		}
	}

	/**
	 * 編集・照会用の表示データを取得する。
	 */
	@Transactional(readOnly = true)
	public FukaDeclarationForm getDeclarationFormForEdit(String shiteiNo, String nendo, Integer kibetsu) {
		FukaDeclarationForm form = new FukaDeclarationForm();
		form.setShiteiNo(shiteiNo);

		String currentJichitaiCd = getCurrentJichitaiCd();

		// 義務者情報（obligorName, facilityName）をセット
		setupObligorInfo(form, shiteiNo);

		fukaRepository
				.findFirstByJichitaiCdAndShiteiNoAndNendoAndKibetsuOrderByRnoDesc(currentJichitaiCd, shiteiNo, nendo,
						kibetsu)
				.ifPresent(entity -> {
					log.info("[getDeclarationFormForEdit] entity found: rno={}, fukaKbn={}, taishoYm={}, totalHakusu={}, totalZeigaku={}, kazeiRyokin={}", entity.getRno(), entity.getFukaKbn(), entity.getTaishoYm(), entity.getTotalHakusu(), entity.getTotalZeigaku(), entity.getKazeiRyokin());
					form.setRegistrationDate(entity.getShinkokuYmd());
					form.setNendo(entity.getNendo());
					form.setKibetsu(entity.getKibetsu());
					form.setFukaKbn(entity.getFukaKbn());
					form.setModificationCategory(entity.getHenkoKbn());
					form.setModificationReason(entity.getHenkoRiyu());

					if ("2".equals(entity.getFukaKbn())) {
						form.setKazeiRyokin(entity.getKazeiRyokin());
						form.setTeiritsuZeigaku(entity.getZeigaku());
						form.setMenjoRyokin(entity.getMenjoRyokin());
						form.setKazeiHakusu(entity.getKazeiHakusu());
					}

					hydrateAdditionalFields(entity, form);
					hydrateMonthlyDetail(entity, form, currentJichitaiCd);
					// TODO: hydrateMonthlyTallyは現在のEntity構造に合わせて再設計が必要
					// hydrateMonthlyTally(form, currentJichitaiCd, entity);
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
		if (!Boolean.TRUE.equals(form.isTaxCheckBypassed())) {
			if (hasTaxAmountDiscrepancy(form)) {
				// 不整合があれば、保存処理を中断してフラグを立てる
				form.setShowTaxWarningModal(true);
				return; // ⚠️ ここで処理を終了し、画面に戻す（保存されない）
			}
		}
		String currentJichitaiCd = getCurrentJichitaiCd();

		// 💡 追記：年度・期別の Null ガード（DBエラー防止の要だぜ）
		if (!StringUtils.hasText(form.getNendo()) || form.getKibetsu() == null) {
			String ym = form.getMonthlyDetail().getPaymentYearMonth();
			if (StringUtils.hasText(ym)) {
				form.setNendo(calculateNendo(ym));
				form.setKibetsu(calculateKibetsu(ym));
			}
		}

		String category = form.getModificationCategory();

		Integer targetRno = "2".equals(category)
				? getCurrentMaxRno(currentJichitaiCd, form.getShiteiNo(), form.getNendo(), form.getKibetsu())
				: determineNextRno(currentJichitaiCd, form.getShiteiNo(), form.getNendo(), form.getKibetsu());

		FukaMonthlyDeclarationDto dto = form.getMonthlyDetail();
		Fuka parentFuka = createParentFuka(form, dto, currentJichitaiCd);
		parentFuka.setRno(targetRno);

		// DTOからparentFukaへの値マッピング（NOT NULL制約違反防止：nullの場合はデフォルト値0を代入）
		if (dto != null) {
			parentFuka.setTotalHakusu(dto.getTotalStayCount() != null ? dto.getTotalStayCount() : 0);
			// kazeiRyokin: DTO直下にバインドがない場合、taxDetailsの各区分から合計を算出
                        Long kazeiRyokinTotal = 0L;
                        if (dto.getKazeiRyokin() != null && dto.getKazeiRyokin() > 0) {
                                kazeiRyokinTotal = dto.getKazeiRyokin();
                        } else if (dto.getTaxDetails() != null) {
                                kazeiRyokinTotal = dto.getTaxDetails().stream()
                                        .mapToLong(d -> d.getKazeiRyokin() != null ? d.getKazeiRyokin().longValue() : 0L)
                                        .sum();
                        }
                        parentFuka.setKazeiRyokin(kazeiRyokinTotal);
			// totalZeigaku: totalPaymentAmount > taxDetails[0].taxAmount > 0L
                        Long taxAmount = null;
			if (dto.getTotalPaymentAmount() != null && dto.getTotalPaymentAmount() > 0) {
				taxAmount = dto.getTotalPaymentAmount();
			}
			parentFuka.setTotalZeigaku(taxAmount != null ? taxAmount : 0L);
                        log.info("★★★ [saveDeclaration] totalZeigaku={}, taxAmount={}, nonyuKingaku={}, totalPaymentAmount={}, fukaKbn={}", parentFuka.getTotalZeigaku(), taxAmount, dto.getNonyuKingaku(), dto.getTotalPaymentAmount(), form.getFukaKbn());
			parentFuka.setZeigaku(dto.getNonyuKingaku() != null ? dto.getNonyuKingaku() : 0L);
		}

		List<FukaUchi> uchiList = createFukaUchiList(form, parentFuka, currentJichitaiCd);

		setAuditFields(parentFuka);
		if (!uchiList.isEmpty()) {
			uchiList.forEach(this::setAuditFields);
		}

		fukaRepository.save(parentFuka);
		if (!uchiList.isEmpty()) {
			fukaUchiRepository.saveAll(uchiList);
		}

		if (form.getMonthlyTally() != null) {
			// TODO: 月計表保存処理は現在のEntity構造に合わせて再設計が必要
			// saveChoshuGenboDataWithRno(form, parentFuka, currentJichitaiCd, targetRno);
		}
	}

	/**
	 * 指定された条件における最新のRNOを取得する。
	 */
	private Integer getCurrentMaxRno(String jichitaiCd, String shiteiNo, String nendo, Integer kibetsu) {
		return fukaRepository.findFirstByJichitaiCdAndShiteiNoAndNendoAndKibetsuOrderByRnoDesc(
				jichitaiCd, shiteiNo, nendo, kibetsu)
				.map(Fuka::getRno)
				.orElse(1);
	}

	/**
	 * エンティティに共通監査項目をセットする。
	 */
	private void setAuditFields(Object entity) {
		try {
			LocalDateTime now = LocalDateTime.now();
			String user = "system";

			invokeMethodIfexists(entity, "setAddDt", LocalDateTime.class, now);
			invokeMethodIfexists(entity, "setAddUser", String.class, user);
			invokeMethodIfexists(entity, "setUpdDt", LocalDateTime.class, now);
			invokeMethodIfexists(entity, "setUpdUser", String.class, user);
			invokeMethodIfexists(entity, "setVersion", Integer.class, INITIAL_VERSION);
		} catch (Exception e) {
			log.warn("共通項目のセット中にエラーが発生しました: {}", e.getMessage());
		}
	}

	/**
	 * メソッドが存在する場合にのみリフレクションで実行する。
	 */
	private void invokeMethodIfexists(Object obj, String methodName, Class<?> paramType, Object value) {
		try {
			obj.getClass().getMethod(methodName, paramType).invoke(obj, value);
		} catch (NoSuchMethodException e) {
			// メソッドが存在しない場合は何もしない
		} catch (Exception e) {
			log.error("メソッド実行エラー: {}", methodName, e);
		}
	}

	/**
	 * 内訳エンティティのリストを生成する。
	 */
	private List<FukaUchi> createFukaUchiList(FukaDeclarationForm form, Fuka parentFuka, String currentJichitaiCd) {
		List<FukaUchi> uchiList = new ArrayList<>();
		FukaMonthlyDeclarationDto dto = form.getMonthlyDetail();

		// =========================================================
		// 💡 【定率制（料金ベース）の早期リターン処理】
		// 定率制（fukaKbn == "2"）の場合はループを回さず、1件だけ内訳を作成して即座に返却する
		// =========================================================
		// 定額制・定率制統一ループ: taxDetailsの区分ごとに内訳レコードを作成
		FukaKbn kbn = FukaKbn.fromCode(form.getFukaKbn());
		for (int i = 0; i < dto.getTaxDetails().size(); i++) {
			FukaTaxDetailDto detail = dto.getTaxDetails().get(i);

			// 未入力(null or 0)の区分はスキップ
			if (detail.getStayCount() == null || detail.getStayCount() == 0) {
				continue;
			}

			FukaUchi uchi = new FukaUchi();
			uchi.setJichitaiCd(currentJichitaiCd);
			uchi.setShiteiNo(form.getShiteiNo());
			uchi.setNendo(parentFuka.getNendo());
			uchi.setKibetsu(parentFuka.getKibetsu());
			uchi.setRno(parentFuka.getRno());
			uchi.setKazeiKbn(i + 1);
			uchi.setFukaKbn(parentFuka.getFukaKbn());
			uchi.setZeiritsuSeq(detail.getZeiritsuSeq());

			// 定率制: stayCount=宿泊料金, taxAmount=税額 / 定額制: stayCount=宿泊数, taxAmount=税額
			if (kbn.isTeiritsu()) {
				uchi.setRyokin(detail.getKazeiRyokin() != null ? detail.getKazeiRyokin().longValue() : 0L);
				uchi.setRyokinSogaku(detail.getKazeiRyokin() != null ? detail.getKazeiRyokin().longValue() : 0L);
				uchi.setHakusu(detail.getStayCount() != null ? detail.getStayCount() : 0L);
			} else {
				uchi.setHakusu(detail.getStayCount() != null ? detail.getStayCount() : 0L);
			}

			long amount = (detail.getTaxAmount() != null) ? detail.getTaxAmount() : 0L;
			uchi.setZeigaku(amount);
			uchi.setCityZeigaku(amount);
			uchi.setKenZeigaku(0L);
			uchi.setZeiRitsu(detail.getTaxRate() != null ? detail.getTaxRate() : BigDecimal.ZERO);

			setAuditFields(uchi);
			uchiList.add(uchi);
		}
		return uchiList;
	}

	/**
	 * 指定された条件のデータが登録済みか判定する。
	 */
	public boolean isAlreadyRegistered(String shiteiNo, String paymentMonth) {
		if (!StringUtils.hasText(paymentMonth)) {
			return false;
		}
		String targetYm = paymentMonth.replace("-", "");
		return fukaRepository.findFirstByJichitaiCdAndShiteiNoAndTaishoYmOrderByRnoDesc(jichitaiCd, shiteiNo, targetYm)
				.isPresent();
	}

	/**
	 * 親エンティティを生成する。
	 */
	private Fuka createParentFuka(FukaDeclarationForm form, FukaMonthlyDeclarationDto dto,
			String jichitaiCd) {
		Fuka parentFuka = new Fuka();
		parentFuka.setJichitaiCd(jichitaiCd);
		parentFuka.setShiteiNo(form.getShiteiNo());

		String[] ym = dto.getPaymentYearMonth().split("-");
		int year = Integer.parseInt(ym[0]);
		int month = Integer.parseInt(ym[1]);

		int nendo = (month >= FISCAL_START_MONTH) ? year : year - 1;
		int kibetsu = (month >= FISCAL_START_MONTH) ? month - 3 : month + 9;

		parentFuka.setNendo(String.valueOf(nendo));
		parentFuka.setKibetsu(kibetsu);
		parentFuka.setTorokuYmd(form.getRegistrationDate() != null ? form.getRegistrationDate() : LocalDate.now());
		parentFuka.setShinkokuYmd(LocalDate.now());
		parentFuka.setFukaKbn(StringUtils.hasText(form.getFukaKbn()) ? form.getFukaKbn() : "1");
		parentFuka.setHenkoKbn(mapModificationCategory(form.getModificationCategory()));
		parentFuka.setHenkoRiyu(form.getModificationReason());
		parentFuka.setNewFlg(DEFAULT_NEW_FLG);
		parentFuka.setDelFlg(DEFAULT_DEL_FLG);
		parentFuka.setVersion(INITIAL_VERSION);
		parentFuka.setTaishoYm(dto.getPaymentYearMonth().replace("-", ""));
		parentFuka.setTotalHakusu(dto.getTotalStayCount() != null ? dto.getTotalStayCount() : 0);
		parentFuka.setTotalZeigaku(dto.getTotalPaymentAmount() != null ? dto.getTotalPaymentAmount() : 0L);
		parentFuka.setMenjoHakusu(dto.getExemptStayCount() != null ? dto.getExemptStayCount() : 0);
		parentFuka.setCityZeigaku(parentFuka.getTotalZeigaku());
		parentFuka.setKenZeigaku(0L);

		// 定率制（fukaKbn == '2'）の場合にフォームの定率制フィールドをエンティティにマッピングする
		if ("2".equals(form.getFukaKbn())) {
			// kazeiRyokin: form直下またはmonthlyDetailのどちらから取得（null安全）
			Long kazeiRyokinValue = form.getKazeiRyokin();
			if (kazeiRyokinValue == null) {
				kazeiRyokinValue = dto.getKazeiRyokin();
			}
			parentFuka.setKazeiRyokin(kazeiRyokinValue != null ? kazeiRyokinValue : 0L);
			parentFuka.setZeigaku(form.getTeiritsuZeigaku() != null ? form.getTeiritsuZeigaku() : 0L);
			parentFuka.setMenjoRyokin(form.getMenjoRyokin() != null ? form.getMenjoRyokin() : 0L);
			parentFuka.setKazeiHakusu(form.getKazeiHakusu() != null ? form.getKazeiHakusu() : 0L);
		}

		mapAdditionalFields(form, parentFuka);
		return parentFuka;
	}

	/**
	 * フォームのメタデータを再セットする。
	 */
	/**
	 * 画面表示に必要なメタデータをフォームに再セットする。
	 * バリデーションエラーによる再表示時、消失したラベル情報を復元（Hydration）する。
	 */
	public void hydrateFormMetadata(FukaDeclarationForm form) {
		if (form.getShiteiNo() == null) {
			return;
		}

		String jichitaiCd = getCurrentJichitaiCd();

		// 義務者情報の復元
		tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, form.getShiteiNo())
				.stream()
				.findFirst()
				.ifPresent(tokugimu -> {
					form.setFacilityName(tokugimu.getShisetsuName());
					form.setObligorName(tokugimu.getKyokaName());
				});

		List<FukaTaxDetailDto> formDetails = form.getMonthlyDetail().getTaxDetails();

		// =========================================================
		// 💡 修正箇所：定率制と定額制で復元（Hydration）のロジックを分ける
		// =========================================================
		if ("2".equals(form.getFukaKbn())) {
			// --- 定率制のラベル＆税率復元 ---
			String ym = form.getMonthlyDetail().getPaymentYearMonth();
			int targetYmInt = parseYmToInt(ym);
			
			// 対象年月から適用される税率マスタを再特定
			zeiritsuRepository.findActiveByJichitaiCd(jichitaiCd).stream()
				.filter(z -> "2".equals(z.getFukaKbn()))
				.filter(z -> {
					int st = parseYmToInt(z.getTekiyoStYm());
					int ed = parseYmToInt(z.getTekiyoEdYm());
					return (st == 0 || st <= targetYmInt) && (ed == 0 || targetYmInt <= ed);
				})
				.findFirst()
				.ifPresent(applied -> {
					List<ZeiritsuTeiritsu> mList = zeiritsuTeiritsuRepository
							.findByJichitaiCdAndSeqAndDelFlgOrderByTeiritsuSeqAsc(jichitaiCd, applied.getSeq(), "0");
					for (int i = 0; i < mList.size() && i < formDetails.size(); i++) {
						formDetails.get(i).setLabel(mList.get(i).getKbnName());
						formDetails.get(i).setTaxRate(mList.get(i).getZeiRitsu() != null ? mList.get(i).getZeiRitsu() : BigDecimal.ZERO);
					}
				});
		} else {
			// --- 定額制のラベル＆税率復元（以前直したもの） ---
			List<ZeiritsuTeigaku> masterRates = zeiritsuTeigakuRepository.findByJichitaiCdOrderByRyokinStAsc(jichitaiCd);
			for (int i = 0; i < masterRates.size() && i < formDetails.size(); i++) {
				ZeiritsuTeigaku master = masterRates.get(i);
				FukaTaxDetailDto detail = formDetails.get(i);
				String label = (master.getRyokinEd() != null)
						? String.format("%,d円 ～ %,d円未満", master.getRyokinSt(), master.getRyokinEd() + 1)
						: String.format("%,d円以上", master.getRyokinSt());
				detail.setLabel(label);
				detail.setTaxRate(master.getZeigaku() != null ? BigDecimal.valueOf(master.getZeigaku()) : BigDecimal.ZERO);
			}
		}
	}

	/**
	 * 変更区分をコード値に変換する。
	 */
	private String mapModificationCategory(String category) {
		if (!StringUtils.hasText(category)) {
			return "0";
		}
		return switch (category) {
		case "更生" -> "1";
		case "修正" -> "2";
		default -> "0";
		};
	}

	/**
	 * 次の履歴番号を決定する。
	 */
	private Integer determineNextRno(String jichitaiCd, String shiteiNo, String nendo, Integer kibetsu) {
		return fukaRepository.findFirstByJichitaiCdAndShiteiNoAndNendoAndKibetsuOrderByRnoDesc(
				jichitaiCd, shiteiNo, nendo, kibetsu)
				.map(Fuka::getRno)
				.map(rno -> rno + 1)
				.orElse(1);
	}

	/**
	 * 追加項目をエンティティにマッピングする。
	 */
	private void mapAdditionalFields(FukaDeclarationForm form, Fuka entity) {
		entity.setKasanKbn(form.getAdditionalCategory());
		if (StringUtils.hasText(form.getAdditionalRate())) {
			try {
				entity.setKasanRitsu(new java.math.BigDecimal(form.getAdditionalRate()));
			} catch (NumberFormatException e) {
				log.warn("加算割合の数値変換に失敗しました: {}", form.getAdditionalRate());
				entity.setKasanRitsu(null);
			}
		} else {
			entity.setKasanRitsu(null);
		}
		entity.setKasanGaku(form.getAdditionalAmount());
		entity.setNokigen(form.getAdditionalDueDate());
	}

	/**
	 * 月次明細サマリを復元する。（照会・編集画面用）
	 */
	private void hydrateMonthlyDetail(Fuka entity, FukaDeclarationForm form, String jichitaiCd) {
		FukaMonthlyDeclarationDto monthDto = new FukaMonthlyDeclarationDto();
		int calendarMonth = (entity.getKibetsu() <= 9) ? entity.getKibetsu() + 3 : entity.getKibetsu() - 9;
		int calendarYear = Integer.parseInt(entity.getNendo());
		if (calendarMonth < FISCAL_START_MONTH) {
			calendarYear++;
		}
		monthDto.setPaymentYearMonth(String.format("%s-%02d", calendarYear, calendarMonth));
		monthDto.setExemptStayCount(entity.getMenjoHakusu());
		monthDto.setTotalStayCount(entity.getTotalHakusu());
		monthDto.setTotalPaymentAmount(entity.getTotalZeigaku());
		monthDto.setKazeiRyokin(entity.getKazeiRyokin());

		// 💡 必須：税明細リストの器を確実に初期化
		monthDto.setTaxDetails(new ArrayList<>());

		// 💡 対象年月をフォーマット（例："202605"）
		String targetYm = String.format("%s%02d", calendarYear, calendarMonth);

		// =========================================================
		// 💡 【仕様書準拠】適用時期ベースのマスタ特定ロジック（照会・編集用）
		// =========================================================
		List<Zeiritsu> allZeiritsu = zeiritsuRepository.findActiveByJichitaiCd(jichitaiCd);
		Zeiritsu appliedZeiritsu = null;
		int targetYmInt = parseYmToInt(targetYm);

		log.info("[hydrateMonthlyDetail] targetYm={}, fukaKbn={}, masterCount={}", targetYm, entity.getFukaKbn(), allZeiritsu.size());

		for (Zeiritsu z : allZeiritsu) {
			if (!"2".equals(z.getTaishoKbn())) {
				continue;
			}
			if (!entity.getFukaKbn().equals(z.getFukaKbn())) {
				continue;
			}
			int stYmInt = parseYmToInt(z.getTekiyoStYm());
			int edYmInt = parseYmToInt(z.getTekiyoEdYm());

			boolean isAfterStart = (stYmInt == 0 || stYmInt <= targetYmInt);
			boolean isBeforeEnd = (edYmInt == 0 || targetYmInt <= edYmInt);

			if (isAfterStart && isBeforeEnd) {
				appliedZeiritsu = z;
				break;
			}
		}

		// appliedZeiritsu==null: fukaKbn"1"(teigaku) may have taishoKbn!="2", retry without taishoKbn filter
		if (appliedZeiritsu == null) {
			log.warn("[hydrateMonthlyDetail] taishoKbn=2 filter found no match. Retrying without taishoKbn filter. fukaKbn={}", entity.getFukaKbn());
			for (Zeiritsu z : allZeiritsu) {
				if (!entity.getFukaKbn().equals(z.getFukaKbn())) {
					continue;
				}
				int stYmInt2 = parseYmToInt(z.getTekiyoStYm());
				int edYmInt2 = parseYmToInt(z.getTekiyoEdYm());
				boolean isAfterStart2 = (stYmInt2 == 0 || stYmInt2 <= targetYmInt);
				boolean isBeforeEnd2 = (edYmInt2 == 0 || targetYmInt <= edYmInt2);
				if (isAfterStart2 && isBeforeEnd2) {
					appliedZeiritsu = z;
					log.info("[hydrateMonthlyDetail] Retry matched: seq={}, fukaKbn={}, taishoKbn={}", z.getSeq(), z.getFukaKbn(), z.getTaishoKbn());
					break;
				}
			}
		}

		// 【第2段階】 子マスタを取得して画面DTOに展開
		if (appliedZeiritsu != null) {
			if ("2".equals(appliedZeiritsu.getFukaKbn())) {
				// --- 定率制の復元 ---
				List<ZeiritsuTeiritsu> masterRates = zeiritsuTeiritsuRepository
						.findByJichitaiCdAndSeqAndDelFlgOrderByTeiritsuSeqAsc(jichitaiCd, appliedZeiritsu.getSeq(), "0");

				for (ZeiritsuTeiritsu m : masterRates) {
				    FukaTaxDetailDto d = new FukaTaxDetailDto();
				    d.setZeiritsuSeq(m.getSeq());
				    d.setLabel(m.getKbnName());
				    d.setTaxRate(m.getZeiRitsu() != null ? m.getZeiRitsu() : BigDecimal.ZERO); // ←ココを直す！
				    d.setStayCount(null);
				    d.setTaxAmount(null);
				    monthDto.getTaxDetails().add(d);
				}
			} else {
				// --- 定額制の復元 ---
				List<ZeiritsuTeigaku> masterRates = zeiritsuTeigakuRepository
						.findActiveBySeq(jichitaiCd, appliedZeiritsu.getSeq());
				for (ZeiritsuTeigaku m : masterRates) {
					FukaTaxDetailDto d = new FukaTaxDetailDto();
					d.setZeiritsuSeq(m.getSeq());
					d.setTeigakuSeq(m.getTeigakuSeq());
					d.setTaxRate(m.getZeigaku() != null ? BigDecimal.valueOf(m.getZeigaku()) : BigDecimal.ZERO);
					d.setLabel(m.getRyokinEd() != null
							? String.format("%,d円 ～ %,d円未満", m.getRyokinSt(), m.getRyokinEd() + 1)
							: String.format("%,d円以上", m.getRyokinSt()));
					monthDto.getTaxDetails().add(d);
				}
			}
		}

		// 【フォールバック】appliedZeiritsu==null かつ定額制: m_zeiritsu_teigakuから直接構築
		if (monthDto.getTaxDetails().isEmpty() && "1".equals(entity.getFukaKbn())) {
			log.warn("[hydrateMonthlyDetail] appliedZeiritsu is null for teigaku. Using direct teigaku master query.");
			List<ZeiritsuTeigaku> fallbackRates = zeiritsuTeigakuRepository.findByJichitaiCdOrderByRyokinStAsc(jichitaiCd);
			for (ZeiritsuTeigaku m : fallbackRates) {
				FukaTaxDetailDto d = new FukaTaxDetailDto();
				d.setZeiritsuSeq(m.getSeq());
				d.setTeigakuSeq(m.getTeigakuSeq());
				d.setTaxRate(m.getZeigaku() != null ? BigDecimal.valueOf(m.getZeigaku()) : BigDecimal.ZERO);
				d.setLabel(m.getRyokinEd() != null
						? String.format("%,d円 ～ %,d円未満", m.getRyokinSt(), m.getRyokinEd() + 1)
						: String.format("%,d円以上", m.getRyokinSt()));
				monthDto.getTaxDetails().add(d);
			}
		}

		form.setMonthlyDetail(monthDto);

		// 💡 内訳テーブル（t_fuka_uchi）から上書き同期を行う
		List<FukaUchi> uchiList = fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
				jichitaiCd, form.getShiteiNo(), entity.getRno(), entity.getNendo(), entity.getKibetsu());

		log.info("[hydrateMonthlyDetail] uchiList size={}, taxDetails size={}, rno={}, nendo={}, kibetsu={}", uchiList.size(), monthDto.getTaxDetails().size(), entity.getRno(), entity.getNendo(), entity.getKibetsu());
		syncUchiDataToForm(uchiList, monthDto);
	}

	/**
	 * 加算金項目を復元する。
	 */
	private void hydrateAdditionalFields(Fuka entity, FukaDeclarationForm form) {
		form.setAdditionalCategory(entity.getKasanKbn());
		if (entity.getKasanRitsu() != null) {
			form.setAdditionalRate(entity.getKasanRitsu().toString());
		}
		form.setAdditionalAmount(entity.getKasanGaku());
		form.setAdditionalDueDate(entity.getNokigen());

		// 定率制（fukaKbn == '2'）の場合にエンティティからフォームへ復元（DB → フォーム）
		if ("2".equals(entity.getFukaKbn())) {
			form.setKazeiRyokin(entity.getKazeiRyokin());
			form.setTeiritsuZeigaku(entity.getZeigaku());
			form.setMenjoRyokin(entity.getMenjoRyokin());
			form.setKazeiHakusu(entity.getKazeiHakusu());
		}
	}

	/**
	 * 申告入力された税額と、システム計算上の税額に不整合がないか判定する。
	 * @param form 申告フォーム
	 * @return true: 不整合あり / false: 不整合なし
	 */
	public boolean hasTaxAmountDiscrepancy(FukaDeclarationForm form) {
		FukaMonthlyDeclarationDto detail = form.getMonthlyDetail();
		if (detail == null || detail.getTaxDetails() == null || detail.getTaxDetails().isEmpty()) {
			return false;
		}

		FukaKbn kbn = FukaKbn.fromCode(form.getFukaKbn());

		// 明細に1件でも入力があるか確認（全未入力ならチェック不要）
		boolean hasInput;
		if (kbn.isTeiritsu()) {
			hasInput = detail.getTaxDetails().stream()
					.anyMatch(d -> d.getKazeiRyokin() != null && d.getKazeiRyokin() > 0);
		} else {
			hasInput = detail.getTaxDetails().stream()
					.anyMatch(d -> d.getStayCount() != null && d.getStayCount() > 0);
		}
		if (!hasInput) {
			return false;
		}

		// 期待される税額を全区分ループで計算
		long calculatedTotal = 0L;
		for (FukaTaxDetailDto d : detail.getTaxDetails()) {
			BigDecimal rate = (d.getTaxRate() != null) ? d.getTaxRate() : BigDecimal.ZERO;
			if (kbn.isTeiritsu()) {
				long ryokin = (d.getKazeiRyokin() != null) ? d.getKazeiRyokin().longValue() : 0L;
				calculatedTotal += kbn.calculateTax(rate, ryokin);
			} else {
				long count = (d.getStayCount() != null) ? d.getStayCount() : 0L;
				calculatedTotal += kbn.calculateTax(rate, count);
			}
		}

		// 画面の合計税額を取得
		long inputTotal = (detail.getTotalPaymentAmount() != null) ? detail.getTotalPaymentAmount() : 0L;

		if (calculatedTotal != inputTotal) {
			log.warn("税額整合性エラー: 算出値={}, 画面入力値={}, fukaKbn={}", calculatedTotal, inputTotal, form.getFukaKbn());
			return true;
		}
		return false;
	}

	/**
	 * 指定された年度・期別に該当する申告データが存在するか判定する。
	 */
	public boolean isAlreadyRegisteredByKibetsu(String shiteiNo, String nendo, Integer kibetsu) {
		// 初期履歴番号(1)のデータが存在するかを確認する
		FukaId fukaId = new FukaId(jichitaiCd, shiteiNo, INITIAL_VERSION, nendo, kibetsu);
		return fukaRepository.findById(fukaId).isPresent();
	}

	/**
	 * 年月文字列を数値（int）に正規化する。
	 * 空白・ハイフン・スラッシュを除去し、6桁の整数として返す。
	 * null/空/パース不能の場合は 0 を返す（0は「制限なし」として扱う）。
	 */
	private int parseYmToInt(String ym) {
		if (ym == null || ym.trim().isEmpty()) {
			return 0;
		}
		try {
			String cleaned = ym.trim().replace("-", "").replace("/", "").replace(" ", "");
			return Integer.parseInt(cleaned);
		} catch (NumberFormatException e) {
			log.warn("年月のパースに失敗しました（0として扱います）: '{}'", ym);
			return 0;
		}
	}
	/**
	 * 💡 納入年月(yyyy-MM)から年度を算出する。
	 */
	private String calculateNendo(String paymentYearMonth) {
		String[] ym = paymentYearMonth.split("-");
		int year = Integer.parseInt(ym[0]);
		int month = Integer.parseInt(ym[1]);
		return String.valueOf(month >= FISCAL_START_MONTH ? year : year - 1);
	}
	/**
	 * 💡 納入年月(yyyy-MM)から期別を算出する。
	 */
	private Integer calculateKibetsu(String paymentYearMonth) {
		String[] ym = paymentYearMonth.split("-");
		int month = Integer.parseInt(ym[1]);
		return month >= FISCAL_START_MONTH ? month - 3 : month + 9;
	}
}

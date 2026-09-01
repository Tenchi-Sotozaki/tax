package jp.lg.asp.accommodation.service.impl;
import java.time.LocalDate;
import java.time.chrono.JapaneseDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.constant.FukaConstants;
import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.KoseiKetteiTsuchiReportsDto;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.FukaUchi;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.entity.ZeiritsuTeiritsu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.FukaUchiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeiritsuRepository;
import jp.lg.asp.accommodation.service.KoseiKetteiTsuchiReportsService;
import jp.lg.asp.accommodation.service.ReportsCommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * 更正・決定通知書 ReportsService実装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KoseiKetteiTsuchiReportsServiceImpl implements KoseiKetteiTsuchiReportsService {

	private final FukaRepository fukaRepository;
	private final FukaUchiRepository fukaUchiRepository;
	private final TokugimuRepository tokugimuRepository;
	private final AtenaRepository atenaRepository;
	private final ReportsCommonService reportsCommonService;
	private final ZeiritsuTeiritsuRepository zeiritsuTeiritsuRepository; // ← 追加

	private final JichitaiContext jichitaiContext;

	private String cityName;
	private String todoufuken;
	private String horeiInyou1;
	private String horeiInyou2;
	private byte[] koin;

	/** 課税区分最大数 */
	private static final int MAX_KBN = 5;

	/** 定率JRXMLパス */
	private static final String JRXML_TEIRITSU = "reports/kouseiKetteiTsuchisho_teiritsu.jrxml";
	/** 定額JRXMLパス */
	private static final String JRXML_TEIGAKU = "reports/kouseiKetteiTsuchisho_teigaku.jrxml";

	/**
	 * 起動時初期化（フォント設定・自治体情報・法令引用文キャッシュ）
	 */
	private void init() {
		System.setProperty("net.sf.jasperreports.default.font.name", "IPAex明朝");
		System.setProperty("net.sf.jasperreports.awt.ignore.missing.font", "true");
		Jichitai jichitai = reportsCommonService.getJichitaiInfo();
		cityName     = jichitai != null ? jichitai.getName() + jichitai.getKbnName() : "";
		todoufuken   = jichitai != null ? jichitai.getKbnName() : "";
		horeiInyou1  = reportsCommonService.getReportsDefText(ReportsConstants.KOSEI_KETTEI_HOREI_INYOU1);
		horeiInyou2  = reportsCommonService.getReportsDefText(ReportsConstants.KOSEI_KETTEI_HOREI_INYOU2);
		koin         = reportsCommonService.getReportsDefData(ReportsConstants.KOIN);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public byte[] generatePdf(String shiteiNo, String b1Ym, String b2Ym, String b3Ym, String henkoKbn) {
		init();
		KoseiKetteiTsuchiReportsDto dto = null;
		try {
			// 対象月の情報をもとに帳票用DTOを構築
			dto = buildDtoByTaishoYm(shiteiNo, b1Ym, b2Ym, b3Ym, henkoKbn);
			log.debug("PDF生成開始 - 指定番号: {}, b1Ym: {}, b2Ym: {}, b3Ym: {}", shiteiNo, b1Ym, b2Ym, b3Ym);

			String fukaKbn = dto.getFukaKbn();
			if (!FukaConstants.TEIRITSU.getValue().equals(fukaKbn)
					&& !FukaConstants.TEIGAKU.getValue().equals(fukaKbn)) {
				throw new RuntimeException("未知の賦課区分です: " + fukaKbn);
			}
			String jrxmlPath = FukaConstants.TEIRITSU.getValue().equals(fukaKbn)
					? JRXML_TEIRITSU
					: JRXML_TEIGAKU;

			ClassPathResource resource = new ClassPathResource(jrxmlPath);
			if (!resource.exists()) {
				throw new RuntimeException("JRXMLファイルが見つかりません: " + jrxmlPath);
			}

			// JasperReportsのテンプレートをコンパイル
			JasperReport jasperReport = JasperCompileManager.compileReport(resource.getInputStream());
			log.debug("JRXMLコンパイル完了: {}", jrxmlPath);

			// 帳票に渡すパラメータを設定
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("city", cityName);
			parameters.put("horei_inyou1", horeiInyou1);
			parameters.put("horei_inyou2", horeiInyou2);
			
			// DTOをコレクションDataSourceとしてラップ
			JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(Arrays.asList(dto));

			// 帳票データの流し込み（Fill）とPDFのエクスポート
			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
			byte[] pdfData = JasperExportManager.exportReportToPdf(jasperPrint);
			log.debug("PDF出力完了 - サイズ: {} bytes", pdfData.length);

			return pdfData;

		} catch (Exception e) {
			log.error("PDF生成に失敗しました - 指定番号: {}", dto != null ? dto.getShitei_no() : shiteiNo, e);
			throw new RuntimeException("PDF生成に失敗しました: " + e.getMessage(), e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public List<String> findTaishoYmList(String shiteiNo) {
		init();
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		// 指定番号に紐づく対象年月の一覧を取得する
		return fukaRepository.findTaishoYmListByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
	}

	/**
	 * 指定番号と対象月からFukaエンティティを検索する
	 */
	private Optional<Fuka> findFukaByTaishoYm(String shiteiNo, String taishoYm) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		List<Fuka> fukaList = fukaRepository
				.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
						jichitaiCd, shiteiNo, taishoYm.substring(0, 4));
		return fukaList.stream()
				.filter(f -> taishoYm.equals(f.getTaishoYm()))
				.findFirst();
	}

	/**
	 * 対象年月（b1/b2/b3）を元にFukaデータを特定し、通知書用DTOを構築する
	 */
	@Transactional(readOnly = true)
	private KoseiKetteiTsuchiReportsDto buildDtoByTaishoYm(
			String shiteiNo, String b1Ym, String b2Ym, String b3Ym, String henkoKbn) {

		KoseiKetteiTsuchiReportsDto dto = new KoseiKetteiTsuchiReportsDto();

		// 通知日（システム日付）を和暦文字列（例: 令和8年8月20日）に変換してDTOにセット
		LocalDate today = LocalDate.now();
		JapaneseDate japaneseDate = JapaneseDate.from(today);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("Gy年M月d日", new Locale("ja", "JP", "JP"));
		dto.setTsuchi_Ymd(formatter.format(japaneseDate));
		
		// 各ブロックの対象年月配列
		String[] ymArr = { b1Ym, b2Ym, b3Ym };
		boolean fukaKbnSet = false;
		Fuka firstFuka = null;

		// b1〜b3の各ブロックに対してデータを設定
		for (int i = 0; i < ymArr.length; i++) {
			String taishoYm = ymArr[i];
			int blockNo = i + 1;

			// 対象月が未指定の場合はブロックを空文字で初期化
			if (taishoYm == null || taishoYm.trim().isEmpty()) {
				setBlockEmpty(dto, blockNo);
				continue;
			}

			// 対象月に一致するFukaデータを検索
			Optional<Fuka> fukaOpt = findFukaByTaishoYm(shiteiNo, taishoYm);
			if (fukaOpt.isEmpty()) {
				setBlockEmpty(dto, blockNo);
				continue;
			}

			Fuka fuka = fukaOpt.get();
			// 最初にヒットしたデータの賦課区分や変更理由などを代表値として保持
			if (!fukaKbnSet) {
				dto.setFukaKbn(fuka.getFukaKbn());
				dto.setHenko_riyu(nvl(fuka.getHenkoRiyu()));
				firstFuka = fuka;
				fukaKbnSet = true;
			}
			
			dto.setShitei_no(shiteiNo);
			// 各期別ブロックの数値をセット
			setKibetsuBlockByFuka(dto, fuka, blockNo, henkoKbn);
		}

		// 賦課区分が特定できなかった場合のデフォルト値設定（定額）
		if (!fukaKbnSet) {
			dto.setFukaKbn(FukaConstants.TEIGAKU.getValue());
		}

		// 納入税額・加算金・納期限などの共通項目を設定
		if (firstFuka != null) {
			setNofuAndKasan(dto, shiteiNo, firstFuka, ymArr, henkoKbn);
			dto.setHenko_kbn(henkoKbn != null && !henkoKbn.isEmpty() ? henkoKbn : firstFuka.getHenkoKbn());
		}

		// 自治体の都道府県名と公印データをセット
		dto.setTodoufuken(todoufuken);

		// 公印
		dto.setKoin(koin != null && koin.length > 0 ? koin : null);

		return dto;
	}

	/**
	 * Fukaエンティティを元に期別ブロック（b1/b2/b3）の各項目をDTOに設定する
	 */
	private void setKibetsuBlockByFuka(KoseiKetteiTsuchiReportsDto dto, Fuka fuka, int blockNo, String henkoKbn) {
        String jichitaiCd = jichitaiContext.getJichitaiCd();
        String pfx = "b" + blockNo + "_"; // ブロックごとのフィールド接頭辞（b1_, b2_, b3_）

        String taishoYm = nvl(fuka.getTaishoYm());
        String nen = taishoYm.length() == 6 ? taishoYm.substring(0, 4) : "";
        
        // 判定フラグの定義
        boolean isTeigaku = FukaConstants.TEIGAKU.getValue().equals(fuka.getFukaKbn());
        boolean isTeiritsu = FukaConstants.TEIRITSU.getValue().equals(fuka.getFukaKbn());
        
		// 対象月の年・月をDTOに設定
		setField(dto, pfx + "nen", !nen.isEmpty() ? nen : "");
		setField(dto, pfx + "tsuki", (taishoYm.length() == 6) ? taishoYm.substring(4, 6) : "");

        // 履歴番号（Rno）の取得
        Integer rno = fukaRepository.findMaxRno(
                jichitaiCd, fuka.getShiteiNo(), fuka.getNendo(), fuka.getKibetsu()).orElse(1);

        // 現在の内訳リストを取得
        List<FukaUchi> uchiList = fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                jichitaiCd, fuka.getShiteiNo(), rno, fuka.getNendo(), fuka.getKibetsu());

        // 更正の場合は変更区分「申告」の最新rnoを取得して差引計算に使用
        boolean isKosei = FukaConstants.KOSEI.getValue().equals(fuka.getHenkoKbn());
        List<FukaUchi> prevUchiList = Collections.emptyList();
        if (isKosei) {
            Optional<Integer> shinkokuRno = fukaRepository.findMaxRnoByHenkoKbn(
                    jichitaiCd, fuka.getShiteiNo(), fuka.getNendo(), fuka.getKibetsu(),
                    FukaConstants.SHINKOKU.getValue());
            if (shinkokuRno.isPresent()) {
                prevUchiList = fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                        jichitaiCd, fuka.getShiteiNo(), shinkokuRno.get(), fuka.getNendo(), fuka.getKibetsu());
            }
        }

        // 課税区分（1〜5）ごとの内訳項目をセット
        for (int kbn = 1; kbn <= MAX_KBN; kbn++) {
            final int k = kbn;
            Optional<FukaUchi> uchiOpt = uchiList.stream().filter(u -> k == u.getKazeiKbn()).findFirst();

            long sogaku = 0L, ryokin = 0L, zeigaku = 0L, hakusu = 0L;
            if (uchiOpt.isPresent()) {
                FukaUchi u = uchiOpt.get();
                sogaku  = u.getRyokinSogaku() != null ? u.getRyokinSogaku() : 0L;
                ryokin  = u.getRyokin() != null ? u.getRyokin() : 0L;
                zeigaku = u.getZeigaku() != null ? u.getZeigaku() : 0L;
                hakusu  = u.getHakusu() != null ? u.getHakusu() : 0L;
            }

            // 前回申告等の税額を取得し、差引増減額を算出
            long prevZeigaku = (prevUchiList != null && !prevUchiList.isEmpty()) ? 
            		prevUchiList.stream().filter(u -> k == u.getKazeiKbn())
                    .mapToLong(u -> u.getZeigaku() != null ? u.getZeigaku() : 0L).findFirst().orElse(0L)
                    : 0L;
            long sashihiki = zeigaku - prevZeigaku;
           
            // 各フィールドに数値をセット
            setField(dto, pfx + "sogaku" + kbn, String.valueOf(sogaku));
            setField(dto, pfx + "ryokin" + kbn, String.valueOf(ryokin));
            setField(dto, pfx + "zeigaku" + kbn, String.valueOf(zeigaku));
            setField(dto, pfx + "sashihiki" + kbn, String.valueOf(sashihiki));
            setField(dto, pfx + "hakusu" + kbn, nen.isEmpty() ? "" : String.valueOf(hakusu));
            setField(dto, pfx + "kino_zeigaku" + kbn, nen.isEmpty() ? "" : String.valueOf(prevZeigaku));
         
			// 【税率（zei_ritsu）の区分別設定】
            String rate = uchiOpt.map(u -> u.getZeiRitsu() != null ? u.getZeiRitsu().stripTrailingZeros().toPlainString() : "")
                    .orElse("");
            setField(dto, pfx + "zei_ritsu" + kbn, rate);
        }

        // 各種合計値の計算用変数
        long hakusuSum = 0L, zeigakuSum = 0L, sashihikiSum = 0L, kbnZeiGakuSum = 0L, kinoZeigakuSum = 0L;
        
        for (int kbn = 1; kbn <= MAX_KBN; kbn++) {
            final int k = kbn;
            Optional<FukaUchi> u = uchiList.stream().filter(x -> k == x.getKazeiKbn()).findFirst();
            if (u.isPresent()) {
                hakusuSum  += u.get().getHakusu()  != null ? u.get().getHakusu()  : 0L;
                zeigakuSum += u.get().getZeigaku() != null ? u.get().getZeigaku() : 0L;
            }
            
            long zeigaku = u.map(x -> x.getZeigaku() != null ? x.getZeigaku() : 0L).orElse(0L);
            long prevZeigaku = prevUchiList.stream()
                    .filter(x -> k == x.getKazeiKbn())
                    .mapToLong(x -> x.getZeigaku() != null ? x.getZeigaku() : 0L)
                    .findFirst().orElse(0L);
            sashihikiSum  += (zeigaku - prevZeigaku);
            kinoZeigakuSum += prevZeigaku;

            if (isTeigaku) {
                long hakusu = u.map(x -> x.getHakusu() != null ? x.getHakusu() : 0L).orElse(0L);
                if (!nen.isEmpty() && u.isPresent() && hakusu > 0) {
                    kbnZeiGakuSum += (zeigaku / hakusu);
                }
            }
        }

        // 合計値をDTOにセット
        setField(dto, pfx + "hakusu_sum",    nen.isEmpty() ? "" : String.valueOf(hakusuSum));
        setField(dto, pfx + "zeigaku_sum",   String.valueOf(zeigakuSum));
        setField(dto, pfx + "sashihiki_sum", String.valueOf(sashihikiSum));
        setField(dto, pfx + "kino_zeigaku_sum", nen.isEmpty() ? "" : String.valueOf(kinoZeigakuSum));
        
        // 【定額・定率ごとの追加情報処理】
        if (isTeigaku) {
            // 定額の場合の合計税率処理
            if (kbnZeiGakuSum > 0) {
                setField(dto, pfx + "zei_ritsu_sum", String.valueOf(kbnZeiGakuSum));
            } else {
                setField(dto, pfx + "zei_ritsu_sum", "");
            }
        } else if (isTeiritsu) {
            // 定率の場合の区分名取得処理
            uchiList.stream()
                .map(FukaUchi::getZeiritsuSeq)
                .filter(seq -> seq != null)
                .findFirst()
                .ifPresentOrElse(zeiritsuSeq -> {
                    List<ZeiritsuTeiritsu> teiritsuList =
                        zeiritsuTeiritsuRepository.findActiveBySeq(jichitaiCd, zeiritsuSeq);
                    for (ZeiritsuTeiritsu t : teiritsuList) {
                        int tSeq = t.getTeiritsuSeq().intValue();
                        if (tSeq >= 1 && tSeq <= MAX_KBN) {
                            setField(dto, "kbn_name" + tSeq, nvl(t.getKbnName()).trim());
                        }
                    }
                }, () -> zeiritsuTeiritsuRepository.findActiveBySeq(jichitaiCd, null));
        }
        
		// 特別徴収義務者情報を取得してDTOにセット
		List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, dto.getShitei_no());
		if (tokugimuList.isEmpty()) {
			log.error("t_tokugimuが見つかりません: shiteiNo={}", dto.getShitei_no());
		}

		Tokugimu toku = tokugimuList.get(0);
		dto.setShitei_no(toku.getShiteiNo());
		dto.setShisetsu_yubin_no("〒" + nvl(toku.getShisetsuYubinNo()));
		dto.setShisetsu_jusho(nvl(toku.getShisetsuJusho()));
		dto.setShisetsu_name(nvl(toku.getShisetsuName()));

		// 宛先情報を取得してDTOにセット
		atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, toku.getAtenaNo())
				.ifPresent(atena -> {
					dto.setYubin_no("〒" + nvl(atena.getYubinNo()));
					dto.setJusho(nvl(atena.getJusho()));
					dto.setName(nvl(atena.getName()));
				});
    }
	
	/**
	 * 納入税額・加算金・納期限を計算してDTOに設定する
	 */
	private void setNofuAndKasan(
			KoseiKetteiTsuchiReportsDto dto, String shiteiNo, Fuka firstFuka, String[] ymArr, String henkoKbn) {

		String jichitaiCd = jichitaiContext.getJichitaiCd();
		long totalSashihikiSum = 0L;

		// 全対象月の差引増減額を合算してトータルの納入税額を算出
		for (String taishoYm : ymArr) {
			if (taishoYm == null || taishoYm.isEmpty()) {
				continue;
			}

			Optional<Fuka> fukaOpt = findFukaByTaishoYm(shiteiNo, taishoYm);
			if (fukaOpt.isEmpty()) {
				continue;
			}

			Fuka fuka = fukaOpt.get();
			Integer rno = fukaRepository.findMaxRno(
					jichitaiCd, fuka.getShiteiNo(), fuka.getNendo(), fuka.getKibetsu()).orElse(1);

			List<FukaUchi> uchiList = fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
					jichitaiCd, fuka.getShiteiNo(), rno, fuka.getNendo(), fuka.getKibetsu());

			boolean isKosei = FukaConstants.KOSEI.getValue().equals(henkoKbn);
			List<FukaUchi> prevUchiList = Collections.emptyList();
			if (isKosei) {
				Optional<Integer> shinkokuRno = fukaRepository.findMaxRnoByHenkoKbn(
						jichitaiCd, fuka.getShiteiNo(), fuka.getNendo(), fuka.getKibetsu(),
						FukaConstants.SHINKOKU.getValue());
				if (shinkokuRno.isPresent()) {
					prevUchiList = fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
							jichitaiCd, fuka.getShiteiNo(), shinkokuRno.get(), fuka.getNendo(), fuka.getKibetsu());
				}
			}

			long blockSashihikiSum = 0L;
			for (int kbn = 1; kbn <= MAX_KBN; kbn++) {
				final int k = kbn;
				long zeigaku = uchiList.stream()
						.filter(u -> k == u.getKazeiKbn())
						.mapToLong(u -> u.getZeigaku() != null ? u.getZeigaku() : 0L)
						.findFirst().orElse(0L);

				long prevZeigaku = prevUchiList.stream()
						.filter(u -> k == u.getKazeiKbn())
						.mapToLong(u -> u.getZeigaku() != null ? u.getZeigaku() : 0L)
						.findFirst().orElse(0L);

				blockSashihikiSum += (zeigaku - prevZeigaku);
			}
			totalSashihikiSum += blockSashihikiSum;
		}

		// 算出された納入税額をセット
		dto.setNofu_zeigaku(String.valueOf(totalSashihikiSum));

		// 加算金の利率・金額・区分をセット
		dto.setKasan_ritsu1(firstFuka.getKasanRitsu1() != null ? firstFuka.getKasanRitsu1().toPlainString() : "");
		dto.setKasan_gaku1(firstFuka.getKasanGaku1() != null ? String.valueOf(firstFuka.getKasanGaku1()) : "");
		dto.setKasan_ritsu2(firstFuka.getKasanRitsu2() != null ? firstFuka.getKasanRitsu2().toPlainString() : "");
		dto.setKasan_gaku2(firstFuka.getKasanGaku2() != null ? String.valueOf(firstFuka.getKasanGaku2()) : "");
		dto.setKasan_ritsu3(firstFuka.getKasanRitsu3() != null ? firstFuka.getKasanRitsu3().toPlainString() : "");
		dto.setKasan_gaku3(firstFuka.getKasanGaku3() != null ? String.valueOf(firstFuka.getKasanGaku3()) : "");
		
		dto.setKasan_kbn1(nvl(firstFuka.getKasanKbn1()));
		dto.setKasan_kbn2(nvl(firstFuka.getKasanKbn2()));
		dto.setKasan_kbn3(nvl(firstFuka.getKasanKbn3()));

		// 納期限をセット
		if (firstFuka.getNokigen() != null) {
			// LocalDate から和暦（JapaneseDate）に変換
		    JapaneseDate japaneseDate = JapaneseDate.from(firstFuka.getNokigen());
		    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("Gy");
		    
		    dto.setNofu_kigen_nen(formatter.format(japaneseDate));
		    dto.setNofu_kigen_tsuki(String.valueOf(firstFuka.getNokigen().getMonthValue()));
		    dto.setNofu_kigen_hi(String.valueOf(firstFuka.getNokigen().getDayOfMonth()));
		} else {
		    dto.setNofu_kigen_nen("");
		    dto.setNofu_kigen_tsuki("");
		    dto.setNofu_kigen_hi("");
		}
	}

    /**
     * 指定ブロックのDTOフィールドを全て空文字に初期化する
     */
    private void setBlockEmpty(KoseiKetteiTsuchiReportsDto dto, int blockNo) {
        String pfx = "b" + blockNo + "_";
        setField(dto, pfx + "nen",   "");
        setField(dto, pfx + "tsuki", "");

        for (int i = 1; i <= MAX_KBN; i++) {
            setField(dto, pfx + "sogaku" + i, "");
            setField(dto, pfx + "ryokin" + i, "");
            setField(dto, pfx + "zeigaku" + i, "");
            setField(dto, pfx + "sashihiki" + i, "");
            setField(dto, pfx + "hakusu" + i, "");
            setField(dto, pfx + "kino_zeigaku" + i, "");
            setField(dto, pfx + "zei_ritsu" + i, "");
            setField(dto, pfx + "kbn_name" + i, "");
        }
        
        setField(dto, pfx + "sogaku_sum",    "");
        setField(dto, pfx + "ryokin_sum",    "");
        setField(dto, pfx + "zeigaku_sum",   "");
        setField(dto, pfx + "sashihiki_sum", "");
        setField(dto, pfx + "hakusu_sum",    "");
        setField(dto, pfx + "kino_zeigaku_sum", "");
        setField(dto, pfx + "zei_ritsu_sum", "");
    }

	/**
	 * リフレクションを利用してDTOのフィールドに値を動的にセットする
	 */
	private void setField(KoseiKetteiTsuchiReportsDto dto, String field, String value) {
		try {
			java.lang.reflect.Field f = dto.getClass().getDeclaredField(field);
			f.setAccessible(true);
			f.set(dto, value);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			log.error("フィールドが見つかりません: {}", field);
		}
	}

	/**
	 * null安全な文字列変換（nullの場合は空文字を返す）
	 */
	private String nvl(String value) {
		return value != null ? value : "";
	}
}
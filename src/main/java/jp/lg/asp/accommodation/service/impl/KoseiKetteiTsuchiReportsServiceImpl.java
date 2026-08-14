package jp.lg.asp.accommodation.service.impl;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
		cityName = jichitai.getName();
		todoufuken = jichitai.getKbnName();
		horeiInyou1 = reportsCommonService.getReportsDefText(ReportsConstants.KOSEI_KETTEI_HOREI_INYOU1);
		horeiInyou2 = reportsCommonService.getReportsDefText(ReportsConstants.KOSEI_KETTEI_HOREI_INYOU2);
		koin = reportsCommonService.getReportsDefData(ReportsConstants.KOIN);
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
			dto = buildDtoByTaishoYm(shiteiNo, b1Ym, b2Ym, b3Ym, henkoKbn);
			log.debug("PDF生成開始 - 指定番号: {}, b1Ym: {}, b2Ym: {}, b3Ym: {}", shiteiNo, b1Ym, b2Ym, b3Ym);

			String jrxmlPath = FukaConstants.TEIRITSU.getValue().equals(dto.getFukaKbn())
					? JRXML_TEIRITSU
					: JRXML_TEIGAKU;

			ClassPathResource resource = new ClassPathResource(jrxmlPath);
			if (!resource.exists()) {
				throw new RuntimeException("JRXMLファイルが見つかりません: " + jrxmlPath);
			}

			JasperReport jasperReport = JasperCompileManager.compileReport(resource.getInputStream());
			log.debug("JRXMLコンパイル完了: {}", jrxmlPath);

			Map<String, Object> parameters = new HashMap<>();
			parameters.put("city", cityName);
			parameters.put("horei_inyou1", horeiInyou1);
			parameters.put("horei_inyou2", horeiInyou2);
			JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(Arrays.asList(dto));

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
		return fukaRepository.findTaishoYmListByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public KoseiKetteiTsuchiReportsDto buildDtoForDisplay(String shiteiNo) {
		init();
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		KoseiKetteiTsuchiReportsDto dto = new KoseiKetteiTsuchiReportsDto();
		dto.setShitei_no(shiteiNo);

		tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
				jichitaiCd, shiteiNo, "1", "0")
				.ifPresent(toku -> {
					dto.setShisetsu_name(nvl(toku.getShisetsuName()));
					String yubinNo = nvl(toku.getShisetsuYubinNo());
					String jusho = nvl(toku.getShisetsuJusho());
					dto.setShisetsu_jusho(yubinNo.isEmpty() ? jusho : yubinNo + " " + jusho);
				});

		return dto;
	}

	/**
	 * b1Ym/b2Ym/b3Ym（taisho_ym）でt_fukaを特定してDTOを構築する
	 * @param shiteiNo 指定番号
	 * @param b1Ym     対象月b1（YYYYMM）
	 * @param b2Ym     対象月b2（YYYYMM、任意）
	 * @param b3Ym     対象月b3（YYYYMM、任意）
	 * @return 構築済みDTO
	 */
	@Transactional(readOnly = true)
	private KoseiKetteiTsuchiReportsDto buildDtoByTaishoYm(
			String shiteiNo, String b1Ym, String b2Ym, String b3Ym, String henkoKbn) {

		KoseiKetteiTsuchiReportsDto dto = new KoseiKetteiTsuchiReportsDto();

		// 通知日（システム日付）
		LocalDate today = LocalDate.now();
		dto.setTsuchi_nen(String.valueOf(today.getYear()));
		dto.setTsuchi_tsuki(String.valueOf(today.getMonthValue()));
		dto.setTsuchi_hi(String.valueOf(today.getDayOfMonth()));

		// b1/b2/b3 各月のブロック設定
		String[] ymArr = { b1Ym, b2Ym, b3Ym };
		boolean fukaKbnSet = false;
		Fuka firstFuka = null;

		for (int i = 0; i < ymArr.length; i++) {
			String taishoYm = ymArr[i];
			int blockNo = i + 1;

			// 対象月がnullまたは空文字の場合はブロック全体を空白にする
			if (taishoYm == null || taishoYm.trim().isEmpty()) {
				setBlockEmpty(dto, blockNo);
				continue;
			}

			Optional<Fuka> fukaOpt = findFukaByTaishoYm(shiteiNo, taishoYm);
			if (fukaOpt.isEmpty()) {
				// データが見つからない場合もブロック全体を空白にする
				setBlockEmpty(dto, blockNo);
				continue;
			}

			Fuka fuka = fukaOpt.get();
			if (!fukaKbnSet) {
				dto.setFukaKbn(fuka.getFukaKbn());
				dto.setHenko_riyu(nvl(fuka.getHenkoRiyu()));
				firstFuka = fuka;
				fukaKbnSet = true;
			}
			
			dto.setShitei_no(shiteiNo);

			// 有効なデータが存在する場合のみブロックの値を設定
			setKibetsuBlockByFuka(dto, fuka, blockNo);
		}

		if (!fukaKbnSet) {
			dto.setFukaKbn(FukaConstants.TEIGAKU.getValue());
		}

		// 納入税額・加算金・納期限の設定
		if (firstFuka != null) {
			setNofuAndKasan(dto, shiteiNo, firstFuka, ymArr);
			dto.setHenko_kbn(henkoKbn != null && !henkoKbn.isEmpty() ? henkoKbn : firstFuka.getHenkoKbn());
		}

		// 都道府県名（m_jichitai.kbn_name）
		dto.setTodoufuken(todoufuken);

		// 公印
		dto.setKoin(koin != null && koin.length > 0 ? koin : null);

		return dto;
	}

	/**
	 * taishoYmからFukaを検索する
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
	 * 納入税額・加算金・納期限をDTOに設定する
	 */
	private void setNofuAndKasan(
			KoseiKetteiTsuchiReportsDto dto, String shiteiNo, Fuka firstFuka, String[] ymArr) {

		String jichitaiCd = jichitaiContext.getJichitaiCd();
		long totalSashihikiSum = 0L;

		// b1, b2, b3 各月の差引増減額を合算して納入税額とする
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

			boolean isKosei = FukaConstants.KOSEI.getValue().equals(fuka.getHenkoKbn());
			List<FukaUchi> prevUchiList = (isKosei && rno > 1)
					? fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
							jichitaiCd, fuka.getShiteiNo(), rno - 1, fuka.getNendo(), fuka.getKibetsu())
					: Collections.emptyList();

			// 月の差引増減額を計算
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

		dto.setNofu_zeigaku(String.valueOf(totalSashihikiSum));

		dto.setKasan_ritsu1(firstFuka.getKasanRitsu1() != null ? firstFuka.getKasanRitsu1().toPlainString() : "");
		dto.setKasan_gaku1(firstFuka.getKasanGaku1() != null ? String.valueOf(firstFuka.getKasanGaku1()) : "");
		dto.setKasan_ritsu2(firstFuka.getKasanRitsu2() != null ? firstFuka.getKasanRitsu2().toPlainString() : "");
		dto.setKasan_gaku2(firstFuka.getKasanGaku2() != null ? String.valueOf(firstFuka.getKasanGaku2()) : "");
		dto.setKasan_ritsu3(firstFuka.getKasanRitsu3() != null ? firstFuka.getKasanRitsu3().toPlainString() : "");
		dto.setKasan_gaku3(firstFuka.getKasanGaku3() != null ? String.valueOf(firstFuka.getKasanGaku3()) : "");
		// 加算金区分（kasan_kbn）のセット
		// DBのカラム値 "1"=過少申告加算金, "2"=不申告加算金, "3"=重加算金
		// nullの場合は空文字（blankWhenNull="true" により帳票上は何も印字されない）
		dto.setKasan_kbn1(nvl(firstFuka.getKasanKbn1()));
		dto.setKasan_kbn2(nvl(firstFuka.getKasanKbn2()));
		dto.setKasan_kbn3(nvl(firstFuka.getKasanKbn3()));

		if (firstFuka.getNokigen() != null) {
			dto.setNofu_kigen_nen(String.valueOf(firstFuka.getNokigen().getYear()));
			dto.setNofu_kigen_tsuki(String.valueOf(firstFuka.getNokigen().getMonthValue()));
			dto.setNofu_kigen_hi(String.valueOf(firstFuka.getNokigen().getDayOfMonth()));
		} else {
			dto.setNofu_kigen_nen("");
			dto.setNofu_kigen_tsuki("");
			dto.setNofu_kigen_hi("");
		}
	}

	/**
	 * Fukaエンティティを元に期別ブロック（b1/b2/b3）をDTOに設定する
	 * @param dto     設定先DTO
	 * @param fuka    対象Fukaエンティティ
	 * @param blockNo ブロック番号（1=b1, 2=b2, 3=b3）
	 */
	private void setKibetsuBlockByFuka(KoseiKetteiTsuchiReportsDto dto, Fuka fuka, int blockNo) {
        String jichitaiCd = jichitaiContext.getJichitaiCd();
        String pfx = "b" + blockNo + "_";

        String taishoYm = nvl(fuka.getTaishoYm());
        String nen = taishoYm.length() == 6 ? taishoYm.substring(0, 4) : "";
        
        setField(dto, pfx + "nen",   nen);
        setField(dto, pfx + "tsuki", taishoYm.length() == 6 ? taishoYm.substring(4, 6) : "");

        Integer rno = fukaRepository.findMaxRno(
                jichitaiCd, fuka.getShiteiNo(), fuka.getNendo(), fuka.getKibetsu()).orElse(1);

        List<FukaUchi> uchiList = fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                jichitaiCd, fuka.getShiteiNo(), rno, fuka.getNendo(), fuka.getKibetsu());

        boolean isKosei = FukaConstants.KOSEI.getValue().equals(fuka.getHenkoKbn());
        List<FukaUchi> prevUchiList = (isKosei && rno > 1)
                ? fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                        jichitaiCd, fuka.getShiteiNo(), rno - 1, fuka.getNendo(), fuka.getKibetsu())
                : Collections.emptyList();
        
        // kazei_kbn=1〜5 の内訳をDTOにセット
        for (int kbn = 1; kbn <= MAX_KBN; kbn++) {
            final int k = kbn;
            Optional<FukaUchi> uchiOpt = uchiList.stream().filter(u -> k == u.getKazeiKbn()).findFirst();

            long sogaku = 0L, ryokin = 0L, zeigaku = 0L, hakusu = 0L;
            boolean hasData = false;
            if (uchiOpt.isPresent()) {
                FukaUchi u = uchiOpt.get();
                sogaku  = u.getRyokinSogaku() != null ? u.getRyokinSogaku() : 0L;
                ryokin  = u.getRyokin()       != null ? u.getRyokin()       : 0L;
                zeigaku = u.getZeigaku()      != null ? u.getZeigaku()      : 0L;
                hakusu  = u.getHakusu()       != null ? u.getHakusu()       : 0L;
                hasData = true;
            }

            long prevZeigaku = prevUchiList.stream().filter(u -> k == u.getKazeiKbn())
                    .mapToLong(u -> u.getZeigaku() != null ? u.getZeigaku() : 0L).findFirst().orElse(0L);
            long sashihiki   = zeigaku - prevZeigaku;

            // DTOの定義に合わせてb1のみフィールド名が特殊（hakusu1, b1_zeigaku1 等）なのを吸収
            String hakusuField    = (blockNo == 1) ? "hakusu" + kbn          : pfx + "hakusu" + kbn;
            String zeigakuField   = (blockNo == 1) ? "b1_zeigaku" + kbn      : pfx + "zeigaku" + kbn;
            String sashihikiField = (blockNo == 1) ? "b1_sashihiki" + kbn    : pfx + "sashihiki" + kbn;
            String kbnZeiField    = (blockNo == 1) ? "kbn_zei_gaku" + kbn    : pfx + "kbn_zei_gaku" + kbn;
            String kinoZeiField   = (blockNo == 1) ? "b1_kino_zeigaku" + kbn : pfx + "kino_zeigaku" + kbn;

            setField(dto, pfx + "sogaku" + kbn, String.valueOf(sogaku));
            setField(dto, pfx + "ryokin" + kbn, String.valueOf(ryokin));
            setField(dto, zeigakuField,         String.valueOf(zeigaku));
            setField(dto, sashihikiField,       String.valueOf(sashihiki));
            setField(dto, hakusuField,          nen.isEmpty() ? "" : String.valueOf(hakusu));
            setField(dto, kinoZeiField,         nen.isEmpty() ? "" : String.valueOf(prevZeigaku));

            // 定額の場合の区分税額セット
            if (FukaConstants.TEIGAKU.getValue().equals(fuka.getFukaKbn())) {
                if (taishoYm.length() != 6 || !hasData || hakusu <= 0) {
                    setField(dto, kbnZeiField, "");
                } else {
                    setField(dto, kbnZeiField, String.valueOf(zeigaku / hakusu));
                }
            }
        }

        // 合計値の計算
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

            if (FukaConstants.TEIGAKU.getValue().equals(fuka.getFukaKbn())) {
                long hakusu = u.map(x -> x.getHakusu() != null ? x.getHakusu() : 0L).orElse(0L);
                if (!nen.isEmpty() && u.isPresent() && hakusu > 0) {
                    kbnZeiGakuSum += (zeigaku / hakusu);
                }
            }
        }
     
        // 合計値の設定
        String hakusuSumField    = (blockNo == 1) ? "b1_hakusu_sum"    : pfx + "hakusu_sum";
        String zeigakuSumField   = (blockNo == 1) ? "b1_zeigaku_sum"   : pfx + "zeigaku_sum";
        String sashihikiSumField = (blockNo == 1) ? "b1_sashihiki_sum" : pfx + "sashihiki_sum";
        String kbnSumField       = (blockNo == 1) ? "kbn_zei_gaku_sum"      : pfx + "kbn_zei_gaku_sum";
        String kinoSumField      = (blockNo == 1) ? "b1_kino_zeigaku_sum"   : pfx + "kino_zeigaku_sum";

        setField(dto, hakusuSumField,    nen.isEmpty() ? "" : String.valueOf(hakusuSum));
        setField(dto, zeigakuSumField,   String.valueOf(zeigakuSum));
        setField(dto, sashihikiSumField, String.valueOf(sashihikiSum));
        setField(dto, kinoSumField,      nen.isEmpty() ? "" : String.valueOf(kinoZeigakuSum));

        if (FukaConstants.TEIGAKU.getValue().equals(fuka.getFukaKbn()) && kbnZeiGakuSum > 0) {
            setField(dto, kbnSumField, String.valueOf(kbnZeiGakuSum));
        } else {
            setField(dto, kbnSumField, "");
        }
        
        // 定率の場合の税率・区分名セット（blockNo == 1 のみ）
        if (blockNo == 1 && FukaConstants.TEIRITSU.getValue().equals(fuka.getFukaKbn())) {
            for (int kbn = 1; kbn <= MAX_KBN; kbn++) {
                final int k = kbn;
                String rate = uchiList.stream().filter(u -> k == u.getKazeiKbn())
                        .map(u -> u.getZeiRitsu() != null ? u.getZeiRitsu().toPlainString() : "")
                        .findFirst().orElse("");
                setField(dto, "zei_ritsu" + kbn, rate);
            }

            uchiList.stream()
                .map(FukaUchi::getZeiritsuSeq)
                .filter(seq -> seq != null)
                .findFirst()
                .ifPresent(zeiritsuSeq -> {
                    List<ZeiritsuTeiritsu> teiritsuList = zeiritsuTeiritsuRepository.findActiveBySeq(jichitaiCd, zeiritsuSeq);
                    for (ZeiritsuTeiritsu t : teiritsuList) {
                        int tSeq = t.getTeiritsuSeq().intValue();
                        if (tSeq >= 1 && tSeq <= MAX_KBN) {
                            setField(dto, "kbn_name" + tSeq, nvl(t.getKbnName()).trim());
                        }
                    }
                });
        }
        
		List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, dto.getShitei_no());
		if (tokugimuList.isEmpty()) {
			log.error("t_tokugimuが見つかりません: shiteiNo={}", dto.getShitei_no());
		}

		Tokugimu toku = tokugimuList.get(0);
		dto.setShitei_no(toku.getShiteiNo());
		dto.setShisetsu_yubin_no(nvl(toku.getShisetsuYubinNo()));
		dto.setShisetsu_jusho(nvl(toku.getShisetsuJusho()));
		dto.setShisetsu_name(nvl(toku.getShisetsuName()));

		atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, toku.getAtenaNo())
				.ifPresent(atena -> {
					dto.setYubin_no(nvl(atena.getYubinNo()));
					dto.setJusho(nvl(atena.getJusho()));
					dto.setName(nvl(atena.getName()));
				});
    }

    /**
     * 指定ブロックのDTOフィールドを全て空文字に初期化する
     */
    private void setBlockEmpty(KoseiKetteiTsuchiReportsDto dto, int blockNo) {
        String pfx = "b" + blockNo + "_";
        setField(dto, pfx + "nen",   "");
        setField(dto, pfx + "tsuki", "");

        for (int i = 1; i <= MAX_KBN; i++) {
            String hakusuField   = (blockNo == 1) ? "hakusu" + i       : pfx + "hakusu" + i;
            String zeigakuField  = (blockNo == 1) ? "b1_zeigaku" + i   : pfx + "zeigaku" + i;
            String sashihikiField = (blockNo == 1) ? "b1_sashihiki" + i : pfx + "sashihiki" + i;
            String kbnZeiField   = (blockNo == 1) ? "kbn_zei_gaku" + i : pfx + "kbn_zei_gaku" + i;

            setField(dto, pfx + "sogaku" + i, "");
            setField(dto, pfx + "ryokin" + i, "");
            String kinoZeiField   = (blockNo == 1) ? "b1_kino_zeigaku" + i : pfx + "kino_zeigaku" + i;
            setField(dto, zeigakuField,       "");
            setField(dto, sashihikiField,     "");
            setField(dto, hakusuField,        "");
            setField(dto, kbnZeiField,        "");
            setField(dto, kinoZeiField,       "");
        }
        
        String hakusuSumField    = (blockNo == 1) ? "b1_hakusu_sum"    : pfx + "hakusu_sum";
        String zeigakuSumField   = (blockNo == 1) ? "b1_zeigaku_sum"   : pfx + "zeigaku_sum";
        String sashihikiSumField = (blockNo == 1) ? "b1_sashihiki_sum" : pfx + "sashihiki_sum";
        String kbnSumField       = (blockNo == 1) ? "kbn_zei_gaku_sum" : pfx + "kbn_zei_gaku_sum";

        setField(dto, pfx + "sogaku_sum",    "");
        setField(dto, pfx + "ryokin_sum",    "");
        String kinoSumField      = (blockNo == 1) ? "b1_kino_zeigaku_sum" : pfx + "kino_zeigaku_sum";
        setField(dto, zeigakuSumField,   "");
        setField(dto, sashihikiSumField, "");
        setField(dto, hakusuSumField,    "");
        setField(dto, kbnSumField,       "");
        setField(dto, kinoSumField,      "");
    }

	/**
	 * DTOのフィールドに値をセットする
	 * ※ JRXMLとの整合性維持のためsnake_caseフィールドを直接操作
	 * @param dto   設定先DTO
	 * @param field フィールド名
	 * @param value 設定値
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
	 * null安全な文字列変換
	 * @param value 対象文字列
	 * @return nullの場合は空文字、それ以外はそのまま返す
	 */
	private String nvl(String value) {
		return value != null ? value : "";
	}
}
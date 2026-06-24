package jp.lg.asp.accommodation.service.impl;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.constant.FukaConstants;
import jp.lg.asp.accommodation.dto.KoseiKetteiTsuchiReportsDto;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.FukaUchi;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.FukaUchiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeiritsuRepository;
import jp.lg.asp.accommodation.service.KoseiKetteiTsuchiReportsService;
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
    private final ZeiritsuTeiritsuRepository zeiritsuTeiritsuRepository;

    @Value("${app.jichitai.code}")
    private String jichitaiCd;

    @Override
    @Transactional(readOnly = true)
    public byte[] generatePdf(String shiteiNo, String b1Ym, String b2Ym, String b3Ym) {
        KoseiKetteiTsuchiReportsDto dto = null;
        try {
            // TODO: b1Ym/b2Ym/b3Ymを元にDTO構築ロジックを実装中（現状はb1YmのtaishoYmで直接検索）
            dto = buildDtoByTaishoYm(shiteiNo, b1Ym, b2Ym, b3Ym);

            log.info("PDF生成開始 - 指定番号: {}, b1Ym: {}, b2Ym: {}, b3Ym: {}", shiteiNo, b1Ym, b2Ym, b3Ym);

            System.setProperty("net.sf.jasperreports.default.font.name", "IPAex明朝");
            System.setProperty("net.sf.jasperreports.awt.ignore.missing.font", "true");

            String jrxml = FukaConstants.TEIRITSU.getValue().equals(dto.getFukaKbn())
                    ? "reports/kouseiKetteiTsuchisho_teiritsu.jrxml"
                    : "reports/kouseiKetteiTsuchisho_teigaku.jrxml";

            ClassPathResource resource = new ClassPathResource(jrxml);
            if (!resource.exists()) {
                throw new RuntimeException("JRXMLファイルが見つかりません: " + jrxml);
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(resource.getInputStream());
            log.info("JRXMLファイルのコンパイル完了: {}", jrxml);

            Map<String, Object> parameters = new HashMap<>();
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(Arrays.asList(dto));

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
            byte[] pdfData = JasperExportManager.exportReportToPdf(jasperPrint);
            log.info("PDF出力完了 - サイズ: {} bytes", pdfData.length);

            return pdfData;

        } catch (Exception e) {
            log.error("PDF生成に失敗しました - 指定番号: {}", dto != null ? dto.getShitei_no() : shiteiNo, e);
            throw new RuntimeException("PDF生成に失敗しました: " + e.getMessage(), e);
        }
    }

    /**
     * b1Ym/b2Ym/b3Ym（taisho_ym）でt_fukaを特定してDTOを構築する
     */
    @Transactional(readOnly = true)
    public KoseiKetteiTsuchiReportsDto buildDtoByTaishoYm(
            String shiteiNo, String b1Ym, String b2Ym, String b3Ym) {

        KoseiKetteiTsuchiReportsDto dto = new KoseiKetteiTsuchiReportsDto();

        LocalDate today = LocalDate.now();
        dto.setTsuchi_nen(String.valueOf(today.getYear()));
        dto.setTsuchi_tsuki(String.valueOf(today.getMonthValue()));
        dto.setTsuchi_hi(String.valueOf(today.getDayOfMonth()));

        // 施設情報・定文句
        List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
        if (tokugimuList.isEmpty()) {
            log.warn("[buildDtoByTaishoYm] t_tokugimuが見つかりません: shiteiNo={}", shiteiNo);
            dto.setShitei_no(shiteiNo);
        } else {
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

        // b1/b2/b3各taishoYmでt_fukaを検索して期別ブロックを設定
        String[] ymArr = {b1Ym, b2Ym, b3Ym};
        boolean fukaKbnSet = false;
        Fuka firstFuka = null;

        for (int i = 0; i < ymArr.length; i++) {
            String taishoYm = ymArr[i];
            int blockNo = i + 1;
            if (taishoYm == null || taishoYm.isEmpty()) {
                setBlockEmpty(dto, blockNo);
                continue;
            }
            // taishoYmからnendo・kibetsuを決定しt_fukaを取得
            List<Fuka> fukaList = fukaRepository
                    .findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                            jichitaiCd, shiteiNo, taishoYm.substring(0, 4));
            Optional<Fuka> fukaOpt = fukaList.stream()
                    .filter(f -> taishoYm.equals(f.getTaishoYm()))
                    .findFirst();
            if (fukaOpt.isEmpty()) {
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
            setKibetsuBlockByFuka(dto, fuka, blockNo);
        }

        if (!fukaKbnSet) {
            dto.setFukaKbn(FukaConstants.TEIGAKU.getValue());
        }

        // nofu_zeigaku / 加算金 / 納期限は b1 の Fuka から取得
        if (firstFuka != null) {
            long totalZeigakuAll = java.util.stream.Stream.of(b1Ym, b2Ym, b3Ym)
                    .filter(ym -> ym != null && !ym.isEmpty())
                    .flatMap(ym -> fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                            jichitaiCd, shiteiNo, ym.substring(0, 4)).stream()
                            .filter(f -> ym.equals(f.getTaishoYm())))
                    .mapToLong(f -> f.getTotalZeigaku() != null ? f.getTotalZeigaku() : 0L)
                    .sum();
            dto.setNofu_zeigaku(String.valueOf(totalZeigakuAll));
            dto.setKasan_ritsu1(firstFuka.getKasanRitsu1() != null ? firstFuka.getKasanRitsu1().toPlainString() : "");
            dto.setKasan_gaku1(firstFuka.getKasanGaku1() != null ? String.valueOf(firstFuka.getKasanGaku1()) : "");
            dto.setKasan_ritsu2(firstFuka.getKasanRitsu2() != null ? firstFuka.getKasanRitsu2().toPlainString() : "");
            dto.setKasan_gaku2(firstFuka.getKasanGaku2() != null ? String.valueOf(firstFuka.getKasanGaku2()) : "");
            dto.setKasan_ritsu3(firstFuka.getKasanRitsu3() != null ? firstFuka.getKasanRitsu3().toPlainString() : "");
            dto.setKasan_gaku3(firstFuka.getKasanGaku3() != null ? String.valueOf(firstFuka.getKasanGaku3()) : "");
            if (firstFuka.getNokigen1() != null) {
                dto.setNofu_kigen_nen(String.valueOf(firstFuka.getNokigen1().getYear()));
                dto.setNofu_kigen_tsuki(String.valueOf(firstFuka.getNokigen1().getMonthValue()));
                dto.setNofu_kigen_hi(String.valueOf(firstFuka.getNokigen1().getDayOfMonth()));
            } else {
                dto.setNofu_kigen_nen(""); dto.setNofu_kigen_tsuki(""); dto.setNofu_kigen_hi("");
            }
        }

        return dto;
    }

    /**
     * Fukaエンティティを直接取って期別ブロックを設定する（taishoYm指定バージョン）
     */
    private void setKibetsuBlockByFuka(KoseiKetteiTsuchiReportsDto dto, Fuka fuka, int blockNo) {
        String prefix = "b" + blockNo + "_";
        String taishoYm = nvl(fuka.getTaishoYm());
        setField(dto, prefix + "nen", taishoYm.length() == 6 ? taishoYm.substring(0, 4) : "");
        setField(dto, prefix + "tsuki", taishoYm.length() == 6 ? taishoYm.substring(4, 6) : "");

        Integer rno = fukaRepository.findMaxRno(jichitaiCd, fuka.getShiteiNo(), fuka.getNendo(), fuka.getKibetsu())
                .orElse(1);
        List<FukaUchi> uchiList = fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                jichitaiCd, fuka.getShiteiNo(), rno, fuka.getNendo(), fuka.getKibetsu());

        boolean isKosei = FukaConstants.KOSEI.getValue().equals(fuka.getHenkoKbn());
        List<FukaUchi> prevUchiList = (isKosei && rno > 1)
                ? fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                        jichitaiCd, fuka.getShiteiNo(), rno - 1, fuka.getNendo(), fuka.getKibetsu())
                : java.util.Collections.emptyList();
        long prevFukaZeigaku = prevUchiList.stream()
                .mapToLong(u -> u.getZeigaku() != null ? u.getZeigaku() : 0L).sum();

        for (int kbn = 1; kbn <= 5; kbn++) {
            final int k = kbn;
            Optional<FukaUchi> uchiOpt = uchiList.stream().filter(u -> k == u.getKazeiKbn()).findFirst();
            long sogaku = 0L, ryokin = 0L, zeigaku = 0L, hakusu = 0L;
            if (uchiOpt.isPresent()) {
                FukaUchi u = uchiOpt.get();
                sogaku  = u.getRyokinSogaku() != null ? u.getRyokinSogaku() : 0L;
                ryokin  = u.getRyokin()       != null ? u.getRyokin()       : 0L;
                zeigaku = u.getZeigaku()      != null ? u.getZeigaku()      : 0L;
                hakusu  = u.getHakusu()       != null ? u.getHakusu()       : 0L; // ①追加
            }
            final int fk = kbn;
            long prevZeigaku = prevUchiList.stream().filter(u -> fk == u.getKazeiKbn())
                    .mapToLong(u -> u.getZeigaku() != null ? u.getZeigaku() : 0L).findFirst().orElse(0L);
            long sashihiki = zeigaku - prevZeigaku;

            if (blockNo == 1) {
                setField(dto, "sogaku" + kbn, String.valueOf(sogaku));
                setField(dto, "ryokin" + kbn, String.valueOf(ryokin));
                setField(dto, "b1_zeigaku" + kbn, String.valueOf(zeigaku));
                setField(dto, "b1_sashihiki" + kbn, String.valueOf(sashihiki));
                // ①追加: 宿泊者数（定額用）
                setField(dto, "hakusu" + kbn, String.valueOf(hakusu));
                // ①追加: 区分税額（定額固定額 = zeigaku ÷ hakusu）
                if (FukaConstants.TEIGAKU.getValue().equals(fuka.getFukaKbn())) {
                    long kbnZeiGaku = (hakusu > 0) ? zeigaku / hakusu : 0L;
                    setField(dto, "kbn_zei_gaku" + kbn, String.valueOf(kbnZeiGaku));
                }
            } else {
                setField(dto, prefix + "sogaku" + kbn, String.valueOf(sogaku));
                setField(dto, prefix + "ryokin" + kbn, String.valueOf(ryokin));
                setField(dto, prefix + "zeigaku" + kbn, String.valueOf(zeigaku));
                setField(dto, prefix + "sashihiki" + kbn, String.valueOf(sashihiki));
                // ①追加: 宿泊者数（b2/b3用）
                setField(dto, prefix + "hakusu" + kbn, String.valueOf(hakusu));
            }
        }

        setField(dto, prefix + "sogaku_sum", fuka.getSogaku() != null ? String.valueOf(fuka.getSogaku()) : "");
        setField(dto, prefix + "ryokin_sum", fuka.getKazeiRyokin() != null ? String.valueOf(fuka.getKazeiRyokin()) : "");
        setField(dto, prefix + "zeigaku_sum", fuka.getZeigaku() != null ? String.valueOf(fuka.getZeigaku()) : "");
        long fukaZeigaku = fuka.getZeigaku() != null ? fuka.getZeigaku() : 0L;
        setField(dto, prefix + "sashihiki_sum", String.valueOf(fukaZeigaku - prevFukaZeigaku));

        // ②追加: hakusu_sum（宿泊者数合計）
        long hakusuSum = uchiList.stream()
                .mapToLong(u -> u.getHakusu() != null ? u.getHakusu() : 0L).sum();
        if (blockNo == 1) {
            setField(dto, "b1_hakusu_sum", String.valueOf(hakusuSum));
        } else {
            setField(dto, prefix + "hakusu_sum", String.valueOf(hakusuSum));
        }

        if (blockNo == 1) {
            for (int kbn = 1; kbn <= 5; kbn++) {
                final int k = kbn;
                String rate = uchiList.stream().filter(u -> k == u.getKazeiKbn())
                    .map(u -> u.getZeiRitsu() != null ? u.getZeiRitsu().toPlainString() : "")
                    .findFirst().orElse("");
                setField(dto, "zei_ritsu" + kbn, rate);
            }
        }
    }

    /**
     * shiteiNoの taisho_ym 一覧を取得する（画面の月選択プルダウン用）
     */
    @Override
    @Transactional(readOnly = true)
    public List<String> findTaishoYmList(String shiteiNo) {
        return fukaRepository.findTaishoYmListByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
    }

    /**
     * shiteiNoのみから画面初期表示用DTOを構築する
     */
    @Override
    @Transactional(readOnly = true)
    public KoseiKetteiTsuchiReportsDto buildDtoForDisplay(String shiteiNo) {
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
     * shiteiNoとnendoからDTOを構築する
     */
    @Transactional(readOnly = true)
    public KoseiKetteiTsuchiReportsDto buildDto(String shiteiNo, String nendo) {
        KoseiKetteiTsuchiReportsDto dto = new KoseiKetteiTsuchiReportsDto();

        // ── 通知日（システム日付）──
        LocalDate today = LocalDate.now();
        dto.setTsuchi_nen(String.valueOf(today.getYear()));
        dto.setTsuchi_tsuki(String.valueOf(today.getMonthValue()));
        dto.setTsuchi_hi(String.valueOf(today.getDayOfMonth()));

        // ── 義務者・施設情報（t_tokugimu）──
        List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
        if (tokugimuList.isEmpty()) {
            log.warn("[buildDto] t_tokugimuが見つかりません: shiteiNo={}", shiteiNo);
            dto.setShitei_no(shiteiNo);
        } else {
            Tokugimu toku = tokugimuList.get(0);
            dto.setShitei_no(toku.getShiteiNo());
            dto.setShisetsu_yubin_no(nvl(toku.getShisetsuYubinNo()));
            dto.setShisetsu_jusho(nvl(toku.getShisetsuJusho()));
            dto.setShisetsu_name(nvl(toku.getShisetsuName()));

            // ── 送付先宛名（m_atena）──
            atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, toku.getAtenaNo())
                    .ifPresent(atena -> {
                        dto.setYubin_no(nvl(atena.getYubinNo()));
                        dto.setJusho(nvl(atena.getJusho()));
                        dto.setName(nvl(atena.getName()));
                    });
        }

        // ── 賦課データ（t_fuka）全期分取得 ──
        List<Fuka> fukaList = fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                jichitaiCd, shiteiNo, nendo);

        if (!fukaList.isEmpty()) {
            // fukaKbnは最初のレコードから取得
            Fuka firstFuka = fukaList.get(0);
            dto.setFukaKbn(firstFuka.getFukaKbn());
            dto.setHenko_riyu(nvl(firstFuka.getHenkoRiyu()));

            // nofu_zeigaku: 全期の total_zeigaku 合計
            long totalZeigakuAll = fukaList.stream()
                    .mapToLong(f -> f.getTotalZeigaku() != null ? f.getTotalZeigaku() : 0L)
                    .sum();
            dto.setNofu_zeigaku(String.valueOf(totalZeigakuAll));

            // 加算金（kasan_kbn1〜3 / kasan_ritsu1〜3 / kasan_gaku1〜3 それぞれ独立したカラム）
            dto.setKasan_ritsu1(firstFuka.getKasanRitsu1() != null ? firstFuka.getKasanRitsu1().toPlainString() : "");
            dto.setKasan_gaku1(firstFuka.getKasanGaku1() != null ? String.valueOf(firstFuka.getKasanGaku1()) : "");
            dto.setKasan_ritsu2(firstFuka.getKasanRitsu2() != null ? firstFuka.getKasanRitsu2().toPlainString() : "");
            dto.setKasan_gaku2(firstFuka.getKasanGaku2() != null ? String.valueOf(firstFuka.getKasanGaku2()) : "");
            dto.setKasan_ritsu3(firstFuka.getKasanRitsu3() != null ? firstFuka.getKasanRitsu3().toPlainString() : "");
            dto.setKasan_gaku3(firstFuka.getKasanGaku3() != null ? String.valueOf(firstFuka.getKasanGaku3()) : "");

            // 納期限は nokigen1 を使用
            if (firstFuka.getNokigen1() != null) {
                dto.setNofu_kigen_nen(String.valueOf(firstFuka.getNokigen1().getYear()));
                dto.setNofu_kigen_tsuki(String.valueOf(firstFuka.getNokigen1().getMonthValue()));
                dto.setNofu_kigen_hi(String.valueOf(firstFuka.getNokigen1().getDayOfMonth()));
            } else {
                dto.setNofu_kigen_nen("");
                dto.setNofu_kigen_tsuki("");
                dto.setNofu_kigen_hi("");
            }

            // 期別1〜3をkibetsuで特定してDTOに詰める
            setKibetsuBlock(dto, fukaList, 1);
            setKibetsuBlock(dto, fukaList, 2);
            setKibetsuBlock(dto, fukaList, 3);
        } else {
            log.warn("[buildDto] t_fukaが見つかりません: shiteiNo={}, nendo={}", shiteiNo, nendo);
            dto.setFukaKbn(FukaConstants.TEIGAKU.getValue());
        }

        return dto;
    }

    /**
     * 指定kibetsuのFukaレコードをDTOの期別ブロック（b1/b2/b3）に詰める
     */
    private void setKibetsuBlock(KoseiKetteiTsuchiReportsDto dto, List<Fuka> fukaList, int kibetsu) {
        String prefix = "b" + kibetsu + "_";

        Optional<Fuka> fukaOpt = fukaList.stream()
                .filter(f -> kibetsu == f.getKibetsu())
                .findFirst();

        if (fukaOpt.isEmpty()) {
            // データなし → 全フィールドを空文字
            setBlockEmpty(dto, kibetsu);
            return;
        }

        Fuka fuka = fukaOpt.get();

        // 対象年月から年・月を分割（YYYYMM形式）
        String taishoYm = nvl(fuka.getTaishoYm());
        if (taishoYm.length() == 6) {
            setField(dto, prefix + "nen", taishoYm.substring(0, 4));
            setField(dto, prefix + "tsuki", taishoYm.substring(4, 6));
        } else {
            setField(dto, prefix + "nen", "");
            setField(dto, prefix + "tsuki", "");
        }

        // t_fuka_uchiから内訳取得（最新rnoのレコード）
        Integer rno = fukaRepository.findMaxRno(jichitaiCd, fuka.getShiteiNo(), fuka.getNendo(), fuka.getKibetsu())
                .orElse(1);
        List<FukaUchi> uchiList = fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                jichitaiCd, fuka.getShiteiNo(), rno, fuka.getNendo(), fuka.getKibetsu());

        // 更正（henko_kbn=2）の場合は rno-1 の内訳を取得して差引計算に使用
        // 決定（henko_kbn=3）は前回unoレコードなしのため空リスト
        boolean isKosei = FukaConstants.KOSEI.getValue().equals(fuka.getHenkoKbn());
        List<FukaUchi> prevUchiList = (isKosei && rno > 1)
                ? fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                        jichitaiCd, fuka.getShiteiNo(), rno - 1, fuka.getNendo(), fuka.getKibetsu())
                : java.util.Collections.emptyList();
        long prevFukaZeigaku = prevUchiList.stream()
                .mapToLong(u -> u.getZeigaku() != null ? u.getZeigaku() : 0L).sum();

        // kazei_kbn=1〜5 に対応する内訳をDTOにセット
        long sogakuSum = 0L, ryokinSum = 0L, zeigakuSum = 0L;
        for (int kbn = 1; kbn <= 5; kbn++) {
            final int k = kbn;
            Optional<FukaUchi> uchiOpt = uchiList.stream()
                    .filter(u -> k == u.getKazeiKbn())
                    .findFirst();

            long sogaku = 0L, ryokin = 0L, zeigaku = 0L;
            if (uchiOpt.isPresent()) {
                FukaUchi u = uchiOpt.get();
                sogaku = u.getRyokinSogaku() != null ? u.getRyokinSogaku() : 0L;
                ryokin = u.getRyokin() != null ? u.getRyokin() : 0L;
                zeigaku = u.getZeigaku() != null ? u.getZeigaku() : 0L;
            }

            // sashihiki（差引増減額）計算
            // 更正: 今囜zeigaku - 前囜zeigaku（rno-1の同一kazei_kbn）
            // 決定: zeigakuそのまま（既申告なし、差引元 = 0）
            final int fk = kbn;
            long prevZeigaku = prevUchiList.stream()
                    .filter(u -> fk == u.getKazeiKbn())
                    .mapToLong(u -> u.getZeigaku() != null ? u.getZeigaku() : 0L)
                    .findFirst().orElse(0L);
            long sashihiki = zeigaku - prevZeigaku;

            if (kibetsu == 1) {
                setField(dto, "sogaku" + kbn, String.valueOf(sogaku));
                setField(dto, "ryokin" + kbn, String.valueOf(ryokin));
                setField(dto, "b1_zeigaku" + kbn, String.valueOf(zeigaku));
                setField(dto, "b1_sashihiki" + kbn, String.valueOf(sashihiki));
            } else {
                setField(dto, prefix + "sogaku" + kbn, String.valueOf(sogaku));
                setField(dto, prefix + "ryokin" + kbn, String.valueOf(ryokin));
                setField(dto, prefix + "zeigaku" + kbn, String.valueOf(zeigaku));
                setField(dto, prefix + "sashihiki" + kbn, String.valueOf(sashihiki));
            }

            sogakuSum += sogaku;
            ryokinSum += ryokin;
            zeigakuSum += zeigaku;
        }

        setField(dto, prefix + "sogaku_sum", fuka.getSogaku() != null ? String.valueOf(fuka.getSogaku()) : "");
        setField(dto, prefix + "ryokin_sum", fuka.getKazeiRyokin() != null ? String.valueOf(fuka.getKazeiRyokin()) : "");
        setField(dto, prefix + "zeigaku_sum", fuka.getZeigaku() != null ? String.valueOf(fuka.getZeigaku()) : "");
        // sashihiki_sum: t_fukaレベルの差引合計（今回zeigaku - 前回zeigaku合計）
        long fukaZeigaku = fuka.getZeigaku() != null ? fuka.getZeigaku() : 0L;
        setField(dto, prefix + "sashihiki_sum", String.valueOf(fukaZeigaku - prevFukaZeigaku));

        // 税率（定率制の場合のみ zei_ritsu1〜5 をセット。kibetsu=1のみ）
        if (kibetsu == 1 && FukaConstants.TEIRITSU.getValue().equals(fuka.getFukaKbn())) {
            // TODO: zei_ritsuはm_zeiritsu_teiritsuから取得要。現状はt_fuka_uchi.zei_ritsuを使用
            for (int kbn = 1; kbn <= 5; kbn++) {
                final int k = kbn;
                String rate = uchiList.stream()
                        .filter(u -> k == u.getKazeiKbn())
                        .map(u -> u.getZeiRitsu() != null ? u.getZeiRitsu().toPlainString() : "")
                        .findFirst().orElse("");
                setField(dto, "zei_ritsu" + kbn, rate);
            }
        }
    }

    /**
     * 指定kibetsuのブロックを全て空文字にする
     */
    private void setBlockEmpty(KoseiKetteiTsuchiReportsDto dto, int kibetsu) {
        String prefix = "b" + kibetsu + "_";
        setField(dto, prefix + "nen", "");
        setField(dto, prefix + "tsuki", "");
        for (int i = 1; i <= 5; i++) {
            if (kibetsu == 1) {
                setField(dto, "sogaku" + i, "");
                setField(dto, "ryokin" + i, "");
                setField(dto, "b1_zeigaku" + i, "");
                setField(dto, "b1_sashihiki" + i, "");
            } else {
                setField(dto, prefix + "sogaku" + i, "");
                setField(dto, prefix + "ryokin" + i, "");
                setField(dto, prefix + "zeigaku" + i, "");
                setField(dto, prefix + "sashihiki" + i, "");
            }
        }
        setField(dto, prefix + "sogaku_sum", "");
        setField(dto, prefix + "ryokin_sum", "");
        setField(dto, prefix + "zeigaku_sum", "");
        setField(dto, prefix + "sashihiki_sum", "");
    }

    /**
     * DTOへのリフレクションなしフィールドセット（スネークケースフィールド対応）
     */
    private void setField(KoseiKetteiTsuchiReportsDto dto, String field, String value) {
        try {
            java.lang.reflect.Field f = dto.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(dto, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.warn("[setField] フィールドが見つかりません: {}", field);
        }
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }
}

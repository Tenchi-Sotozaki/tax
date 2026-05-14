package jp.lg.asp.accommodation.service;

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
import jp.lg.asp.accommodation.dto.FukaMonthlyTallyDto.DailyItem;
import jp.lg.asp.accommodation.dto.FukaTaxDetailDto;
import jp.lg.asp.accommodation.entity.ChoshuGenbo;
import jp.lg.asp.accommodation.entity.ChoshuGenboId;
import jp.lg.asp.accommodation.entity.ChoshuGenboUchi;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.FukaId;
import jp.lg.asp.accommodation.entity.FukaUchi;
import jp.lg.asp.accommodation.entity.FukaZeiritsuTeigaku;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.ChoshuGenboRepository;
import jp.lg.asp.accommodation.repository.ChoshuGenboUchiRepository;
import jp.lg.asp.accommodation.repository.FukaMonthlyDeclarationRepository;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.FukaUchiRepository;
import jp.lg.asp.accommodation.repository.FukaZeiritsuTeigakuRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
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
    private final FukaMonthlyDeclarationRepository repository;
    private final FukaValidatorService validatorService;
    private final FukaZeiritsuTeigakuRepository zeiritsuTeigakuRepository;
    private final FukaUchiRepository fukaUchiRepository;
    private final ChoshuGenboRepository choshuGenboRepository;
    private final ChoshuGenboUchiRepository choshuGenboUchiRepository;
    private final AtenaRepository atenaRepository;

    // 定数定義（マジックナンバーの排除）
    private static final String STATUS_ALL = "999";
    private static final String STATUS_ZUMI = "1";
    private static final String STATUS_MI = "2";
    private static final int MAX_KIBETSU = 12;
    private static final int MAX_DAYS = 31;
    private static final int FISCAL_START_MONTH = 4;
    private static final String DEFAULT_NEW_FLG = "1";
    private static final String DEFAULT_DEL_FLG = "0";
    private static final int INITIAL_VERSION = 1;

    @Value("${app.jichitai.code}")
    private String jichitaiCd;

    @Value("${app.jichitai.code}")
    private String configJichitaiCd;

    /**
     * 徴収原簿エンティティから31日分の内訳IDをリストとして抽出する。
     */
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

        List<Fuka> fukaList = fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(jichitaiCd, shiteiNo, nendo);
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

        try {
            setupObligorInfo(form, shiteiNo);

            List<FukaZeiritsuTeigaku> masterRates = zeiritsuTeigakuRepository.findByJichitaiCdOrderByRyokinStAsc(jichitaiCd);
            setupMonthlyDetail(form, masterRates, paymentMonth);

            jp.lg.asp.accommodation.dto.FukaMonthlyTallyDto tallyDto = new jp.lg.asp.accommodation.dto.FukaMonthlyTallyDto();
            tallyDto.initialize(masterRates.size());
            form.setMonthlyTally(tallyDto);

            if (StringUtils.hasText(paymentMonth)) {
                restoreExistingDeclaration(form, shiteiNo, paymentMonth);
            }
        } catch (Exception e) {
            log.error("登録用データの取得に失敗しました。指定番号: {}, 年月: {}", shiteiNo, paymentMonth, e);
        }
        return form;
    }

    /**
     * 義務者情報を取得しフォームにセットする。
     */
    private void setupObligorInfo(FukaDeclarationForm form, String shiteiNo) {
        tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
                .stream()
                .findFirst()
                .ifPresent(tokugimu -> {
                    form.setObligorName(tokugimu.getKyokaName());
                    form.setFacilityName(tokugimu.getShisetsuName());
                });
    }

    /**
     * 月次明細情報をマスタに基づいてセットアップする。
     */
    private void setupMonthlyDetail(FukaDeclarationForm form, List<FukaZeiritsuTeigaku> masterRates, String paymentMonth) {
        FukaMonthlyDeclarationDto monthlyDetail = new FukaMonthlyDeclarationDto();
        monthlyDetail.setPaymentYearMonth(paymentMonth);

        for (FukaZeiritsuTeigaku master : masterRates) {
            FukaTaxDetailDto detail = new FukaTaxDetailDto();
            detail.setZeiritsuSeq(master.getSeq());
            detail.setTeigakuSeq(master.getTeigakuSeq());

            String label = (master.getRyokinEd() != null)
                    ? String.format("%,d円 ～ %,d円未満", master.getRyokinSt(), master.getRyokinEd() + 1)
                    : String.format("%,d円以上", master.getRyokinSt());

            detail.setLabel(label);
            detail.setTaxRate(master.getZeigaku());
            monthlyDetail.getTaxDetails().add(detail);
        }
        form.setMonthlyDetail(monthlyDetail);
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

                    List<FukaUchi> uchiList = fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                            latestFuka.getJichitaiCd(), latestFuka.getShiteiNo(), latestFuka.getRno(),
                            latestFuka.getNendo(), latestFuka.getKibetsu());

                    syncUchiDataToForm(uchiList, monthlyDetail);
                    hydrateMonthlyTally(form, latestFuka.getJichitaiCd(), latestFuka);
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

        for (int i = 0; i < dto.getTaxDetails().size(); i++) {
            FukaTaxDetailDto formDetail = dto.getTaxDetails().get(i);
            FukaUchi matched = uchiMap.get(i + 1);

            if (matched != null) {
                formDetail.setStayCount(matched.getHakusu());
                formDetail.setTaxAmount(matched.getZeigaku());
            } else {
                formDetail.setStayCount(null);
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
        hydrateFormMetadata(form);

        String jichitaiCd = getCurrentJichitaiCd();
        fukaRepository.findFirstByJichitaiCdAndShiteiNoAndNendoAndKibetsuOrderByRnoDesc(jichitaiCd, shiteiNo, nendo, kibetsu)
                .ifPresent(entity -> {
                    form.setRegistrationDate(entity.getShinkokuYmd());
                    form.setNendo(entity.getNendo());
                    form.setKibetsu(entity.getKibetsu());
                    form.setModificationCategory(entity.getHenkoKbn());
                    form.setModificationReason(entity.getHenkoRiyu());

                    hydrateAdditionalFields(entity, form);
                    hydrateMonthlyDetail(entity, form, jichitaiCd);
                    hydrateMonthlyTally(form, jichitaiCd, entity);
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
        String currentJichitaiCd = getCurrentJichitaiCd();
        String category = form.getModificationCategory();

        Integer targetRno = "2".equals(category)
                ? getCurrentMaxRno(currentJichitaiCd, form.getShiteiNo(), form.getNendo(), form.getKibetsu())
                : determineNextRno(currentJichitaiCd, form.getShiteiNo(), form.getNendo(), form.getKibetsu());

        FukaMonthlyDeclarationDto dto = form.getMonthlyDetail();
        Fuka parentFuka = createParentFuka(form, dto, currentJichitaiCd);
        parentFuka.setRno(targetRno);

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
            saveChoshuGenboDataWithRno(form, parentFuka, currentJichitaiCd, targetRno);
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
     * 徴収原簿データを保存する。
     */
    private void saveChoshuGenboData(FukaDeclarationForm form, Fuka parentFuka, String jichitaiCd) {
        Long[] uchiIndices = new Long[MAX_DAYS];
        List<DailyItem> dailyItems = form.getMonthlyTally().getDailyItems();
        Long currentMaxIdx = choshuGenboUchiRepository.getMaxUchiIdx();

        for (int i = 0; i < dailyItems.size() && i < MAX_DAYS; i++) {
            DailyItem item = dailyItems.get(i);
            if (isDailyDataPresent(item)) {
                currentMaxIdx++;
                Long nextIdx = currentMaxIdx;
                uchiIndices[i] = nextIdx;

                ChoshuGenboUchi uchi = new ChoshuGenboUchi();
                uchi.setUchiIdx(nextIdx);

                List<Integer> counts = item.getTaxCategoryCounts();
                if (counts.size() >= 1) uchi.setHakusu1(counts.get(0));
                if (counts.size() >= 2) uchi.setHakusu2(counts.get(1));
                if (counts.size() >= 3) uchi.setHakusu3(counts.get(2));

                uchi.setMenjoHakusu(item.getExemptCount());
                setAuditFields(uchi);
                choshuGenboUchiRepository.save(uchi);
            }
        }

        ChoshuGenbo genbo = new ChoshuGenbo();
        genbo.setJichitaiCd(jichitaiCd);
        genbo.setShiteiNo(parentFuka.getShiteiNo());
        genbo.setNendo(parentFuka.getNendo());
        genbo.setKibetsu(parentFuka.getKibetsu());
        genbo.setRno(parentFuka.getRno());
        setUchiIndicesToGenbo(genbo, uchiIndices);
        choshuGenboRepository.save(genbo);
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
     * 日別データに入力があるか判定する。
     */
    private boolean isDailyDataPresent(DailyItem item) {
        if (item == null) {
            return false;
        }
        int taxSum = item.getTaxCategoryCounts().stream()
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        int exempt = (item.getExemptCount() != null) ? item.getExemptCount() : 0;
        return (taxSum + exempt) > 0;
    }

    /**
     * 内訳IDを徴収原簿のカラムにマッピングする。
     */
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

    /**
     * 内訳エンティティのリストを生成する。
     */
    private List<FukaUchi> createFukaUchiList(FukaDeclarationForm form, Fuka parentFuka, String currentJichitaiCd) {
        List<FukaUchi> uchiList = new ArrayList<>();
        FukaMonthlyDeclarationDto dto = form.getMonthlyDetail();

        for (int i = 0; i < dto.getTaxDetails().size(); i++) {
            FukaTaxDetailDto detail = dto.getTaxDetails().get(i);
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
            uchi.setHakusu(detail.getStayCount());
            uchi.setZeigaku(detail.getTaxAmount());
            uchi.setCityZeigaku(detail.getTaxAmount());
            uchi.setKenZeigaku(0L);
            uchi.setZeiRitsu(java.math.BigDecimal.valueOf(detail.getTaxRate()));

            setAuditFields(uchi);
            uchiList.add(uchi);
        }
        return uchiList;
    }

    /**
     * 徴収原簿から月計表データを復元する。
     */
    private void hydrateMonthlyTally(FukaDeclarationForm form, String jichitaiCd, Fuka parentFuka) {
        ChoshuGenboId genboId = new ChoshuGenboId(jichitaiCd, parentFuka.getShiteiNo(), parentFuka.getRno(),
                parentFuka.getNendo(), parentFuka.getKibetsu());

        Optional<ChoshuGenbo> genboOpt = choshuGenboRepository.findById(genboId);
        if (genboOpt.isEmpty()) {
            return;
        }

        ChoshuGenbo genbo = genboOpt.get();
        List<Long> uchiIndices = collectUchiIndices(genbo);

        List<ChoshuGenboUchi> uchiList = choshuGenboUchiRepository.findAllById(
                uchiIndices.stream().filter(java.util.Objects::nonNull).collect(Collectors.toList()));

        Map<Long, ChoshuGenboUchi> uchiMap = uchiList.stream()
                .collect(Collectors.toMap(ChoshuGenboUchi::getUchiIdx, u -> u));

        int categoryCount = form.getMonthlyDetail().getTaxDetails().size();
        form.getMonthlyTally().initialize(categoryCount);

        for (int i = 0; i < MAX_DAYS; i++) {
            Long idx = uchiIndices.get(i);
            if (idx != null && uchiMap.containsKey(idx)) {
                ChoshuGenboUchi uchi = uchiMap.get(idx);
                DailyItem dDto = form.getMonthlyTally().getDailyItems().get(i);
                for (int j = 1; j <= categoryCount; j++) {
                    Integer value = getHakusuValue(uchi, j);
                    dDto.getTaxCategoryCounts().set(j - 1, value);
                }
                dDto.setExemptCount(uchi.getMenjoHakusu());
            }
        }
    }

    /**
     * エンティティから動的に宿泊数値を取得する。
     */
    private Integer getHakusuValue(ChoshuGenboUchi uchi, int index) {
        try {
            String methodName = "getHakusu" + index;
            Object val = uchi.getClass().getMethod(methodName).invoke(uchi);
            return (val != null) ? (Integer) val : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 指定された条件のデータが登録済みか判定する。
     */
    public boolean isAlreadyRegistered(String shiteiNo, String paymentMonth) {
        if (!StringUtils.hasText(paymentMonth)) {
            return false;
        }
        String targetYm = paymentMonth.replace("-", "");
        return fukaRepository.findFirstByJichitaiCdAndShiteiNoAndTaishoYmOrderByRnoDesc(jichitaiCd, shiteiNo, targetYm).isPresent();
    }

    /**
     * 親エンティティを生成する。
     */
    private Fuka createParentFuka(FukaDeclarationForm form, jp.lg.asp.accommodation.dto.FukaMonthlyDeclarationDto dto,
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
        parentFuka.setFukaKbn(StringUtils.hasText(form.getModificationCategory()) ? form.getModificationCategory() : "1");
        parentFuka.setHenkoKbn(mapModificationCategory(form.getModificationCategory()));
        parentFuka.setHenkoRiyu(form.getModificationReason());
        parentFuka.setNewFlg(DEFAULT_NEW_FLG);
        parentFuka.setDelFlg(DEFAULT_DEL_FLG);
        parentFuka.setVersion(INITIAL_VERSION);
        parentFuka.setTaishoYm(dto.getPaymentYearMonth().replace("-", ""));
        parentFuka.setTotalHakusu(dto.getTotalStayCount());
        parentFuka.setTotalZeigaku(dto.getTotalPaymentAmount());
        parentFuka.setMenjoHakusu(dto.getExemptStayCount());
        parentFuka.setCityZeigaku(parentFuka.getTotalZeigaku());
        parentFuka.setKenZeigaku(0L);

        mapAdditionalFields(form, parentFuka);
        return parentFuka;
    }

    /**
     * 月計表データを特定のRNOで保存する。
     */
    private void saveChoshuGenboDataWithRno(FukaDeclarationForm form, Fuka parentFuka, String jichitaiCd, int targetRno) {
        jp.lg.asp.accommodation.entity.ChoshuGenboId genboId = new jp.lg.asp.accommodation.entity.ChoshuGenboId(
                jichitaiCd, parentFuka.getShiteiNo(), targetRno, parentFuka.getNendo(), parentFuka.getKibetsu());

        Optional<ChoshuGenbo> existingGenboOpt = choshuGenboRepository.findById(genboId);
        Long[] uchiIndices = new Long[MAX_DAYS];
        if (existingGenboOpt.isPresent()) {
            List<Long> currentIndices = collectUchiIndices(existingGenboOpt.get());
            for (int i = 0; i < MAX_DAYS; i++) uchiIndices[i] = currentIndices.get(i);
        }

        List<DailyItem> dailyItems = form.getMonthlyTally().getDailyItems();
        Long currentMaxIdx = choshuGenboUchiRepository.getMaxUchiIdx();

        for (int i = 0; i < dailyItems.size() && i < MAX_DAYS; i++) {
            DailyItem item = dailyItems.get(i);
            if (isDailyDataPresent(item)) {
                Long targetIdx = uchiIndices[i];
                if (targetIdx == null) {
                    currentMaxIdx++;
                    targetIdx = currentMaxIdx;
                }
                uchiIndices[i] = targetIdx;

                jp.lg.asp.accommodation.entity.ChoshuGenboUchi uchi = new jp.lg.asp.accommodation.entity.ChoshuGenboUchi();
                uchi.setUchiIdx(targetIdx);
                List<Integer> counts = item.getTaxCategoryCounts();
                if (counts.size() >= 1) uchi.setHakusu1(counts.get(0));
                if (counts.size() >= 2) uchi.setHakusu2(counts.get(1));
                if (counts.size() >= 3) uchi.setHakusu3(counts.get(2));
                uchi.setMenjoHakusu(item.getExemptCount());
                setAuditFields(uchi);
                choshuGenboUchiRepository.save(uchi);
            }
        }

        ChoshuGenbo genbo = existingGenboOpt.orElse(new ChoshuGenbo());
        genbo.setJichitaiCd(jichitaiCd);
        genbo.setShiteiNo(parentFuka.getShiteiNo());
        genbo.setNendo(parentFuka.getNendo());
        genbo.setKibetsu(parentFuka.getKibetsu());
        genbo.setRno(targetRno);
        setUchiIndicesToGenbo(genbo, uchiIndices);
        setAuditFields(genbo);
        choshuGenboRepository.save(genbo);
    }

    /**
     * フォームのメタデータを再セットする。
     */
    public void hydrateFormMetadata(FukaDeclarationForm form) {
        if (form.getShiteiNo() == null) {
            return;
        }
        String jichitaiCd = getCurrentJichitaiCd();
        tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, form.getShiteiNo())
                .stream()
                .findFirst()
                .ifPresent(tokugimu -> {
                    form.setFacilityName(tokugimu.getShisetsuName());
                    form.setObligorName(tokugimu.getKyokaName());
                });
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
     * 月次明細サマリを復元する。
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

        List<FukaZeiritsuTeigaku> masterRates = zeiritsuTeigakuRepository.findByJichitaiCdOrderByRyokinStAsc(jichitaiCd);
        for (FukaZeiritsuTeigaku m : masterRates) {
            FukaTaxDetailDto d = new FukaTaxDetailDto();
            d.setZeiritsuSeq(m.getSeq());
            d.setTeigakuSeq(m.getTeigakuSeq());
            d.setTaxRate(m.getZeigaku());
            d.setLabel(m.getRyokinEd() != null ? String.format("%,d円 ～ %,d円未満", m.getRyokinSt(), m.getRyokinEd() + 1)
                    : String.format("%,d円以上", m.getRyokinSt()));
            monthDto.getTaxDetails().add(d);
        }
        form.setMonthlyDetail(monthDto);
        List<FukaUchi> uchiList = fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                jichitaiCd, form.getShiteiNo(), entity.getRno(), entity.getNendo(), entity.getKibetsu());
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
    }

    /**
     * 入力税額と理論値の相違を判定する。
     */
    public boolean hasTaxAmountDiscrepancy(FukaDeclarationForm form) {
        FukaMonthlyDeclarationDto detail = form.getMonthlyDetail();
        if (detail == null || detail.getTaxDetails() == null) {
            return false;
        }

        long calculatedTotal = detail.getTaxDetails().stream()
                .mapToLong(d -> {
                    long rate = (d.getTaxRate() != null) ? d.getTaxRate() : 0L;
                    int count = (d.getStayCount() != null) ? d.getStayCount() : 0;
                    return rate * count;
                })
                .sum();

        long inputTotal = (detail.getTotalPaymentAmount() != null) ? detail.getTotalPaymentAmount() : 0L;
        return calculatedTotal != inputTotal;
    }
    
    /**
     * 指定された年度・期別に該当する申告データが存在するか判定する。
     */
    public boolean isAlreadyRegisteredByKibetsu(String shiteiNo, String nendo, Integer kibetsu) {
        // 初期履歴番号(1)のデータが存在するかを確認する
        FukaId fukaId = new FukaId(jichitaiCd, shiteiNo, INITIAL_VERSION, nendo, kibetsu);
        return fukaRepository.findById(fukaId).isPresent();
    }
}
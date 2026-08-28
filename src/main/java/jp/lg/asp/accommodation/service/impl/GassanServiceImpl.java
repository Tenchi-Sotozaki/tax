package jp.lg.asp.accommodation.service.impl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.GassanForm;
import jp.lg.asp.accommodation.dto.GassanForm.FacilityItem;
import jp.lg.asp.accommodation.dto.GassanForm.GassanListItem;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Gassan;
import jp.lg.asp.accommodation.entity.GassanUchi;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.GassanRepository;
import jp.lg.asp.accommodation.repository.GassanUchiRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.GassanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GassanServiceImpl implements GassanService {

    private final GassanRepository gassanRepository;
    private final GassanUchiRepository gassanUchiRepository;
    private final AtenaRepository atenaRepository;
    private final TokugimuRepository tokugimuRepository;
    private final JichitaiRepository jichitaiRepository;

    private final JichitaiContext jichitaiContext;

    @Override
    @Transactional(readOnly = true)
    public void reloadFacilityList(GassanForm form) {
    	String jichitaiCd = jichitaiContext.getJichitaiCd();
        List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, form.getAtenaNo());
        Set<String> checkedSet = form.getShiteiNoList() != null ? Set.copyOf(form.getShiteiNoList()) : Set.of();
        form.setFacilityList(tokugimuList.stream()
                .map(t -> new FacilityItem(t.getShiteiNo(), t.getShisetsuName(), t.getAtena().getName(), checkedSet.contains(t.getShiteiNo())))
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public GassanForm getByGassanShiteiNo(String gassanShiteiNo) {
    	String jichitaiCd = jichitaiContext.getJichitaiCd();
        Gassan gassan = gassanRepository.findByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo)
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("合算申告が見つかりません: " + gassanShiteiNo));

        List<String> checkedShiteiNos = gassanUchiRepository
                .findByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo)
                .stream().map(GassanUchi::getShiteiNo).toList();

        List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, gassan.getAtenaNo());
        Set<String> checkedSet = Set.copyOf(checkedShiteiNos);
        
        // 代表施設は最初にチェックされた施設とする
        String daihyoShiteiNo = checkedShiteiNos.isEmpty() ? null : checkedShiteiNos.get(0);
        
        List<FacilityItem> facilityList = tokugimuList.stream()
                .map(t -> {
                    boolean isChecked = checkedSet.contains(t.getShiteiNo());
                    boolean isDaihyo = t.getShiteiNo().equals(daihyoShiteiNo);
                    FacilityItem item = new FacilityItem(t.getShiteiNo(), t.getShisetsuName(), t.getAtena().getName(), isChecked);
                    item.setDaihyo(isDaihyo);
                    return item;
                })
                .toList();

        Atena atena = atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, gassan.getAtenaNo()).orElse(null);

        GassanForm form = new GassanForm();
        form.setGassanShiteiNo(gassan.getGassanShiteiNo());
        form.setAtenaNo(gassan.getAtenaNo());
        form.setAtenaName(atena != null ? atena.getName() : "");
        form.setTekiyoStYmd(gassan.getTekiyoStYmd());
        form.setTekiyoEdYmd(gassan.getTekiyoEdYmd());
        form.setTorokuYmd(gassan.getTorokuYmd());
        form.setShinkokuYmd(gassan.getShinkokuYmd());
        form.setFacilityList(facilityList);
        form.setShiteiNoList(checkedShiteiNos);
        form.setDaihyoShiteiNo(daihyoShiteiNo);
        form.setRno(gassan.getRno());
        BigDecimal maxRno = gassanRepository.countValidRnoByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo);
        BigDecimal minRno = gassanRepository.findMinRnoByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo);
        form.setMaxRno(maxRno);
        form.setMinRno(minRno);
        form.setPrevRno(gassanRepository.findPrevRnoByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo, gassan.getRno()));
        form.setNextRno(gassanRepository.findNextRnoByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo, gassan.getRno()));
        form.setCurrentNo(gassanRepository.countValidRnoByJichitaiCdAndGassanShiteiNoAndRnoLe(jichitaiCd, gassanShiteiNo, gassan.getRno()));
        return form;
    }

    @Override
    @Transactional(readOnly = true)
    public GassanForm getByGassanShiteiNoAndRno(String gassanShiteiNo, BigDecimal rno) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
        Gassan gassan = gassanRepository.findByJichitaiCdAndGassanShiteiNoAndRno(jichitaiCd, gassanShiteiNo, rno)
                .orElseThrow(() -> new RuntimeException("合算申告が見つかりません: " + gassanShiteiNo + "/rno=" + rno));

        List<String> checkedShiteiNos = gassanUchiRepository
                .findByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo)
                .stream().map(GassanUchi::getShiteiNo).toList();

        List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, gassan.getAtenaNo());
        Set<String> checkedSet = Set.copyOf(checkedShiteiNos);
        String daihyoShiteiNo = checkedShiteiNos.isEmpty() ? null : checkedShiteiNos.get(0);

        List<FacilityItem> facilityList = tokugimuList.stream()
                .map(t -> {
                    boolean isChecked = checkedSet.contains(t.getShiteiNo());
                    boolean isDaihyo = t.getShiteiNo().equals(daihyoShiteiNo);
                    FacilityItem item = new FacilityItem(t.getShiteiNo(), t.getShisetsuName(), t.getAtena().getName(), isChecked);
                    item.setDaihyo(isDaihyo);
                    return item;
                })
                .toList();

        Atena atena = atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, gassan.getAtenaNo()).orElse(null);

        GassanForm form = new GassanForm();
        form.setGassanShiteiNo(gassan.getGassanShiteiNo());
        form.setAtenaNo(gassan.getAtenaNo());
        form.setAtenaName(atena != null ? atena.getName() : "");
        form.setTekiyoStYmd(gassan.getTekiyoStYmd());
        form.setTekiyoEdYmd(gassan.getTekiyoEdYmd());
        form.setTorokuYmd(gassan.getTorokuYmd());
        form.setShinkokuYmd(gassan.getShinkokuYmd());
        form.setFacilityList(facilityList);
        form.setShiteiNoList(checkedShiteiNos);
        form.setDaihyoShiteiNo(daihyoShiteiNo);
        form.setRno(gassan.getRno());
        BigDecimal maxRno = gassanRepository.countValidRnoByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo);
        BigDecimal minRno = gassanRepository.findMinRnoByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo);
        form.setMaxRno(maxRno);
        form.setMinRno(minRno);
        form.setPrevRno(gassanRepository.findPrevRnoByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo, gassan.getRno()));
        form.setNextRno(gassanRepository.findNextRnoByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo, gassan.getRno()));
        form.setCurrentNo(gassanRepository.countValidRnoByJichitaiCdAndGassanShiteiNoAndRnoLe(jichitaiCd, gassanShiteiNo, gassan.getRno()));
        return form;
    }

    @Override
    @Transactional(readOnly = true)
    public GassanForm buildFormByShiteiNo(String shiteiNo) {
    	String jichitaiCd = jichitaiContext.getJichitaiCd();
        Tokugimu tokugimu = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("施設が見つかりません: " + shiteiNo));

        List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, tokugimu.getAtenaNo());
        
        // 合算指定済みの指定番号を取得
        List<String> allShiteiNos = tokugimuList.stream().map(Tokugimu::getShiteiNo).toList();
        List<GassanUchi> assignedList = gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(jichitaiCd, allShiteiNos, null);
        Map<String, String> assignedMap = new HashMap<>();
        for (GassanUchi uchi : assignedList) {
            assignedMap.put(uchi.getShiteiNo(), uchi.getGassanShiteiNo());
        }
        
        List<FacilityItem> facilityList = tokugimuList.stream()
                .map(t -> {
                    FacilityItem item = new FacilityItem(t.getShiteiNo(), t.getShisetsuName(), t.getAtena().getName(), false);
                    if (assignedMap.containsKey(t.getShiteiNo())) {
                        item.setDisabled(true);
                        item.setGassanShiteiNo(assignedMap.get(t.getShiteiNo()));
                    }
                    return item;
                })
                .toList();

        Atena atena = atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, tokugimu.getAtenaNo()).orElse(null);

        GassanForm form = new GassanForm();
        form.setFromShiteiNo(shiteiNo);
        form.setAtenaNo(tokugimu.getAtenaNo());
        form.setAtenaName(atena != null ? atena.getName() : tokugimu.getKyokaName());
        form.setTorokuYmd(LocalDate.now());
        form.setShinkokuYmd(LocalDate.now());
        form.setFacilityList(facilityList);
        return form;
    }

    @Override
    @Transactional
    public String register(GassanForm form, String gassanShiteiNo) {
    	String jichitaiCd = jichitaiContext.getJichitaiCd();
        try {
            String daihyoShiteiNo = form.getDaihyoShiteiNo() != null ? form.getDaihyoShiteiNo()
                    : (form.getShiteiNoList() != null && !form.getShiteiNoList().isEmpty() ? form.getShiteiNoList().get(0) : null);
            if (daihyoShiteiNo == null) {
                throw new RuntimeException("代表施設を選択してください。");
            }

            // 適用開始・終了年月の逆転チェック
            if (form.getTekiyoStYmd() != null && form.getTekiyoEdYmd() != null
                    && !form.getTekiyoEdYmd().isAfter(form.getTekiyoStYmd())) {
                throw new RuntimeException("適用終了年月は適用開始年月より後の年月を入力してください。");
            }

            if (gassanShiteiNo != null) {
                // ③再登録：セッションに合算指定番号あり
                reRegister(jichitaiCd, gassanShiteiNo, daihyoShiteiNo, form);
                return gassanShiteiNo;
            } else {
                // ①新規登録
                validateNotAlreadyAssigned(form.getShiteiNoList(), null);
                String newGassanShiteiNo = generateGassanShiteiNo();
                Gassan gassan = buildGassan(jichitaiCd, newGassanShiteiNo, BigDecimal.ONE, daihyoShiteiNo, form, "1");
                gassanRepository.save(gassan);
                saveGassanUchi(newGassanShiteiNo, BigDecimal.ONE, form.getShiteiNoList());
                log.debug("合算申告登録完了: gassanShiteiNo={}", newGassanShiteiNo);
                return newGassanShiteiNo;
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("合算申告登録エラー", e);
            throw new RuntimeException(convertDatabaseErrorToJapanese(e.getMessage()), e);
        }
    }

    /** ③再登録：前履歴の適用終了年月以降の開始年月で新履歴を追加 */
    private void reRegister(String jichitaiCd, String gassanShiteiNo, String daihyoShiteiNo, GassanForm form) {
        Gassan prev = gassanRepository.findByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo)
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("前履歴が見つかりません: " + gassanShiteiNo));

        if (prev.getTekiyoEdYmd() != null && form.getTekiyoStYmd() != null
                && !form.getTekiyoStYmd().isAfter(prev.getTekiyoEdYmd())) {
            throw new RuntimeException("適用開始年月は前履歴の適用終了年月（" + prev.getTekiyoEdYmd() + "）より後の日付を入力してください。");
        }

        BigDecimal nextRno = gassanRepository.findMaxRnoIncludingDeletedByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo).add(BigDecimal.ONE);
        // 前履歴の newFlg をクリア
        gassanRepository.clearNewFlgByRno(jichitaiCd, gassanShiteiNo, prev.getRno());

        Gassan gassan = buildGassan(jichitaiCd, gassanShiteiNo, nextRno, daihyoShiteiNo, form, "1");
        gassanRepository.save(gassan);
        saveGassanUchi(gassanShiteiNo, nextRno, form.getShiteiNoList());
        log.debug("合算申告再登録完了: gassanShiteiNo={}, rno={}", gassanShiteiNo, nextRno);
    }

    /** 登録・再登録共通の Gassan エンティティ構築 */
    private Gassan buildGassan(String jichitaiCd, String gassanShiteiNo, BigDecimal rno,
            String daihyoShiteiNo, GassanForm form, String newFlg) {
        Gassan gassan = new Gassan();
        gassan.setJichitaiCd(jichitaiCd);
        gassan.setGassanShiteiNo(gassanShiteiNo);
        gassan.setRno(rno);
        gassan.setAtenaNo(form.getAtenaNo());
        gassan.setShiteiNo(daihyoShiteiNo);
        gassan.setTorokuYmd(form.getTorokuYmd());
        gassan.setShinkokuYmd(form.getShinkokuYmd());
        gassan.setTekiyoStYmd(form.getTekiyoStYmd());
        gassan.setTekiyoEdYmd(form.getTekiyoEdYmd());
        gassan.setNewFlg(newFlg);
        gassan.setDelFlg("0");
        return gassan;
    }

    @Override
    @Transactional
    public void updateByGassanShiteiNo(String gassanShiteiNo, GassanForm form) {
    	String jichitaiCd = jichitaiContext.getJichitaiCd();
        try {
            Gassan gassan = gassanRepository.findByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo)
                    .stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("合算申告が見つかりません: " + gassanShiteiNo));

            // ③編集：適用開始年月は前履歴の適用終了年月より後であれば編集可
            if (form.getTekiyoStYmd() != null && gassan.getRno() != null && gassan.getRno().compareTo(BigDecimal.ONE) > 0) {
                BigDecimal prevRno = gassan.getRno().subtract(BigDecimal.ONE);
                gassanRepository.findByJichitaiCdAndGassanShiteiNoAndRno(jichitaiCd, gassanShiteiNo, prevRno)
                        .ifPresent(prev -> {
                            if (prev.getTekiyoEdYmd() != null
                                    && !form.getTekiyoStYmd().isAfter(prev.getTekiyoEdYmd())) {
                                throw new RuntimeException(
                                        "適用開始年月は前履歴の適用終了年月（" + prev.getTekiyoEdYmd() + "）より後の日付を入力してください。");
                            }
                        });
            }

            // 施設は編集不可—現在の内訳をそのまま維持
            if (form.getTekiyoStYmd() != null) {
                gassan.setTekiyoStYmd(form.getTekiyoStYmd());
            }
            gassan.setTekiyoEdYmd(form.getTekiyoEdYmd());

            // 適用開始・終了年月の逆転チェック
            LocalDate stYmd = gassan.getTekiyoStYmd();
            LocalDate edYmd = gassan.getTekiyoEdYmd();
            if (stYmd != null && edYmd != null && !edYmd.isAfter(stYmd)) {
                throw new RuntimeException("適用終了年月は適用開始年月より後の年月を入力してください。");
            }

            gassan.setShinkokuYmd(form.getShinkokuYmd());
            gassan.setTorokuYmd(form.getTorokuYmd());
            gassanRepository.save(gassan);

            log.debug("合算申告更新完了: gassanShiteiNo={}", gassanShiteiNo);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("合算申告更新エラー", e);
            throw new RuntimeException(convertDatabaseErrorToJapanese(e.getMessage()), e);
        }
    }

    @Override
    @Transactional
    public void deleteByGassanShiteiNo(String gassanShiteiNo) {
    	String jichitaiCd = jichitaiContext.getJichitaiCd();
        // 全履歴を rno 降順で取得
        List<Gassan> all = gassanRepository.findAllRnoByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo);
        if (all.isEmpty()) return;

        Gassan latest = all.get(0); // rno 最大（最新）
        // ⑤削除：最新履歴を論理削除
        gassanRepository.deleteLogicallyByRno(jichitaiCd, gassanShiteiNo, latest.getRno());

        // 前履歴があれば newFlg を '1' に戻す
        if (all.size() > 1) {
            Gassan prev = all.get(1);
            gassanRepository.setNewFlgByRno(jichitaiCd, gassanShiteiNo, prev.getRno());
        }
        log.debug("合算申告削除完了: gassanShiteiNo={}, rno={}", gassanShiteiNo, latest.getRno());
    }

    @Override
    @Transactional(readOnly = true)
    public GassanForm getLatestByShiteiNo(String shiteiNo) {
    	String jichitaiCd = jichitaiContext.getJichitaiCd();
        List<Gassan> gassanList = gassanRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
        if (gassanList.isEmpty()) {
            throw new RuntimeException("合算申告が見つかりません: shiteiNo=" + shiteiNo);
        }
        return buildViewForm(shiteiNo, gassanList, gassanList.get(0).getGassanShiteiNo());
    }

    @Override
    @Transactional(readOnly = true)
    public GassanForm getViewFormByShiteiNo(String shiteiNo, String gassanShiteiNo) {
    	String jichitaiCd = jichitaiContext.getJichitaiCd();
        List<Gassan> gassanList = gassanRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
        if (gassanList.isEmpty()) {
            throw new RuntimeException("合算申告が見つかりません: shiteiNo=" + shiteiNo);
        }
        return buildViewForm(shiteiNo, gassanList, gassanShiteiNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GassanForm.FacilityItem> getFacilitiesByAtenaNo(BigDecimal atenaNo) {
    	String jichitaiCd = jichitaiContext.getJichitaiCd();
        List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, atenaNo);
        return tokugimuList.stream()
                .map(t -> new GassanForm.FacilityItem(t.getShiteiNo(), t.getShisetsuName(), t.getAtena().getName(), false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public void validateNotAlreadyAssigned(List<String> shiteiNoList, String excludeGassanShiteiNo) {
    	String jichitaiCd = jichitaiContext.getJichitaiCd();
        if (shiteiNoList == null || shiteiNoList.isEmpty()) {
            return;
        }
        
        List<GassanUchi> existingAssignments = gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(jichitaiCd, shiteiNoList, excludeGassanShiteiNo);
        
        if (!existingAssignments.isEmpty()) {
            List<String> duplicateShiteiNos = existingAssignments.stream()
                    .map(GassanUchi::getShiteiNo)
                    .distinct()
                    .toList();
            
            throw new RuntimeException("以下の指定番号は既に合算申告に登録されています: " + String.join(", ", duplicateShiteiNos));
        }
    }
    
    /**
     * データベースエラーを日本語に変換する
     */
    private String convertDatabaseErrorToJapanese(String originalMessage) {
        if (originalMessage == null) {
            return "データベースエラーが発生しました。";
        }
        
        String lowerMessage = originalMessage.toLowerCase();
        
        if (lowerMessage.contains("null value") && lowerMessage.contains("not-null constraint")) {
            return "必須項目が未入力です。入力内容を確認してください。";
        }
        
        if (lowerMessage.contains("duplicate key") || lowerMessage.contains("unique constraint")) {
            return "重複するデータが存在します。入力内容を確認してください。";
        }
        
        if (lowerMessage.contains("foreign key constraint")) {
            return "関連するデータが存在しないため、操作できません。";
        }
        
        if (lowerMessage.contains("constraint")) {
            return "データ制約エラーが発生しました。入力内容を確認してください。";
        }
        
        if (lowerMessage.contains("timeout")) {
            return "データベースへのアクセスがタイムアウトしました。しばらく待ってから再度お試しください。";
        }
        
        return "データベースエラーが発生しました。しばらく待ってから再度お試しください。";
    }

    private GassanForm buildViewForm(String shiteiNo, List<Gassan> gassanList, String selectedGassanShiteiNo) {
        GassanForm form = getByGassanShiteiNo(selectedGassanShiteiNo);
        form.setGassanList(gassanList.stream()
                .map(g -> new GassanListItem(g.getGassanShiteiNo(), g.getTekiyoStYmd()))
                .toList());
        form.setFromShiteiNo(shiteiNo);
        return form;
    }

    private String generateGassanShiteiNo() {
    	String jichitaiCd = jichitaiContext.getJichitaiCd();
        String prefix = jichitaiRepository.findById(jichitaiCd)
                .map(j -> j.getGassanStChar() != null ? j.getGassanStChar() : "900")
                .orElse("900");
        int max = gassanRepository.findMaxGassanShiteiNoByJichitaiCdAndPrefix(jichitaiCd, prefix).orElse(0);
        return prefix + String.format("%05d", max + 1);
    }

    private void saveGassanUchi(String gassanShiteiNo, BigDecimal rno, List<String> shiteiNoList) {
    	String jichitaiCd = jichitaiContext.getJichitaiCd();
        if (shiteiNoList == null || shiteiNoList.isEmpty()) return;
        for (String shiteiNo : shiteiNoList) {
            GassanUchi uchi = new GassanUchi();
            uchi.setJichitaiCd(jichitaiCd);
            uchi.setGassanShiteiNo(gassanShiteiNo);
            uchi.setRno(rno);
            uchi.setShiteiNo(shiteiNo);
            gassanUchiRepository.save(uchi);
        }
    }
}

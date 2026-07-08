package jp.lg.asp.accommodation.service.impl;
import jp.lg.asp.accommodation.config.JichitaiContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
    
    @PersistenceContext
    private EntityManager entityManager;

    private final JichitaiContext jichitaiContext;

    @Override
    @Transactional(readOnly = true)
    public void reloadFacilityList(GassanForm form) {
    	String jichitaiCd = jichitaiContext.getJichitaiCd();
        List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, form.getAtenaNo());
        Set<String> checkedSet = form.getShiteiNoList() != null ? Set.copyOf(form.getShiteiNoList()) : Set.of();
        form.setFacilityList(tokugimuList.stream()
                .map(t -> new FacilityItem(t.getShiteiNo(), t.getShisetsuName(), t.getKyokaName(), checkedSet.contains(t.getShiteiNo())))
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
                    FacilityItem item = new FacilityItem(t.getShiteiNo(), t.getShisetsuName(), t.getKyokaName(), isChecked);
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
        List<GassanUchi> assignedList = gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(jichitaiCd, allShiteiNos);
        Map<String, String> assignedMap = new HashMap<>();
        for (GassanUchi uchi : assignedList) {
            assignedMap.put(uchi.getShiteiNo(), uchi.getGassanShiteiNo());
        }
        
        List<FacilityItem> facilityList = tokugimuList.stream()
                .map(t -> {
                    FacilityItem item = new FacilityItem(t.getShiteiNo(), t.getShisetsuName(), t.getKyokaName(), false);
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
    public void register(GassanForm form) {
    	String jichitaiCd = jichitaiContext.getJichitaiCd();
        try {
            // 既に合算指定済みの指定番号が含まれていないかチェック
            validateNotAlreadyAssigned(form.getShiteiNoList());
            
            // 代表施設を取得（最初の施設または指定された代表施設）
            String daihyoShiteiNo = form.getDaihyoShiteiNo() != null ? form.getDaihyoShiteiNo() 
                    : (form.getShiteiNoList() != null && !form.getShiteiNoList().isEmpty() ? form.getShiteiNoList().get(0) : null);
            
            if (daihyoShiteiNo == null) {
                throw new RuntimeException("代表施設を選択してください。");
            }
            
            String gassanShiteiNo = generateGassanShiteiNo();

            Gassan gassan = new Gassan();
            gassan.setJichitaiCd(jichitaiCd);
            gassan.setGassanShiteiNo(gassanShiteiNo);
            gassan.setRno(BigDecimal.ONE);
            gassan.setAtenaNo(form.getAtenaNo());
            gassan.setShiteiNo(daihyoShiteiNo); // 代表施設の指定番号を設定
            gassan.setTorokuYmd(form.getTorokuYmd());
            gassan.setShinkokuYmd(form.getShinkokuYmd());
            gassan.setTekiyoStYmd(form.getTekiyoStYmd());
            gassan.setTekiyoEdYmd(form.getTekiyoEdYmd());
            gassan.setNewFlg("1");
            gassan.setDelFlg("0");
            gassanRepository.save(gassan);

            saveGassanUchi(gassanShiteiNo, BigDecimal.ONE, form.getShiteiNoList());
            log.info("合算申告登録完了: gassanShiteiNo={}", gassanShiteiNo);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("合算申告登録エラー", e);
            throw new RuntimeException(convertDatabaseErrorToJapanese(e.getMessage()), e);
        }
    }

    @Override
    @Transactional
    public void updateByGassanShiteiNo(String gassanShiteiNo, GassanForm form) {
    	String jichitaiCd = jichitaiContext.getJichitaiCd();
        try {
            Gassan gassan = gassanRepository.findByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo)
                    .stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("合算申告が見つかりません: " + gassanShiteiNo));

            // 編集時は現在のレコードを除外して重複チェック
            validateNotAlreadyAssignedForUpdate(gassanShiteiNo, form.getShiteiNoList());
            
            // 代表施設を取得（最初の施設または指定された代表施設）
            String daihyoShiteiNo = form.getDaihyoShiteiNo() != null ? form.getDaihyoShiteiNo() 
                    : (form.getShiteiNoList() != null && !form.getShiteiNoList().isEmpty() ? form.getShiteiNoList().get(0) : null);
            
            if (daihyoShiteiNo == null) {
                throw new RuntimeException("代表施設を選択してください。");
            }
            
            // メインテーブルの更新
            gassan.setShiteiNo(daihyoShiteiNo); // 代表施設の指定番号を設定
            gassan.setTekiyoStYmd(form.getTekiyoStYmd());
            gassan.setTekiyoEdYmd(form.getTekiyoEdYmd());
            gassan.setShinkokuYmd(form.getShinkokuYmd());
            gassan.setTorokuYmd(form.getTorokuYmd());
            gassanRepository.save(gassan);

            // 内訳テーブルの更新（完全にセッションをクリア）
            gassanUchiRepository.deleteByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo);
            entityManager.flush();
            entityManager.clear(); // セッションを完全にクリア
            
            // 新しい内訳データを一括作成・保存
            if (form.getShiteiNoList() != null && !form.getShiteiNoList().isEmpty()) {
                List<GassanUchi> newUchiList = form.getShiteiNoList().stream()
                        .map(shiteiNo -> {
                            GassanUchi uchi = new GassanUchi();
                            uchi.setJichitaiCd(jichitaiCd);
                            uchi.setGassanShiteiNo(gassanShiteiNo);
                            uchi.setRno(gassan.getRno());
                            uchi.setShiteiNo(shiteiNo);
                            return uchi;
                        })
                        .toList();
                
                gassanUchiRepository.saveAll(newUchiList);
            }
            
            log.info("合算申告更新完了: gassanShiteiNo={}", gassanShiteiNo);
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
        gassanRepository.deleteLogicallyByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo);
        log.info("合算申告論理削除完了: gassanShiteiNo={}", gassanShiteiNo);
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
                .map(t -> new GassanForm.FacilityItem(t.getShiteiNo(), t.getShisetsuName(), t.getKyokaName(), false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public void validateNotAlreadyAssigned(List<String> shiteiNoList) {
    	String jichitaiCd = jichitaiContext.getJichitaiCd();
        if (shiteiNoList == null || shiteiNoList.isEmpty()) {
            return;
        }
        
        // 現在有効な合算申告内訳から該当する指定番号を検索
        List<GassanUchi> existingAssignments = gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(jichitaiCd, shiteiNoList);
        
        if (!existingAssignments.isEmpty()) {
            // 重複している指定番号のリストを作成
            List<String> duplicateShiteiNos = existingAssignments.stream()
                    .map(GassanUchi::getShiteiNo)
                    .distinct()
                    .toList();
            
            throw new RuntimeException("以下の指定番号は既に合算申告に登録されています: " + String.join(", ", duplicateShiteiNos));
        }
    }
    
    private void validateNotAlreadyAssignedForUpdate(String currentGassanShiteiNo, List<String> shiteiNoList) {
    	String jichitaiCd = jichitaiContext.getJichitaiCd();
        if (shiteiNoList == null || shiteiNoList.isEmpty()) {
            return;
        }
        
        // 現在編集中のレコード以外で重複チェック
        List<GassanUchi> existingAssignments = gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(jichitaiCd, shiteiNoList)
                .stream()
                .filter(uchi -> !uchi.getGassanShiteiNo().equals(currentGassanShiteiNo))
                .toList();
        
        if (!existingAssignments.isEmpty()) {
            List<String> duplicateShiteiNos = existingAssignments.stream()
                    .map(GassanUchi::getShiteiNo)
                    .distinct()
                    .toList();
            
            throw new RuntimeException("以下の指定番号は既に他の合算申告に登録されています: " + String.join(", ", duplicateShiteiNos));
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

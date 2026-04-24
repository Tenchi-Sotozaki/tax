package jp.lg.asp.accommodation.service.impl;

import jp.lg.asp.accommodation.dto.CollectorForm;
import jp.lg.asp.accommodation.dto.CollectorListItem;
import jp.lg.asp.accommodation.dto.CollectorSearchForm;
import jp.lg.asp.accommodation.dto.FacilityDto;
import jp.lg.asp.accommodation.dto.TaxManagerForm;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.GassanUchi;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.GassanUchiRepository;
import jp.lg.asp.accommodation.repository.NokanRepository;
import jp.lg.asp.accommodation.repository.ShoyushaRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.CollectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectorServiceImpl implements CollectorService {

    private final TokugimuRepository tokugimuRepository;
    private final AtenaRepository atenaRepository;
    private final GassanUchiRepository gassanUchiRepository;
    private final ShoyushaRepository shoyushaRepository;
    private final NokanRepository nokanRepository;
    
    // 固定の自治体コード（実際の運用では設定ファイルから取得）
    private static final String JICHITAI_CD = "01234";

    @Override
    @Transactional(readOnly = true)
    public List<CollectorListItem> search(CollectorSearchForm form) {
        // データベースから検索
        List<Tokugimu> tokugimuList = tokugimuRepository.findBySearchConditions(
            form.getShiteiNo(),
            form.getName(),
            form.getShisetsuName(),
            form.getKyokaShu(),
            form.getKojinNo(),
            form.getHojinNo()
        );

        if (tokugimuList.isEmpty()) {
            return List.of();
        }

        // 関連データを一括取得
        List<BigDecimal> atenaNos = tokugimuList.stream().map(Tokugimu::getAtenaNo).toList();
        List<String> shiteiNos = tokugimuList.stream().map(Tokugimu::getShiteiNo).toList();
        
        Map<BigDecimal, Atena> atenaMap = atenaRepository.findByJichitaiCdAndAtenaNoIn(JICHITAI_CD, atenaNos)
            .stream().collect(Collectors.toMap(Atena::getAtenaNo, a -> a));
        
        Map<String, Boolean> gassanMap = gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(JICHITAI_CD, shiteiNos)
            .stream().collect(Collectors.toMap(GassanUchi::getShiteiNo, g -> true, (a, b) -> a));

        // 結果をDTOに変換
        return tokugimuList.stream()
            .map(t -> {
                Atena atena = atenaMap.get(t.getAtenaNo());
                boolean isGassanTarget = gassanMap.containsKey(t.getShiteiNo());
                String status = t.getStatus();
                
                // ステータスフィルタリング
                if (form.getStatus() != null && !"4".equals(form.getStatus()) && !form.getStatus().equals(status)) {
                    return null;
                }
                
                // 合算対象フィルタリング
                if (form.getGasanTaisho() != null && !"999".equals(form.getGasanTaisho())) {
                    boolean shouldBeTarget = "2".equals(form.getGasanTaisho());
                    if (shouldBeTarget != isGassanTarget) {
                        return null;
                    }
                }
                
                return new CollectorListItem(
                    t.getAtenaNo().longValue(), // IDとして宛名番号を使用
                    t.getShiteiNo(),
                    atena != null ? atena.getName() : t.getKyokaName(),
                    t.getShisetsuName(),
                    t.getKyokaShu(),
                    getBusinessTypeLabel(t.getKyokaShu()),
                    isGassanTarget ? "target" : "non-target",
                    status,
                    atena != null ? atena.getKojinNo() : null,
                    atena != null ? atena.getHojinNo() : null
                );
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

    @Override
    @Transactional(readOnly = true)
    public CollectorForm getCollectorById(Long id) {
        // 宛名番号からデータを取得
        BigDecimal atenaNo = BigDecimal.valueOf(id);
        Atena atena = atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, atenaNo)
            .orElseThrow(() -> new RuntimeException("特別徴収義務者が見つかりません: " + id));
        
        CollectorForm form = new CollectorForm();
        form.setId(id);
        form.setObligorName(atena.getName());
        // 他のフィールドは必要に応じて追加
        
        return form;
    }

    @Override
    public String getObligorName(String obligorId) {
        try {
            Long id = Long.parseLong(obligorId);
            BigDecimal atenaNo = BigDecimal.valueOf(id);
            Atena atena = atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, atenaNo).orElse(null);
            return atena != null ? atena.getName() : "不明";
        } catch (NumberFormatException e) {
            log.warn("無効なID形式: {}", obligorId);
            return "不明";
        }
    }

    @Override
    public List<FacilityDto> getFacilities(String obligorId) {
        // 施設情報は別テーブルから取得する予定ですが、今はダミーデータを返します
        String name = getObligorName(obligorId);
        return List.of(
            new FacilityDto(obligorId + "-F1", name + " 本館", "東京都新宿区西新宿1-1-1")
        );
    }

    @Override
    public TaxManagerForm buildTaxManagerForm(Long collectorId) {
        CollectorForm collector = getCollectorById(collectorId);
        TaxManagerForm form = new TaxManagerForm();
        form.setCollectorId(collectorId);
        form.setObligorName(collector.getObligorName());
        return form;
    }

    @Override
    @Transactional
    public void register(CollectorForm form) {
        // 実装予定
        log.info("特別徴収義務者登録: {}", form.getObligorName());
    }

    @Override
    @Transactional
    public void update(Long id, CollectorForm form) {
        // 実装予定
        log.info("特別徴収義務者更新: id={}, name={}", id, form.getObligorName());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        BigDecimal atenaNo = BigDecimal.valueOf(id);
        
        // 削除対象の特別徴収義務者情報を取得
        Tokugimu tokugimu = tokugimuRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, atenaNo)
            .orElseThrow(() -> new RuntimeException("削除対象の特別徴収義務者が見つかりません: " + id));
        
        String shiteiNo = tokugimu.getShiteiNo();
        String obligorName = tokugimu.getKyokaName();
        
        try {
            // 関連データを順番に削除
            log.info("特別徴収義務者削除開始: id={}, 指定番号={}, 名称={}", id, shiteiNo, obligorName);
            
            // 1. 所有者情報を削除
            shoyushaRepository.deleteByJichitaiCdAndShiteiNo(JICHITAI_CD, shiteiNo);
            log.debug("所有者情報削除完了: 指定番号={}", shiteiNo);
            
            // 2. 納税管理人情報を削除
            nokanRepository.deleteByJichitaiCdAndShiteiNo(JICHITAI_CD, shiteiNo);
            log.debug("納税管理人情報削除完了: 指定番号={}", shiteiNo);
            
            // 3. 合算申告内訳を削除
            gassanUchiRepository.deleteByJichitaiCdAndShiteiNo(JICHITAI_CD, shiteiNo);
            log.debug("合算申告内訳削除完了: 指定番号={}", shiteiNo);
            
            // 4. 特別徴収義務者情報を削除
            tokugimuRepository.deleteByJichitaiCdAndAtenaNo(JICHITAI_CD, atenaNo);
            log.debug("特別徴収義務者情報削除完了: 宛名番号={}", atenaNo);
            
            // 5. 宛名情報を削除（最後に削除）
            atenaRepository.deleteByJichitaiCdAndAtenaNo(JICHITAI_CD, atenaNo);
            log.debug("宛名情報削除完了: 宛名番号={}", atenaNo);
            
            log.info("特別徴収義務者削除完了: id={}, 指定番号={}, 名称={}", id, shiteiNo, obligorName);
            
        } catch (Exception e) {
            log.error("特別徴収義務者削除エラー: id={}, 指定番号={}, 名称={}", id, shiteiNo, obligorName, e);
            throw new RuntimeException("特別徴収義務者の削除に失敗しました: " + obligorName, e);
        }
    }

    @Override
    @Transactional
    public void saveTaxManager(Long collectorId, TaxManagerForm form) {
        // 実装予定
        log.info("納税管理人登録: collectorId={}, manager={}", collectorId, form.getManagerName());
    }

    @Override
    @Transactional(readOnly = true)
    public String getShiteiNoById(Long id) {
        BigDecimal atenaNo = BigDecimal.valueOf(id);
        return tokugimuRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, atenaNo)
            .map(Tokugimu::getShiteiNo)
            .orElseThrow(() -> new RuntimeException("指定番号が見つかりません: " + id));
    }
}
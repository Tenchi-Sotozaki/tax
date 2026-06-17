package jp.lg.asp.accommodation.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.dto.GassanDaichoItem;
import jp.lg.asp.accommodation.dto.GassanDaichoSearchForm;
import jp.lg.asp.accommodation.entity.Gassan;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.entity.GassanUchi;
import jp.lg.asp.accommodation.repository.GassanRepository;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.repository.GassanUchiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GassanDaichoServiceImpl implements GassanDaichoService {

    private final GassanRepository gassanRepository;
    private final TokugimuRepository tokugimuRepository;
    private final AtenaRepository atenaRepository;
    private final GassanUchiRepository gassanUchiRepository;

    @Value("${app.jichitai.code}")
    private String jichitaiCd;

    @Override
    public List<GassanDaichoItem> search(GassanDaichoSearchForm searchForm) {
        log.info("合算申告情報管理台帳検索開始: {}", searchForm);
        
        List<Gassan> gassanList = gassanRepository.findAllByJichitaiCd(jichitaiCd);
        
        // 検索条件でフィルタリング
        if (searchForm.getGassanShiteiNo() != null && !searchForm.getGassanShiteiNo().isEmpty()) {
            gassanList = gassanList.stream()
                    .filter(g -> g.getGassanShiteiNo().contains(searchForm.getGassanShiteiNo()))
                    .collect(Collectors.toList());
        }
        
        if (searchForm.getShiteiNo() != null && !searchForm.getShiteiNo().isEmpty()) {
            gassanList = gassanList.stream()
                    .filter(g -> g.getShiteiNo() != null && g.getShiteiNo().contains(searchForm.getShiteiNo()))
                    .collect(Collectors.toList());
        }

        // 合算指定番号でグループ化して変換
        return gassanList.stream()
                .collect(Collectors.groupingBy(Gassan::getGassanShiteiNo))
                .entrySet().stream()
                .map(entry -> {
                    String gassanShiteiNo = entry.getKey();
                    List<Gassan> group = entry.getValue();
                    
                    // 代表施設（rno=1）を取得
                    Gassan daihyo = group.stream()
                            .filter(g -> g.getRno().compareTo(BigDecimal.ONE) == 0)
                            .findFirst()
                            .orElse(group.get(0));
                    
                    return convertToGassanDaichoItem(gassanShiteiNo, daihyo, group);
                })
                .filter(item -> {
                    // 氏名/名称でのフィルタリング
                    if (searchForm.getName() != null && !searchForm.getName().isEmpty()) {
                        return item.getName() != null && item.getName().contains(searchForm.getName());
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    @Override
    public GassanDaichoItem getByGassanShiteiNo(String gassanShiteiNo) {
        log.info("合算申告情報詳細取得: {}", gassanShiteiNo);
        
        List<Gassan> gassanList = gassanRepository.findByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo);
        
        if (gassanList.isEmpty()) {
            return null;
        }
        
        // 代表施設（rno=1）を取得
        Gassan daihyo = gassanList.stream()
                .filter(g -> g.getRno().compareTo(BigDecimal.ONE) == 0)
                .findFirst()
                .orElse(gassanList.get(0));
        
        return convertToGassanDaichoItem(gassanShiteiNo, daihyo, gassanList);
    }
    
    private GassanDaichoItem convertToGassanDaichoItem(String gassanShiteiNo, Gassan daihyo, List<Gassan> gassanList) {
        GassanDaichoItem item = new GassanDaichoItem();
        item.setGassanShiteiNo(gassanShiteiNo);
        
        // 合算内訳テーブルから代表指定番号を取得
        List<GassanUchi> gassanUchiList = gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo);
        
        if (!gassanUchiList.isEmpty()) {
            // 代表施設（最初のレコード）を取得
            GassanUchi daihyoUchi = gassanUchiList.get(0);
            String daihyoShiteiNo = daihyoUchi.getShiteiNo();
            
            // 代表施設情報を取得
            List<Tokugimu> daihyoTokugimuList = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, daihyoShiteiNo);
            if (!daihyoTokugimuList.isEmpty()) {
                Tokugimu daihyoTokugimu = daihyoTokugimuList.get(0);
                item.setDaihyoShisetsuName(daihyoTokugimu.getShisetsuName());
                item.setShiteiNo(daihyoTokugimu.getShiteiNo());
                
                // 宛名情報を取得
                atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, daihyoTokugimu.getAtenaNo())
                    .ifPresent(atena -> {
                        item.setName(atena.getName());
                        item.setAtenaNo(atena.getAtenaNo());
                    });
            }
            
            // 合算対象施設リストを作成
            List<GassanDaichoItem.GassanFacilityItem> facilityList = new ArrayList<>();
            for (GassanUchi gassanUchi : gassanUchiList) {
                List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, gassanUchi.getShiteiNo());
                if (!tokugimuList.isEmpty()) {
                    Tokugimu tokugimu = tokugimuList.get(0);
                    GassanDaichoItem.GassanFacilityItem facilityItem = new GassanDaichoItem.GassanFacilityItem();
                    facilityItem.setShiteiNo(tokugimu.getShiteiNo());
                    facilityItem.setShisetsuName(tokugimu.getShisetsuName());
                    facilityItem.setAtenaNo(tokugimu.getAtenaNo());
                    
                    // 宛名情報を取得
                    atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, tokugimu.getAtenaNo())
                        .ifPresent(atena -> facilityItem.setName(atena.getName()));
                    
                    facilityList.add(facilityItem);
                }
            }
            item.setFacilityList(facilityList);
        }
        
        return item;
    }
}
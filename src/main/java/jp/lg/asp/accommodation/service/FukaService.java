package jp.lg.asp.accommodation.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.FukaDaichoForm;
import jp.lg.asp.accommodation.dto.FukaDaichoListItem;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FukaService {

    private final FukaRepository fukaRepository;
    private final TokugimuRepository tokugimuRepository;
    
    @Value("${app.jichitai.code}")
    private String jichitaiCd;

    // 規約に基づく定数
    private static final String STATUS_ALL = "999";
    private static final String STATUS_ZUMI = "1";
    private static final String STATUS_MI = "2";

    /**
     * 納入金額管理台帳のデータを取得する
     */
    @Transactional(readOnly = true)
    public FukaDaichoForm getDaichoData(String shiteiNo, String nendo, String status) {
        FukaDaichoForm form = new FukaDaichoForm();
        form.setShiteiNo(shiteiNo);
        form.setNendo(nendo);
        form.setStatus(status != null ? status : STATUS_ALL);

        // 1. ヘッダー情報（特別徴収義務者名称）の取得
        tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
                .stream()        
                .findFirst()
                .ifPresent(tokugimu -> form.setObligorName(tokugimu.getKyokaName()));

        // 仮設定：今回は「毎月申告（全12期）」として処理（※後日マスタと連動）
        form.setShukiKbnName("毎月申告");
        int maxKibetsu = 12; 

        // 2. DBから実データ（t_fuka）を取得し、期別(kibetsu)をキーにしたMapに変換
        List<Fuka> fukaList = fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(jichitaiCd, shiteiNo, nendo);
        Map<Integer, Fuka> fukaMap = fukaList.stream()
                .collect(Collectors.toMap(Fuka::getKibetsu, f -> f));

        // 3. 1期〜12期までのリストを生成し、DBデータがあればセット、なければ「未」とする
        List<FukaDaichoListItem> items = new ArrayList<>();
        
        for (int i = 1; i <= maxKibetsu; i++) {
            FukaDaichoListItem item = new FukaDaichoListItem();
            item.setNendo(nendo);
            item.setKibetsu(i);
            
            // 例: 1期なら4月、2期なら5月... (※業務要件に合わせて調整)
            int displayMonth = (i + 3) > 12 ? (i + 3) - 12 : (i + 3);
            item.setDisplayNengetsu(displayMonth + "月");
            
            // 例: 翌月末を納期とする簡易計算
            int nokiMonth = displayMonth == 12 ? 1 : displayMonth + 1;
            item.setDisplayNoki(nokiMonth + "月末");

         // DBにこの期のデータが存在するか？s
            if (fukaMap.containsKey(i)) {
                Fuka dbData = fukaMap.get(i);
                
                // 両方の項目に同じ金額をセット
                item.setAmount(dbData.getTotalZeigaku()); 
                item.setTotalZeigaku(dbData.getTotalZeigaku()); // ★ここを追加
                
                item.setStatus("済");
                item.setDisplayNengetsu(displayMonth + "月"); 
                
                int year = Integer.parseInt(nendo);
                if (displayMonth < 4) year++;
                item.setTargetYearMonth(LocalDate.of(year, displayMonth, 1));
                
                item.setShinkokuYmd(dbData.getShinkokuYmd());
                item.setShinkokuZumi(true);
            } else {
                item.setAmount(0L);
                item.setTotalZeigaku(0L); 
                item.setStatus("未");
                item.setDisplayNengetsu(displayMonth + "月");
                
                int year = Integer.parseInt(nendo);
                if (displayMonth < 4) year++;
                item.setTargetYearMonth(LocalDate.of(year, displayMonth, 1));
                
                item.setShinkokuZumi(false);
            }
            // 4. 画面のステータス絞り込み（すべて/済/未）を適用
            if (STATUS_ZUMI.equals(form.getStatus()) && !item.isShinkokuZumi()) continue;
            if (STATUS_MI.equals(form.getStatus()) && item.isShinkokuZumi()) continue;

            items.add(item);
        }

        form.setItems(items);
        return form;
    }
    
    
}
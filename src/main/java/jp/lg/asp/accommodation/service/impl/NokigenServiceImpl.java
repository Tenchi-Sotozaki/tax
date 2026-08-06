package jp.lg.asp.accommodation.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.entity.Kyugyobi;
import jp.lg.asp.accommodation.entity.Nokigen;
import jp.lg.asp.accommodation.entity.NokigenId;
import jp.lg.asp.accommodation.repository.HolidayRepository;
import jp.lg.asp.accommodation.repository.NokigenRepository;
import jp.lg.asp.accommodation.service.NokigenService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NokigenServiceImpl implements NokigenService {

    private final NokigenRepository nokigenRepository;
    private final HolidayRepository holidayRepository;
    private final JichitaiContext jichitaiContext;

    @Override
    @Transactional(readOnly = true)
    public List<Nokigen> findAll() {
        String jichitaiCd = jichitaiContext.getJichitaiCd();
        return nokigenRepository.findAllByJichitaiCd(jichitaiCd);
    }

    @Override
    @Transactional(readOnly = true)
    public Nokigen findByNendo(String nendo) {
        String jichitaiCd = jichitaiContext.getJichitaiCd();
        return nokigenRepository.findById(new NokigenId(jichitaiCd, nendo)).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByNendo(String nendo) {
        String jichitaiCd = jichitaiContext.getJichitaiCd();
        return nokigenRepository.countByJichitaiCdAndNendo(jichitaiCd, nendo) > 0;
    }

    @Override
    @Transactional
    public Nokigen save(Nokigen nokigen) {
        String jichitaiCd = jichitaiContext.getJichitaiCd();
        nokigen.setJichitaiCd(jichitaiCd);
        // HTMLのdate入力(yyyy-MM-dd)をDBのchar(8)(yyyyMMdd)に変換
        nokigen.setNokigen1st(toDbDate(nokigen.getNokigen1st()));
        nokigen.setNokigen2nd(toDbDate(nokigen.getNokigen2nd()));
        nokigen.setNokigen3rd(toDbDate(nokigen.getNokigen3rd()));
        nokigen.setNokigen4th(toDbDate(nokigen.getNokigen4th()));
        nokigen.setNokigen5th(toDbDate(nokigen.getNokigen5th()));
        nokigen.setNokigen6th(toDbDate(nokigen.getNokigen6th()));
        nokigen.setNokigen7th(toDbDate(nokigen.getNokigen7th()));
        nokigen.setNokigen8th(toDbDate(nokigen.getNokigen8th()));
        nokigen.setNokigen9th(toDbDate(nokigen.getNokigen9th()));
        nokigen.setNokigen10th(toDbDate(nokigen.getNokigen10th()));
        nokigen.setNokigen11th(toDbDate(nokigen.getNokigen11th()));
        nokigen.setNokigen12th(toDbDate(nokigen.getNokigen12th()));
        return nokigenRepository.save(nokigen);
    }

    /** yyyy-MM-dd → yyyyMMdd 変換。null/空の場合は空文字を返す */
    private String toDbDate(String value) {
        if (value == null || value.isBlank()) return "";
        return value.replace("-", "");
    }
    
    private static final String SENTINEL_CD = "99999";
    private static final LocalDate SENTINEL_DATE = LocalDate.of(1, 1, 1);

    /**
     * 休業日を考慮してずらす
     */
    @Override
    public Map<String, String> getPrevDataWithShift(Nokigen prev, String targetNendo, String shiftMode) {
        String jichitaiCd = jichitaiContext.getJichitaiCd();

        // 対象年度および次年度の休業日を一括取得（番兵レコード除外、未登録時は99999にフォールバック）
        Set<LocalDate> holidaySet = new HashSet<>();
        addHolidaysToSet(holidaySet, jichitaiCd, targetNendo);
        try {
            addHolidaysToSet(holidaySet, jichitaiCd, String.valueOf(Integer.parseInt(targetNendo) + 1));
        } catch (NumberFormatException ignored) {}

        Map<String, String> result = new LinkedHashMap<>();
        
        result.put("nokigen1st", shiftDate(prev.getNokigen1st(), targetNendo, shiftMode, holidaySet));
        result.put("nokigen2nd", shiftDate(prev.getNokigen2nd(), targetNendo, shiftMode, holidaySet));
        result.put("nokigen3rd", shiftDate(prev.getNokigen3rd(), targetNendo, shiftMode, holidaySet));
        result.put("nokigen4th", shiftDate(prev.getNokigen4th(), targetNendo, shiftMode, holidaySet));
        result.put("nokigen5th", shiftDate(prev.getNokigen5th(), targetNendo, shiftMode, holidaySet));
        result.put("nokigen6th", shiftDate(prev.getNokigen6th(), targetNendo, shiftMode, holidaySet));
        result.put("nokigen7th", shiftDate(prev.getNokigen7th(), targetNendo, shiftMode, holidaySet));
        result.put("nokigen8th", shiftDate(prev.getNokigen8th(), targetNendo, shiftMode, holidaySet));
        result.put("nokigen9th", shiftDate(prev.getNokigen9th(), targetNendo, shiftMode, holidaySet));
        result.put("nokigen10th", shiftDate(prev.getNokigen10th(), targetNendo, shiftMode, holidaySet));
        result.put("nokigen11th", shiftDate(prev.getNokigen11th(), targetNendo, shiftMode, holidaySet));
        result.put("nokigen12th", shiftDate(prev.getNokigen12th(), targetNendo, shiftMode, holidaySet));
        
        return result;
    }

    private void addHolidaysToSet(Set<LocalDate> set, String jichitaiCd, String nen) {
        List<Kyugyobi> list = holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi(jichitaiCd, nen);
        if (list.isEmpty()) {
            list = holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi(SENTINEL_CD, nen);
        }
        for (Kyugyobi h : list) {
            if (h.getKyugyobi() != null && !SENTINEL_DATE.equals(h.getKyugyobi())) {
                set.add(h.getKyugyobi());
            }
        }
    }

    private String shiftDate(String originalValue, String targetNendo, String shiftMode, Set<LocalDate> holidaySet) {
        if (originalValue == null || originalValue.isBlank() || originalValue.length() != 8) {
            return "";
        }
        
        try {
            // 前年度の日付から月日を抽出し、targetNendoの年を適用
            String monthDay = originalValue.substring(4, 8);
            LocalDate date = LocalDate.parse(targetNendo + monthDay, DateTimeFormatter.ofPattern("yyyyMMdd"));
            
            // シフトモードが "none" の場合はそのまま返却
            if ("none".equals(shiftMode)) {
                return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
            
            // 休業日に該当しなくなるまでループしてずらす
            int addDays = "friday".equals(shiftMode) ? -1 : 1;
            while (isHoliday(date, holidaySet)) {
                date = date.plusDays(addDays);
            }
            
            return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * DBに含まれる日であれば休業日と判定
     */
    private boolean isHoliday(LocalDate date, Set<LocalDate> holidaySet) {
        return holidaySet.contains(date);
    }
}
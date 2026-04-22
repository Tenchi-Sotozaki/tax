package jp.lg.asp.accommodation.service;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.TaxManagerForm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaxManagerService {

    private final JdbcTemplate jdbcTemplate;
    // 既に存在するCollectorServiceを呼び出して、IDから指定番号を検索できるようにします！
    private final CollectorService collectorService; 

    /**
     * IDからデータを取得し、画面表示用のFormを作成する
     */
    public TaxManagerForm getById(Long id) {
        TaxManagerForm form = new TaxManagerForm();
        form.setCollectorId(id);
        form.setRegistrationDate(LocalDate.now()); // 登録日の初期値を今日にする

        try {
            // 1. Long型のIDから、String型の指定番号（shitei_no）を取得する
            String shiteiNo = collectorService.getShiteiNoById(id);

            // 2. 指定番号を使って、t_tokugimuテーブルから名称を取得
            String sql = "SELECT kyoka_name, shisetsu_name FROM t_tokugimu WHERE shitei_no = ? AND del_flg = '0'";
            Map<String, Object> result = jdbcTemplate.queryForMap(sql, shiteiNo);
            
            form.setObligorName((String) result.get("kyoka_name"));
            form.setFacilityName((String) result.get("shisetsu_name"));
            
        } catch (Exception e) {
            log.warn("データの取得に失敗しました。ID: {}", id, e);
            form.setObligorName("データ取得エラー");
            form.setFacilityName("データ取得エラー");
        }

        return form;
    }

    /**
     * 入力されたFormのデータをDB（t_nokan）に保存する
     */
    @Transactional
    public void save(Long id, TaxManagerForm form) {
        // 選任免除フラグの boolean を '1' か '0' に変換
        String menjoKbn = form.isExemptionFlag() ? "1" : "0";

        // 保存時も、Long型のIDから指定番号（shitei_no）に変換して使います
        String shiteiNo = collectorService.getShiteiNoById(id);

        String sql = "INSERT INTO t_nokan " +
                     "(jichitai_cd, shitei_no, rno, toroku_ymd, shinkoku_ymd, name, name_kana, jusho, tel, menjo_kbn, menjo_riyu, new_flg, del_flg, add_dt, add_user, upd_dt, upd_user, version) " +
                     "VALUES ('01202', ?, 1, ?, CURRENT_DATE, ?, ?, ?, ?, ?, ?, '1', '0', CURRENT_TIMESTAMP, 'sys', CURRENT_TIMESTAMP, 'sys', 1)";

        jdbcTemplate.update(sql, 
                shiteiNo, 
                form.getRegistrationDate(), 
                form.getManagerName(), 
                form.getManagerNameKana(), 
                form.getManagerAddress(), 
                form.getManagerPhone(), 
                menjoKbn, 
                form.getExemptionReason()
        );
    }
}
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
        
        // 初期値として今日の日付をセット（データがない場合のデフォルト）
        form.setRegistrationDate(LocalDate.now());

        try {
            // 1. 指定番号（shitei_no）を取得
            String shiteiNo = collectorService.getShiteiNoById(id);

            // 2. 特別徴収義務者テーブル（t_tokugimu）から施設情報を取得
            String sqlTokugimu = "SELECT kyoka_name, shisetsu_name FROM t_tokugimu WHERE shitei_no = ? AND del_flg = '0'";
            Map<String, Object> resTokugimu = jdbcTemplate.queryForMap(sqlTokugimu, shiteiNo);
            form.setObligorName((String) resTokugimu.get("kyoka_name"));
            form.setFacilityName((String) resTokugimu.get("shisetsu_name"));

            // 3. 納税管理人テーブル（t_nokan）から保存済みのデータを取得
            String sqlNokan = "SELECT toroku_ymd, name, name_kana, jusho, tel, menjo_kbn, menjo_riyu " +
                              "FROM t_nokan WHERE jichitai_cd = '01202' AND shitei_no = ? AND rno = 1 AND del_flg = '0'";
            
            var nokanList = jdbcTemplate.queryForList(sqlNokan, shiteiNo);

            if (!nokanList.isEmpty()) {
                // データが存在する場合（編集・照会モード）
                form.setEdit(true);
                Map<String, Object> resNokan = nokanList.get(0);
                
                // DBから取得した「登録日」をセット
                if (resNokan.get("toroku_ymd") != null) {
                    form.setRegistrationDate(((java.sql.Date) resNokan.get("toroku_ymd")).toLocalDate());
                }
                
                // 保存されている管理人の情報をセット
                form.setManagerName((String) resNokan.get("name"));
                form.setManagerNameKana((String) resNokan.get("name_kana"));
                form.setManagerAddress((String) resNokan.get("jusho"));
                form.setManagerPhone((String) resNokan.get("tel"));
                form.setExemptionFlag("1".equals(resNokan.get("menjo_kbn")));
                form.setExemptionReason((String) resNokan.get("menjo_riyu"));
            }

        } catch (Exception e) {
            log.warn("データの取得に失敗しました。ID: {}", id, e.getMessage());
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

        // 1. 既にデータが存在するかチェックする
        String checkSql = "SELECT COUNT(*) FROM t_nokan WHERE jichitai_cd = '01202' AND shitei_no = ? AND rno = 1";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, shiteiNo);

        if (count != null && count > 0) {
            // ==========================================
            // パターンA：既に存在する場合は UPDATE（更新）
            // ==========================================
            String updateSql = "UPDATE t_nokan SET " +
                    "toroku_ymd = ?, name = ?, name_kana = ?, jusho = ?, tel = ?, " +
                    "menjo_kbn = ?, menjo_riyu = ?, upd_dt = CURRENT_TIMESTAMP, upd_user = 'sys' " +
                    "WHERE jichitai_cd = '01202' AND shitei_no = ? AND rno = 1";

            jdbcTemplate.update(updateSql,
                    form.getRegistrationDate(),
                    form.getManagerName(),
                    form.getManagerNameKana(),
                    form.getManagerAddress(),
                    form.getManagerPhone(),
                    menjoKbn,
                    form.getExemptionReason(),
                    shiteiNo // WHERE句の条件用
            );
            log.info("納税管理人情報を更新しました。指定番号: {}", shiteiNo);

        } else {
            // ==========================================
            // パターンB：存在しない場合は INSERT（新規登録）
            // ==========================================
            String insertSql = "INSERT INTO t_nokan " +
                    "(jichitai_cd, shitei_no, rno, toroku_ymd, shinkoku_ymd, name, name_kana, jusho, tel, menjo_kbn, menjo_riyu, new_flg, del_flg, add_dt, add_user, upd_dt, upd_user, version) " +
                    "VALUES ('01202', ?, 1, ?, CURRENT_DATE, ?, ?, ?, ?, ?, ?, '1', '0', CURRENT_TIMESTAMP, 'sys', CURRENT_TIMESTAMP, 'sys', 1)";

            jdbcTemplate.update(insertSql,
                    shiteiNo,
                    form.getRegistrationDate(),
                    form.getManagerName(),
                    form.getManagerNameKana(),
                    form.getManagerAddress(),
                    form.getManagerPhone(),
                    menjoKbn,
                    form.getExemptionReason()
            );
            log.info("納税管理人情報を新規登録しました。指定番号: {}", shiteiNo);
        }
    }
}    
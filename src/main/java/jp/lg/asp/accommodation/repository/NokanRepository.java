package jp.lg.asp.accommodation.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class NokanRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void deleteByJichitaiCdAndShiteiNo(String jichitaiCd, String shiteiNo) {
        String sql = "DELETE FROM t_nokan WHERE jichitai_cd = ? AND shitei_no = ?";
        jdbcTemplate.update(sql, jichitaiCd, shiteiNo);
    }
}
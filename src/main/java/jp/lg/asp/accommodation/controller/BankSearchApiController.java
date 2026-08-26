package jp.lg.asp.accommodation.controller;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * 金融機関・支店あいまい検索 API
 * pg_trgm の similarity() を使用（% 演算子はJDBCプレースホルダー非対応のため）
 */
@RestController
@RequestMapping("/api/bank")
@RequiredArgsConstructor
public class BankSearchApiController {

    private final JdbcTemplate jdbcTemplate;

    /** 金融機関名あいまい検索 */
    @GetMapping("/search")
    public List<Map<String, Object>> searchBanks(@RequestParam String q) {
        if (!StringUtils.hasText(q)) {
            return List.of();
        }
        return jdbcTemplate.queryForList(
                "SELECT bank_code, bank_name FROM m_bank WHERE similarity(bank_name, ?) > 0.1 ORDER BY similarity(bank_name, ?) DESC LIMIT 20",
                q, q);
    }

    /** 支店名あいまい検索（金融機関コードで絞り込み） */
    @GetMapping("/branch/search")
    public List<Map<String, Object>> searchBranches(
            @RequestParam String q,
            @RequestParam(required = false) String bankCode) {
        if (!StringUtils.hasText(q)) {
            return List.of();
        }
        if (StringUtils.hasText(bankCode)) {
            return jdbcTemplate.queryForList(
                    "SELECT branch_code, branch_name FROM m_branch WHERE bank_code = ? AND similarity(branch_name, ?) > 0.1 ORDER BY similarity(branch_name, ?) DESC LIMIT 20",
                    bankCode, q, q);
        }
        return jdbcTemplate.queryForList(
                "SELECT branch_code, branch_name FROM m_branch WHERE similarity(branch_name, ?) > 0.1 ORDER BY similarity(branch_name, ?) DESC LIMIT 20",
                q, q);
    }
}

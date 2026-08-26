package jp.lg.asp.accommodation.controller;

import java.util.List;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jp.lg.asp.accommodation.entity.MBank;
import jp.lg.asp.accommodation.entity.MBranch;
import jp.lg.asp.accommodation.repository.BankSearchRepository;
import jp.lg.asp.accommodation.repository.BranchSearchRepository;
import lombok.RequiredArgsConstructor;

/**
 * 金融機関・支店あいまい検索 API
 *
 * furikomiKoza.html の入力補助用。
 * pg_trgm の % 演算子で金融機関マスタ・支店マスタを検索し、候補をJSONで返す。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bank")
public class BankSearchApiController {

    private final BankSearchRepository bankSearchRepository;
    private final BranchSearchRepository branchSearchRepository;

    /** 金融機関名あいまい検索 */
    @GetMapping("/search")
    public List<BankItem> searchBank(@RequestParam String word) {
        if (!StringUtils.hasText(word) || word.length() < 2) {
            return List.of();
        }
        return bankSearchRepository.searchByName(word).stream()
                .map(b -> new BankItem(b.getBankCode(), b.getBankName(), b.getBankKana()))
                .toList();
    }

    /** 支店名あいまい検索 */
    @GetMapping("/branch/search")
    public List<BranchItem> searchBranch(@RequestParam String bankCode, @RequestParam String word) {
        if (!StringUtils.hasText(bankCode) || !StringUtils.hasText(word) || word.length() < 2) {
            return List.of();
        }
        return branchSearchRepository.searchByName(bankCode, word).stream()
                .map(b -> new BranchItem(b.getBranchCode(), b.getBranchName(), b.getBranchKana()))
                .toList();
    }

    public record BankItem(String code, String name, String kana) {}
    public record BranchItem(String code, String name, String kana) {}
}

package jp.lg.asp.accommodation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.MBank;

@Repository
public interface BankSearchRepository extends JpaRepository<MBank, String> {

    /** 金融機関名のあいまい検索（pg_trgm） */
    @Query(value = "SELECT * FROM m_bank WHERE bank_name % :word OR bank_kana % :word ORDER BY similarity(bank_name, :word) DESC LIMIT 20",
            nativeQuery = true)
    List<MBank> searchByName(@Param("word") String word);
}

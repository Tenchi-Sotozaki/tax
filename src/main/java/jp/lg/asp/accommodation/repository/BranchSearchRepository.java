package jp.lg.asp.accommodation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.MBranch;
import jp.lg.asp.accommodation.entity.MBranchId;

@Repository
public interface BranchSearchRepository extends JpaRepository<MBranch, MBranchId> {

    /** 支店名のあいまい検索（pg_trgm） */
    @Query(value = "SELECT * FROM m_branch WHERE bank_code = :bankCode AND (branch_name % :word OR branch_kana % :word) ORDER BY similarity(branch_name, :word) DESC LIMIT 20",
            nativeQuery = true)
    List<MBranch> searchByName(@Param("bankCode") String bankCode, @Param("word") String word);
}

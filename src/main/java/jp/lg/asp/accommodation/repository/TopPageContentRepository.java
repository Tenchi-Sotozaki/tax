package jp.lg.asp.accommodation.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.TopPageContent;
import jp.lg.asp.accommodation.entity.TopPageContentId;

@Repository
public interface TopPageContentRepository extends JpaRepository<TopPageContent, TopPageContentId> {

    /** トップページの表示コンテンツ取得 jichitaiCd="99999") */
	List<TopPageContent> findByJichitaiCdAndPostingStartDateLessThanEqualAndPostingEndDateGreaterThanEqual(String jichitaiCd,LocalDate startDate,LocalDate endDate);
}

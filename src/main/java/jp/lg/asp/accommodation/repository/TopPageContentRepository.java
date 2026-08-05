package jp.lg.asp.accommodation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.TopPageContent;
import jp.lg.asp.accommodation.entity.TopPageContentId;

@Repository
public interface TopPageContentRepository extends JpaRepository<TopPageContent, TopPageContentId> {

    /** 全自治体共有コンテンツ取得 (kbn="0", jichitaiCd="00000") */
    Optional<TopPageContent> findByKbnAndJichitaiCd(String kbn, String jichitaiCd);
}

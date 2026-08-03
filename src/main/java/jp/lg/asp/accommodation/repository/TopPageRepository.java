package jp.lg.asp.accommodation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.TopPage;
import jp.lg.asp.accommodation.entity.TopPageId;

@Repository
public interface TopPageRepository extends JpaRepository<TopPage, TopPageId> {

	/**
	 * 表示区分と自治体コードでトップページの内容を取得する。
	 *
	 * @param kbn 表示区分
	 * @param jichitaiCd 自治体コード（全自治体共有の場合は "00000"）
	 * @return トップページマスタ
	 */
	Optional<TopPage> findByKbnAndJichitaiCd(String kbn, String jichitaiCd);
}

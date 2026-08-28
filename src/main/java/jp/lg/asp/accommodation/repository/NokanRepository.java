package jp.lg.asp.accommodation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.Nokan;
import jp.lg.asp.accommodation.entity.NokanId;

@Repository
public interface NokanRepository extends JpaRepository<Nokan, NokanId> {

	/**
	 * 最新の納税管理人情報を取得する。
	 * t_nokan は rno を持つ履歴テーブルのため、new_flg で最新の1件に絞る。
	 * 絞らないと履歴が2件以上あるときに複数行が返り、Optional で受け取れない。
	 */
	@Query("""
			SELECT n FROM Nokan n
			WHERE n.jichitaiCd = :jichitaiCd AND n.shiteiNo = :shiteiNo
			  AND n.newFlg = '1' AND n.delFlg = '0'
			""")
	Optional<Nokan> findByJichitaiCdAndShiteiNo(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("shiteiNo") String shiteiNo);

	@Modifying
	@Query("DELETE FROM Nokan n WHERE n.jichitaiCd = :jichitaiCd AND n.shiteiNo = :shiteiNo")
	void deleteByJichitaiCdAndShiteiNo(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("shiteiNo") String shiteiNo);
}
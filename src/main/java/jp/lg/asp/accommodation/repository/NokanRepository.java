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

	@Query("""
			SELECT n FROM Nokan n
			WHERE n.jichitaiCd = :jichitaiCd AND n.shiteiNo = :shiteiNo AND n.delFlg = '0'
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
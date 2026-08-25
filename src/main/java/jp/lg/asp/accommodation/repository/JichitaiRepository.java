package jp.lg.asp.accommodation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.Jichitai;

@Repository
public interface JichitaiRepository extends JpaRepository<Jichitai, String> {

	/**
	 * クエリパラメータ文字列から自治体を取得する。
	 * param に一意制約が無いため、複数件ヒットした場合は先頭の1件を返す。
	 *
	 * @param param クエリパラメータ文字列（m_jichitai.param）
	 * @return 該当する自治体。存在しない場合は empty
	 */
	Optional<Jichitai> findFirstByParam(String param);
	
	/**
     * 自治体コードを指定して user_name を更新する
     */
    @Modifying
    @Query("UPDATE Jichitai j SET j.userName = :userName WHERE j.jichitaiCd = :jichitaiCd")
    int updateUserNameByJichitaiCd(
        @Param("jichitaiCd") String jichitaiCd, 
        @Param("userName") String userName
    );
}
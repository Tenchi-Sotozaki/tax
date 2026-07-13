package jp.lg.asp.accommodation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.User;
import jp.lg.asp.accommodation.entity.UserId;

@Repository
public interface UserRepository extends JpaRepository<User, UserId> {

	List<User> findByJichitaiCdOrderById(String jichitaiCd);

	List<User> findByJichitaiCdAndRoleId(String jichitaiCd, java.math.BigDecimal roleId);

	@Query("SELECT u FROM User u WHERE u.jichitaiCd = :jichitaiCd" +
			" AND u.delFlg = '0'" +
			" AND (:id IS NULL OR u.id LIKE %:id%)" +
			" AND (:name IS NULL OR u.name LIKE :name)" +
			" AND (:nameKana IS NULL OR u.nameKana LIKE :nameKana)" +
			" AND (:busho IS NULL OR u.busho LIKE :busho)" +
			" ORDER BY u.id")
	List<User> search(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("id") String id,
			@Param("name") String name,
			@Param("nameKana") String nameKana,
			@Param("busho") String busho);
}

package jp.lg.asp.accommodation.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

	/**
	 * システム管理用のデフォルト権限のID（内部データのため一覧には表示しない）
	 *
	 * 権限の採番は1から始まるため、0にしておくことで採番と衝突しない
	 */
	long DEFAULT_USER_ROLE_ID = 0L;

	/** 指定した権限が付与されているユーザーを取得する */
	@Query("SELECT u FROM User u WHERE u.jichitaiCd = :jichitaiCd" +
			" AND u.delFlg = '0'" +
			" AND u.roleId <> " + DEFAULT_USER_ROLE_ID +
			" AND u.roleId = :roleId" +
			" ORDER BY u.id")
	List<User> findAssignedUsers(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("roleId") java.math.BigDecimal roleId);

	@Query("SELECT u FROM User u WHERE u.jichitaiCd = :jichitaiCd" +
			" AND u.delFlg = '0'" +
			" AND u.roleId <> " + DEFAULT_USER_ROLE_ID +
			" AND (:id IS NULL OR u.id LIKE %:id%)" +
			" AND (:name IS NULL OR u.name LIKE :name)" +
			" AND (:nameKana IS NULL OR u.nameKana LIKE :nameKana)" +
			" AND (:busho IS NULL OR u.busho LIKE :busho)" +
			" AND (:roleId IS NULL OR u.roleId = :roleId)" +
			" ORDER BY u.id")
	Page<User> searchPage(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("id") String id,
			@Param("name") String name,
			@Param("nameKana") String nameKana,
			@Param("busho") String busho,
			@Param("roleId") java.math.BigDecimal roleId,
			Pageable pageable);

	@Query("SELECT u FROM User u WHERE u.jichitaiCd = :jichitaiCd" +
			" AND u.delFlg = '0'" +
			" AND u.roleId <> " + DEFAULT_USER_ROLE_ID +
			" AND (:id IS NULL OR u.id LIKE %:id%)" +
			" AND (:name IS NULL OR u.name LIKE :name)" +
			" AND (:nameKana IS NULL OR u.nameKana LIKE :nameKana)" +
			" AND (:busho IS NULL OR u.busho LIKE :busho)" +
			" AND (:roleId IS NULL OR u.roleId = :roleId)" +
			" ORDER BY u.id")
	java.util.List<User> searchAll(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("id") String id,
			@Param("name") String name,
			@Param("nameKana") String nameKana,
			@Param("busho") String busho,
			@Param("roleId") java.math.BigDecimal roleId);

}

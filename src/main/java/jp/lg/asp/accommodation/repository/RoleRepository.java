package jp.lg.asp.accommodation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.Role;
import jp.lg.asp.accommodation.entity.RoleId;

@Repository
public interface RoleRepository extends JpaRepository<Role, RoleId> {

	@Query("SELECT r FROM Role r WHERE r.jichitaiCd = :jichitaiCd ORDER BY r.roleId")
	List<Role> findByJichitaiCd(@Param("jichitaiCd") String jichitaiCd);
}

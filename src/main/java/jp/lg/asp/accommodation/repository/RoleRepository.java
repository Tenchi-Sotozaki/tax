package jp.lg.asp.accommodation.repository;

import jp.lg.asp.accommodation.entity.Role;
import jp.lg.asp.accommodation.entity.RoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RoleRepository extends JpaRepository<Role, RoleId> {
    
    List<Role> findByJichitaiCdOrderByRoleId(String jichitaiCd);
    
    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.roleDetails rd LEFT JOIN FETCH rd.screen WHERE r.jichitaiCd = :jichitaiCd ORDER BY r.roleId ASC")
    List<Role> findByJichitaiCdWithDetails(@Param("jichitaiCd") String jichitaiCd);
    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.roleDetails WHERE r.jichitaiCd = :jichitaiCd AND r.roleId = :roleId")
    java.util.Optional<Role> findByIdWithDetails(@Param("jichitaiCd") String jichitaiCd, @Param("roleId") Long roleId);

    @Query("SELECT COALESCE(MAX(r.roleId), 0) FROM Role r WHERE r.jichitaiCd = :jichitaiCd")
    Long findMaxRoleIdByJichitaiCd(@Param("jichitaiCd") String jichitaiCd);

    @Query("SELECT COUNT(rd) FROM RoleDetail rd WHERE rd.jichitaiCd = :jichitaiCd AND rd.roleId = :roleId AND TRIM(rd.screenId) = TRIM(:screenId) AND rd.permission >= '1'")
    long countAccessibleScreen(@Param("jichitaiCd") String jichitaiCd, @Param("roleId") Long roleId, @Param("screenId") String screenId);

    @Query("SELECT COUNT(rd) FROM RoleDetail rd WHERE rd.jichitaiCd = :jichitaiCd AND rd.roleId = :roleId AND TRIM(rd.screenId) = TRIM(:screenId) AND rd.permission >= '2'")
    long countWritableScreen(@Param("jichitaiCd") String jichitaiCd, @Param("roleId") Long roleId, @Param("screenId") String screenId);
}
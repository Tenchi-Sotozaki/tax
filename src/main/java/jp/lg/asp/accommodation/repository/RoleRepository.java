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
    
    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.roleDetails rd LEFT JOIN FETCH rd.screen WHERE r.jichitaiCd = :jichitaiCd")
    List<Role> findByJichitaiCdWithDetails(@Param("jichitaiCd") String jichitaiCd);
}
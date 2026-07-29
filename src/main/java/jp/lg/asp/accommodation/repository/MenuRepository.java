package jp.lg.asp.accommodation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jp.lg.asp.accommodation.entity.Menu;
import jp.lg.asp.accommodation.entity.MenuId;

public interface MenuRepository extends JpaRepository<Menu, MenuId> {

	@Query("SELECT m FROM Menu m WHERE m.jichitaiCd = :jichitaiCd ORDER BY m.dspOdr")
	List<Menu> findByJichitaiCdOrderByDspOdr(@Param("jichitaiCd") String jichitaiCd);
}

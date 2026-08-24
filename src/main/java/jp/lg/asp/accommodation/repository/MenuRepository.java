package jp.lg.asp.accommodation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import jp.lg.asp.accommodation.entity.Menu;

public interface MenuRepository extends JpaRepository<Menu, String> {

	@Query("SELECT m FROM Menu m ORDER BY m.dspOdr")
	List<Menu> findAllOrderByDspOdr();
}

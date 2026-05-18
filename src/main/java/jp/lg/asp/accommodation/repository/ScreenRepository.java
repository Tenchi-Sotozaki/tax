package jp.lg.asp.accommodation.repository;

import jp.lg.asp.accommodation.entity.Screen;
import jp.lg.asp.accommodation.entity.ScreenId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, ScreenId> {
    
    List<Screen> findByJichitaiCdOrderByScreenId(String jichitaiCd);
}